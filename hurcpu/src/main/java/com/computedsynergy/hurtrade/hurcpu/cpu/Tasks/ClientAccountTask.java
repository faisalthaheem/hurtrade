/*
 * Copyright 2016 Faisal Thaheem <faisal.ajmal@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.computedsynergy.hurtrade.hurcpu.cpu.Tasks;

import com.computedsynergy.hurtrade.hurcpu.cpu.RequestConsumers.ClientRequestConsumer;
import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.Quote;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.SourceQuote;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.LedgerModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.QuoteModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CommodityUser;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.updates.ClientUpdate;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.SavedPositionModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.GeneralUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;

import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.ORDER_STATE_CANCELED;
import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.ORDER_STATE_OPEN;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.EXPIRY_LOCK_USER_POSITIONS;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.TIMEOUT_LOCK_USER_POSITIONS;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.getLockNameForUserPositions;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.getUserPositionsKeyName;
import com.github.jedis.lock.JedisLock;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import redis.clients.jedis.Jedis;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 *
 * Subscribes to the rate queue and notifies the clients in case there is an
 * update after applying client specific spreads
 *
 * Also processes client's positions effectively calculating P/L
 *
 * Listens to the client's requests
 */
public class ClientAccountTask extends AmqpBase {
    


    Gson gson = new Gson();
    QuoteModel quoteModel = null;

    private User _self = null;
    private ClientRequestConsumer _clientRequestConsumer;

    //mq related
    private String _clientExchangeName = "";
    private String _myRateQueueName = "";
    private String _incomingQueueName = "";
    private String _outgoingQueueName = "";
    private String _officeExchangeName = "";

    //redis related
    private String _userPositionsKeyName = "";

    //finance related variables
    private int _countOpenPositions = 0;
    private BigDecimal _floating = BigDecimal.ZERO;
    private BigDecimal _availableCash = BigDecimal.ZERO;
    private BigDecimal _usedMargin = BigDecimal.ZERO;
    private BigDecimal _equity = BigDecimal.ZERO;
    private BigDecimal _usableMargin = BigDecimal.ZERO;
    private BigDecimal _usedMarginBuy = BigDecimal.ZERO;
    private BigDecimal _usedMarginSell = BigDecimal.ZERO;
    private Object _accountStatusLock = new Object();

    //use a dedicated channel for client requests/responses
    Channel _exclusiveChannelClientRequestConsumer = null;

    public ClientAccountTask(User u){
        _self = u;

        _userPositionsKeyName = getUserPositionsKeyName(_self.getUseruuid());

        _clientExchangeName = MqNamingUtil.getClientExchangeName(_self.getUseruuid());
        _incomingQueueName = MqNamingUtil.getClientIncomingQueueName(u.getUseruuid());
        _outgoingQueueName = MqNamingUtil.getClientOutgoingQueueName(u.getUseruuid());
        _myRateQueueName = Constants.QUEUE_NAME_RATES + _self.getUseruuid().toString();
        _officeExchangeName = MqNamingUtil.getOfficeExchangeName(_self.getUserOffice().getOfficeuuid());

        _exclusiveChannelClientRequestConsumer = CreateNewChannel();

        this.init();
    }

