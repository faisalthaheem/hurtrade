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
package com.computedsynergy.hurtrade.hurcpu.cpu.RequestConsumers;

import com.computedsynergy.hurtrade.hurcpu.cpu.Tasks.ClientAccountTask;
import com.computedsynergy.hurtrade.sharedcomponents.charting.CandleStickChartingDataProvider;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.charting.CandleStick;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.LedgerModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.PositionModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CommodityUser;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade.TradeRequest;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade.TradeResponse;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.updates.ClientUpdate;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;

import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.ORDER_STATE_CANCELED;
import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.ORDER_STATE_CLOSED;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.EXPIRY_LOCK_USER_POSITIONS;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.TIMEOUT_LOCK_USER_POSITIONS;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.getLockNameForUserPositions;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.getUserPositionsKeyName;
import com.github.jedis.lock.JedisLock;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class ClientRequestConsumer extends DefaultConsumer {

    private final User _user;
    private final String _exchangeName;
    private final Object _channelPublishLock = new Object();
    private final ExecutorService _singleExecutorService;
    private final ClientAccountTask _account;

    private Gson gson = new Gson();

    //logging
    protected Logger _log = Logger.getLogger(this.getClass().getName());

    public ClientRequestConsumer(ClientAccountTask account, User user, Channel channel, String exchangeName) {
        super(channel);

        _account = account;
        _user = user;
        this._exchangeName= exchangeName;
        this._singleExecutorService = Executors.newFixedThreadPool(2);

    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body); //To change body of generated methods, choose Tools | Templates.

        long deliveryTag = envelope.getDeliveryTag();

        AMQP.BasicProperties props = null;
        String serializedResponse = null;

        try {

            String commandVerb = properties.getType();

            if(commandVerb.equals("trade")) {

                TradeRequest request = TradeRequest.fromJson(new String(body));
                TradeResponse response = new TradeResponse(request);

                processTradeRequest(response, _user);

                ClientUpdate update = new ClientUpdate(response);
                serializedResponse = gson.toJson(update);

                publishToExchange(props, serializedResponse);

            }else if(commandVerb.equals("tradeClosure")) {

                Map<String,String> cmdParams =  new Gson().fromJson(new String(body), Constants.TYPE_DICTIONARY);
                closePosition(UUID.fromString(cmdParams.get("orderid")));

            }else if(commandVerb.equalsIgnoreCase("candlestick")){


                _singleExecutorService.submit(new Runnable() {
                    @Override
                    public void run() {

                        Gson gson = new Gson();
                        Map<String,String> cmdParams =  gson.fromJson(new String(body), Constants.TYPE_DICTIONARY);
                        CandleStickChartingDataProvider cstickProvider = new CandleStickChartingDataProvider();

                        List<CandleStick> lstRet =  cstickProvider.GetChartData(
                                cmdParams.get("commodity"),
                                _user.getId(),
                                cmdParams.get("resolution"),
                                Integer.parseInt(cmdParams.get("samples"))
                        );
                        String serializedResponse = gson.toJson(lstRet);
                        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
                        propsBuilder.type("candlestick");
                        com.rabbitmq.client.AMQP.BasicProperties props = propsBuilder.build();

                        publishToExchange(props, serializedResponse);
                    }
                });
            }

        } catch (Exception ex) {
            _log.log(Level.SEVERE, null, ex);
        }

        getChannel().basicAck(deliveryTag, false);

    }

    private void publishToExchange(com.rabbitmq.client.AMQP.BasicProperties props, String data){

        try {
            synchronized (_channelPublishLock) {
                getChannel().basicPublish(_exchangeName, "response", props, data.getBytes());
            }
        } catch (Exception ex) {
            _log.log(Level.SEVERE, null, ex);
        }
    }

    private void closePosition(UUID orderId)
    {

        //get the last quoted prices for commodities for this user
        String serializedQuotes = RedisUtil.getInstance().getSeriaizedQuotesForClient(_user.getUseruuid());
        QuoteList quotesForClient = gson.fromJson(serializedQuotes, QuoteList.class);


        String userPositionsKeyName = getUserPositionsKeyName(_user.getUseruuid());
        Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();

        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName),mapType);
                    if(positions.containsKey(orderId)) {


                        Position position = positions.get(orderId);
                        closePosition(position, quotesForClient);

                        //set client positions
                        String serializedPositions = gson.toJson(positions);
                        jedis.set(userPositionsKeyName, serializedPositions);
                    }



                    lock.release();

                }else{
                    _log.log(Level.SEVERE, null, "Could not process user positions " + _user.getUsername());
                }
            }catch(Exception ex){
                _log.log(Level.SEVERE, null, "Could not process user positions " + _user.getUsername());
            }
        }
    }

    public void closeAllPositions()
    {

        //get the last quoted prices for commodities for this user
        String serializedQuotes = RedisUtil.getInstance().getSeriaizedQuotesForClient(_user.getUseruuid());
        QuoteList quotesForClient = gson.fromJson(serializedQuotes, QuoteList.class);

        String userPositionsKeyName = getUserPositionsKeyName(_user.getUseruuid());
        Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();

        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName),mapType);


                    Iterator<Map.Entry<UUID, Position>> iter = positions.entrySet().iterator();
                    while(iter.hasNext()){
                        Position p = iter.next().getValue();

                        if(p.isOpen()) {
                            closePosition(p, quotesForClient);

                            p.setOrderState(ORDER_STATE_CLOSED);

                        }else if(p.isPendingOpen()){
                            p.setOrderState(ORDER_STATE_CANCELED);
                        }

                        iter.remove();
                    }

                    //set client positions
                    String serializedPositions = gson.toJson(positions);
                    jedis.set(userPositionsKeyName, serializedPositions);

                    lock.release();

                }else{
                    _log.log(Level.SEVERE, null, "Could not process user positions " + _user.getUsername());
                }
            }catch(Exception ex){
                _log.log(Level.SEVERE, null, "Could not process user positions " + _user.getUsername());
            }
        }
    }


    private void closePosition(Position position, QuoteList quotesForClient)
    {

        //process close request only if order is open
        if(position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_OPEN)) {

            try {

                position.setOrderState(Constants.ORDER_STATE_PENDING_CLOSE);

                switch (position.getOrderType()) {
                    case TradeRequest.REQUEST_TYPE_BUY: {
                        position.setClosePrice(quotesForClient.get(position.getCommodity()).bid);
                    }
                    break;
                    case TradeRequest.REQUEST_TYPE_SELL: {
                        position.setClosePrice(quotesForClient.get(position.getCommodity()).ask);
                    }
                    break;
                }

                //update the current order state to db
                PositionModel positionModel = new PositionModel();
                positionModel.saveUpdatePosition(position);

            } catch (Exception ex) {
                _log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
    
    private void processTradeRequest(TradeResponse response, User user)
    {

        Map<String, CommodityUser> userCommodities = RedisUtil
                                                .getInstance()
                                                .getCachedUserCommodities(user.getUseruuid());
        
        if(!userCommodities.containsKey(response.getRequest().getCommodity())){
            
            response.setResponseCommodityNotAllowed();
            
            _log.log(Level.SEVERE,
                    "{0} requested invalid commodity: {1}",new Object[]{ user.getUsername() , response.getRequest().getCommodity()});
            return;
        }
        
        
        //get the last quoted prices for commodities for this user
        String serializedQuotes = RedisUtil.getInstance().getSeriaizedQuotesForClient(user.getUseruuid());
        QuoteList quotesForClient = gson.fromJson(serializedQuotes, QuoteList.class);
        
        if(!quotesForClient.containsKey(response.getRequest().getCommodity()))
        {
            response.setResponseCommodityNotAllowed();
            
            _log.log(Level.SEVERE,
                    "{0} requested unknown commodity: {1}",new Object[]{ user.getUsername() , response.getRequest().getCommodity()});
            return;
        }
        
        String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());
        Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();
        
        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName),mapType);
                    Position position = null;

                    try {

                        switch (response.getRequest().getRequestType()) {
                            case TradeRequest.REQUEST_TYPE_BUY: {
                                position = new Position(
                                        UUID.randomUUID(), Constants.ORDER_TYPE_BUY,
                                        response.getRequest().getCommodity(),
                                        response.getRequest().getRequestedLot(),
                                        quotesForClient.get(response.getRequest().getCommodity()).ask,
                                        userCommodities.get(response.getRequest().getCommodity()).getRatio(),
                                        _user
                                );
                            }
                            break;
                            case TradeRequest.REQUEST_TYPE_SELL: {
                                position = new Position(
                                        UUID.randomUUID(), Constants.ORDER_TYPE_SELL,
                                        response.getRequest().getCommodity(),
                                        response.getRequest().getRequestedLot(),
                                        quotesForClient.get(response.getRequest().getCommodity()).bid,
                                        userCommodities.get(response.getRequest().getCommodity()).getRatio(),
                                        _user
                                );
                            }
                            break;
                        }

                        if(position != null){
                            //check if the used margin of this order < usable margin
                            position.processQuote(quotesForClient, true);

                            BigDecimal usableMargin = _account.get_usableMargin(); //avoid successive locks
                            if(position.getUsedMargin().compareTo(usableMargin) <= 0) {

                                positions.put(position.getOrderId(), position);
                                response.setResposneOk();
                            }else{
                                //todo: notify client with an error message explainnig there is not enough usable margin available
                                String logMessage = String.format("New Trade declined because of insufficient usable margin available. [%s] requested [%s] required [%f] available [%f]",
                                        _user.getUsername(),
                                        position.getCommodity(),
                                        position.getUsedMargin(),
                                        usableMargin
                                );
                                _log.log(Level.INFO, logMessage);
                            }
                        }

                    }catch(Exception ex){
                        _log.log(Level.SEVERE, ex.getMessage(), ex);
                    }

                    if(null != position){
                        //set client positions
                        String serializedPositions = gson.toJson(positions);
                        jedis.set(userPositionsKeyName, serializedPositions);
                    }

                    lock.release();

                }else{
                    _log.log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
                }
            }catch(Exception ex){
                _log.log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
            }
        }
    }


}