    public void init(){

        try {
            super.setupAMQP();

            quoteModel = new QuoteModel();

            Map<String, Object> args = new HashMap<String, Object>();
            args.put("x-max-length", CommandLineOptions.getInstance().maxQueuedMessages); //retain only x messages
            args.put("x-message-ttl", CommandLineOptions.getInstance().maxQueueTtl); //retain only for x seconds

            channel.exchangeDeclare(_clientExchangeName, "direct", true);

            channel.queueDeclare(_incomingQueueName, true, false, false, args);
            channel.queueBind(_incomingQueueName, _clientExchangeName, "request");

            channel.queueDeclare(_outgoingQueueName, true, false, false, args);
            channel.queueBind(_outgoingQueueName, _clientExchangeName, "response");

            channel.queueDeclare(_myRateQueueName, true, false, false, null);
            channel.queueBind(_myRateQueueName, Constants.EXCHANGE_NAME_RATES, "");

            //consume command related messages from user

            _clientRequestConsumer = new ClientRequestConsumer(this, _self, _exclusiveChannelClientRequestConsumer, _clientExchangeName);

            _exclusiveChannelClientRequestConsumer.basicConsume(_incomingQueueName, false, "command" + _self.getUseruuid().toString(), _clientRequestConsumer);

            //consume the rates specific messages and send updates to the user as a result
            channel.basicConsume(_myRateQueueName, false, "rates-"+ _self.getUseruuid().toString(),
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag,
                                                   Envelope envelope,
                                                   AMQP.BasicProperties properties,
                                                   byte[] body)
                                throws IOException {
                            String routingKey = envelope.getRoutingKey();
                            String contentType = properties.getContentType();
                            long deliveryTag = envelope.getDeliveryTag();


                            SourceQuote quote = new Gson().fromJson(new String(body), SourceQuote.class);
                            processQuote(quote);
                            publishAccountStatus();

                            channel.basicAck(deliveryTag, false);
                        }
                    });
        }catch (Exception ex){
            _log.log(Level.SEVERE, null, ex);
        }
    }

    private void processQuote(SourceQuote quote) {
        

        
        //introduce the spread as defined for this client for the symbols in the quote list
        Map<String, CommodityUser> userCommodities = RedisUtil
                                                .getInstance()
                                                .getCachedUserCommodities(_self.getUseruuid());
        
        
        QuoteList sourceQuotes = quote.getQuoteList();
        QuoteList clientQuotes = new QuoteList();
        for(String k:sourceQuotes.keySet()){
            
            Quote sourceQuote = sourceQuotes.get(k);

            Quote q = null;
            //include propagation to client only if they are allowed this symbol
            if(null != userCommodities && userCommodities.containsKey(k)){
                q = new Quote(
                        sourceQuote.bid,
                        sourceQuote.bid.add(userCommodities.get(k).getSpread()),
                        sourceQuote.quoteTime,
                        BigDecimal.ZERO,
                        sourceQuote.name,
                        sourceQuote.lotSize
                );
                
            }else{
                q = new Quote(
                        sourceQuote.bid,
                        sourceQuote.ask,
                        sourceQuote.quoteTime,
                        BigDecimal.ZERO,
                        sourceQuote.name,
                        sourceQuote.lotSize
                );
            }
            clientQuotes.put(k, q);
        }
        
        
        Map<UUID, Position> clientPositions = null;
        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(
                        jedis, 
                        RedisUtil.getLockNameForUserProcessing(_self.getUseruuid()),
                        RedisUtil.TIMEOUT_LOCK_USER_PROCESSING, 
                        RedisUtil.EXPIRY_LOCK_USER_PROCESSING
                    );

            
            try {
                if(lock.acquire()){

                    //process client's positions
                    clientPositions = updateClientPositions(_self, clientQuotes);
                    lock.release();

                }else{
                    _log.log(Level.SEVERE, "Could not lock user position in redis {0}",  _self.getUsername());
                }
            } catch (InterruptedException ex) {
                _log.log(Level.SEVERE, null, ex);
            }
        }
        

        //set the quotes as they are needed when trading
        String serializedQuotes = gson.toJson(clientQuotes);
        RedisUtil.getInstance().setSeriaizedQuotesForClient(serializedQuotes, _self.getUseruuid());
        
        //remove the quotes not allowed for this client
        for(String k:sourceQuotes.keySet()){
            if(!userCommodities.containsKey(k)){
                clientQuotes.remove(k);
            }
        }

        ClientUpdate update = new ClientUpdate(clientPositions, clientQuotes);
        String serializedQuotesForClient = gson.toJson(update);

        //finally send out the quotes and updated positions to the client
        publishMessage(
                _clientExchangeName,
                "response",
                "update",
                serializedQuotesForClient
        );
    }


    
    private Map<UUID, Position> updateClientPositions(User user, QuoteList clientQuotes)
    {

        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            
            JedisLock lock = new JedisLock(
                    jedis,
                    getLockNameForUserPositions(_userPositionsKeyName),
                    TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS
            );

            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions =
                            gson.fromJson(jedis.get(_userPositionsKeyName), Constants.TYPE_POSITIONS_MAP);

                    if(positions == null){
                        positions = new HashMap<>();
                    }

                    RedisUtil redis = RedisUtil.getInstance();

                    Iterator<Map.Entry<UUID, Position>> iter = positions.entrySet().iterator();
                    while(iter.hasNext()){

                        Position p = iter.next().getValue();
                        p.processQuote(clientQuotes);

                        _floating = _floating.add(p.getCurrentPl());

                        if(p.getOrderType().equals(Constants.ORDER_TYPE_BUY)){
                            _usedMarginBuy = _usedMarginBuy.add(p.getUsedMargin());
                        }else{
                            _usedMarginSell = _usedMarginSell.add(p.getUsedMargin());
                        }

                        if(p.isRequoted()){

                            if(!redis.GetOrderRequoteValid(p.getOrderId())){

                                _log.log(Level.INFO, "Requote expired for " + p.getOrderId().toString());

                                if(p.is_wasPendingClose()) {
                                    p.setOrderState(ORDER_STATE_OPEN);

                                }else{
                                    p.setOrderState(ORDER_STATE_CANCELED);
                                    iter.remove();
                                }
                            }
                        }
                    }

                    //set the number of open positions as they are used later in determining whether to issue a margin call
                    _countOpenPositions = positions.size();

                    //set client positions
                    String serializedPositions = gson.toJson(positions);
                    jedis.set(_userPositionsKeyName, serializedPositions);

                    lock.release();

                    //dump positions to db
                    GeneralUtil.saveClientPositions(user, serializedPositions);

                    //insert quotes to db
                    quoteModel.saveQuote(user.getId(), clientQuotes.values());

                    return positions;

                }else{
                    _log.log(Level.SEVERE, "Could not get lock to process user positions for {0}", user.getUsername());
                }
            }catch(Exception ex){
                _log.log(Level.SEVERE, "Error processing user positions for {0}", user.getUsername());
                _log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            //or return null if problems
            return null;
        }
    }

    private void publishAccountStatus()
    {
        String serializedAccountStatus;

        synchronized (_accountStatusLock) {
            _availableCash = new LedgerModel().GetAvailableCashForUser(_self.getId());

            _usedMargin = (_usedMarginBuy.subtract(_usedMarginSell)).abs();
            _equity = _availableCash.add(_floating);
            _usableMargin = _equity.subtract(_usedMargin);

            HashMap<String, BigDecimal> accountStatus = new HashMap<>();
            accountStatus.put("floating", _floating);
            accountStatus.put("usedMargin", _usedMargin);
            accountStatus.put("equity", _equity);
            accountStatus.put("usableMargin", _usableMargin);
            accountStatus.put("availableCash", _availableCash);

            serializedAccountStatus = gson.toJson(accountStatus);
        }

        publishMessage(
                _clientExchangeName,
                "response",
                "accountStatus",
                serializedAccountStatus
        );

        _floating = BigDecimal.ZERO;
        _usedMarginSell = BigDecimal.ZERO;
        _usedMarginBuy = BigDecimal.ZERO;

        //check if margin call and close all positions
        if(_countOpenPositions > 0 && _usableMargin.compareTo(BigDecimal.ZERO) <= 0){

            if(_self.isLiquidate()) {
                if (null != _clientRequestConsumer) {

                    String notification = String.format("[%s] margin call.", _self.getUsername());

                    _log.log(Level.INFO, notification);
                    _clientRequestConsumer.closeAllPositions();

                    publishMessage(
                            _clientExchangeName,
                            "response",
                            "notification",
                            notification
                    );

                    publishMessage(
                            _officeExchangeName,
                            "todealer",
                            "notification",
                            notification
                    );
                }
            }else{

                String notification = String.format("[%s] margin call suppressed.", _self.getUsername());
                _log.log(Level.WARNING, notification);

                publishMessage(
                        _clientExchangeName,
                        "response",
                        "notification",
                        notification
                );

                publishMessage(
                        _officeExchangeName,
                        "todealer",
                        "notification",
                        notification
                );
            }

        }
    }

    /**
     * Used with new trade requests
     * @return
     */
    public BigDecimal get_usableMargin() {

        BigDecimal ret;

        synchronized (_accountStatusLock) {

            ret = _usableMargin;
        }

        return ret;
    }
}
