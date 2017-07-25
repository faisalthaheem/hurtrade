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
import com.computedsynergy.hurtrade.sharedcomponents.amqp.CustomDefaultConsumer;
import com.computedsynergy.hurtrade.sharedcomponents.charting.CandleStickChartingDataProvider;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.charting.CandleStick;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade.TradeRequest;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade.TradeResponse;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CommodityUser;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.GeneralUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import com.github.jedis.lock.JedisLock;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import redis.clients.jedis.Jedis;
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

import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.*;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.*;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class ClientRequestConsumer extends CustomDefaultConsumer {

    private final User _user;
    private final String _exchangeName;
    private final String _officeExchangeName;
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
        this._officeExchangeName = MqNamingUtil.getOfficeExchangeName(_user.getUserOffice().getOfficeuuid());
        this._singleExecutorService = Executors.newFixedThreadPool(2);

    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body); //To change body of generated methods, choose Tools | Templates.

        long deliveryTag = envelope.getDeliveryTag();

        try {

            String commandVerb = properties.getType();

            if(commandVerb.equals("trade")) {

                TradeRequest request = TradeRequest.fromJson(new String(body));
                TradeResponse response = new TradeResponse(request);

                processTradeRequest(response, _user);

            }else if(commandVerb.equals("tradeClosure")) {

                Map<String,String> cmdParams =  new Gson().fromJson(new String(body), Constants.TYPE_DICTIONARY);
                closePosition(UUID.fromString(cmdParams.get("orderid")));

            }else if(commandVerb.equals("requote")) {

                Map<String,String> cmdParams =  new Gson().fromJson(new String(body), Constants.TYPE_DICTIONARY);
                processRequoteCommand(cmdParams);

            }else if(commandVerb.equalsIgnoreCase("candlestick")){

                _singleExecutorService.submit(() -> {

                    Gson gson = new Gson();
                    Map<String,String> cmdParams =  gson.fromJson(new String(body), Constants.TYPE_DICTIONARY);

                    //todo dealers are not getting these
                    String notification = String.format("Candlestick chart request received for commodity [%s]", cmdParams.get("commodity"));
                    publishNotificationMessage(
                            _user.getId(),
                            -1,
                            _exchangeName,
                            "response",
                            notification
                    );

                    CandleStickChartingDataProvider cstickProvider = new CandleStickChartingDataProvider();

                    List<CandleStick> lstRet =  cstickProvider.GetChartData(
                            cmdParams.get("commodity"),
                            _user.getId(),
                            cmdParams.get("resolution"),
                            Integer.parseInt(cmdParams.get("samples"))
                    );
                    String serializedResponse = gson.toJson(lstRet);

                    publishMessage(
                            _exchangeName,
                            "response",
                            "candlestick",
                            serializedResponse
                    );

                });
            }

        } catch (Exception ex) {
            _log.log(Level.SEVERE, null, ex);
        }

        getChannel().basicAck(deliveryTag, false);

    }

    private void processRequoteCommand(Map<String,String> params){


        String notification = null;

        if(!GeneralUtil.isTradingOpen()){
            notification = String.format("[%s] Floor is closed for trading. Cannot close posittion.", _user.getUsername());
        }

        if(null == notification && params.containsKey("orderid")){
            UUID orderId = UUID.fromString(params.get("orderid"));

            String userPositionsKeyName = getUserPositionsKeyName(_user.getUseruuid());



            try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
                JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
                try{
                    if(lock.acquire()){

                        //get client positions
                        Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName), Constants.TYPE_POSITIONS_MAP);

                        if(positions.containsKey(orderId)){

                            Position p = positions.get(orderId);
                            if(p.isRequoted()){

                                if(p.is_wasPendingClose()){
                                    p.setClosePrice(p.getRequoteprice());
                                    p.setOrderState(Constants.ORDER_STATE_CLOSED);
                                    positions.remove(orderId);

                                    notification = String.format("Requote accepted by [%s] for position [%d] -> closed.", _user.getUsername() ,p.getFriendlyorderid());

                                }else {
                                    p.setOpenPrice(p.getRequoteprice());
                                    p.setOrderState(ORDER_STATE_OPEN);

                                    notification = String.format("Requote accepted by [%s] for position [%d] -> opened.", _user.getUsername() ,p.getFriendlyorderid());
                                }


                                //set client positions
                                String serializedPositions = gson.toJson(positions);
                                jedis.set(userPositionsKeyName, serializedPositions);

                            }else {
                                _log.log(Level.INFO, _user.getUsername() + " requested requote on an order that has not been requoted: " + orderId);
                                //todo: record this breach
                            }

                        }else{
                            _log.log(Level.INFO, _user.getUsername() + " requested requote on non-existent order: " + orderId);
                        }


                        lock.release();

                    }else{
                        _log.log(Level.SEVERE, null, "Could not process user positions " + _user.getUsername());
                    }
                }catch(Exception ex){
                    _log.log(Level.SEVERE, null, "Could not process user positions " + _user.getUsername());
                }
            }


            if(null != notification) {

                publishNotificationMessage(
                        _user.getId(),
                        -1,
                        _exchangeName,
                        "response",
                        notification
                );

                publishNotificationMessage(
                        -1,
                        _user.getUserOffice().getId(),
                        _officeExchangeName,
                        "todealer",
                        notification
                );
            }
        }

    }

    private void closePosition(UUID orderId)
    {
        String notification = null;

        if(!GeneralUtil.isTradingOpen()){
            notification = String.format("[%s] Floor is closed for trading. Cannot close posittion.", _user.getUsername());
        }

        if(null == notification) {

            try (Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()) {

                //get the last quoted prices for commodities for this user
                String serializedQuotes = RedisUtil.getInstance().getSeriaizedQuotesForClient(_user.getUseruuid());
                QuoteList quotesForClient = gson.fromJson(serializedQuotes, QuoteList.class);

                String userPositionsKeyName = getUserPositionsKeyName(_user.getUseruuid());
                Type mapType = Constants.TYPE_POSITIONS_MAP;

                JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
                try {
                    if (lock.acquire()) {

                        //get client positions
                        Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName), mapType);
                        if (positions.containsKey(orderId)) {


                            Position position = positions.get(orderId);
                            if(position.isOpen()) {
                                closePosition(position, quotesForClient);
                                //do not remove from here as this position has been marked close and now pending dealer revie.

                                notification = String.format("[%d]:[%s] requested close and is pending dealer review.",
                                        position.getFriendlyorderid(), _user.getUsername());

                                //set client positions
                                String serializedPositions = gson.toJson(positions);
                                jedis.set(userPositionsKeyName, serializedPositions);
                            }
                        }

                        lock.release();

                    } else {
                        _log.log(Level.SEVERE, null, "Could not process user positions " + _user.getUsername());
                    }
                } catch (Exception ex) {
                    _log.log(Level.SEVERE, null, "Could not process user positions " + _user.getUsername());
                }
            }
        }

        //dispatch notification
        if(null != notification){

            publishNotificationMessage(
                _user.getId(),
                -1,
                _exchangeName,
                "response",
                notification
            );

            publishNotificationMessage(
                -1,
                _user.getUserOffice().getId(),
                _officeExchangeName,
                "todealer",
                notification
            );

        }

    }

    public void closeAllPositions()
    {

        //get the last quoted prices for commodities for this user
        String serializedQuotes = RedisUtil.getInstance().getSeriaizedQuotesForClient(_user.getUseruuid());
        QuoteList quotesForClient = gson.fromJson(serializedQuotes, QuoteList.class);

        String userPositionsKeyName = getUserPositionsKeyName(_user.getUseruuid());

        StringBuilder builder = new StringBuilder();

        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName), Constants.TYPE_POSITIONS_MAP);

                    String notification = "";

                    Iterator<Map.Entry<UUID, Position>> iter = positions.entrySet().iterator();
                    while(iter.hasNext()){
                        Position p = iter.next().getValue();

                        if(p.isOpen()) {
                            closePosition(p, quotesForClient);

                            p.setOrderState(ORDER_STATE_CLOSED);

                            notification = String.format("[%d]:[%s] liquidated.\n",
                                    p.getFriendlyorderid(), _user.getUsername());

                        }else if(p.isPendingOpen()){
                            p.setOrderState(ORDER_STATE_CANCELED);

                            notification = String.format("[%d]:[%s] cancelled due to margin call.\n",
                                    p.getFriendlyorderid(), _user.getUsername());
                        }

                        builder.append(notification);
                        _log.info(notification);


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

        String notification = builder.toString();

        //dispatch notification
        if(null != notification){

            publishNotificationMessage(
                    _user.getId(),
                    -1,
                    _exchangeName,
                    "response",
                    notification
            );

            publishNotificationMessage(
                    -1,
                    _user.getUserOffice().getId(),
                    _officeExchangeName,
                    "todealer",
                    notification
            );

        }
    }


    /**
     * On request of trader the selected trade is queued for dealer's review.
     * @param position
     * @param quotesForClient
     */
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

        String notification = null;

        if(!GeneralUtil.isTradingOpen()){
            notification = String.format("[%s][%s] Floor is closed for trading.", user.getUsername(), response.getRequest().getCommodity());
        }
        
        if(notification == null && !quotesForClient.containsKey(response.getRequest().getCommodity()))
        {
            notification = String.format("[{0}] is not allowed to trade [{1}]",user.getUsername() , response.getRequest().getCommodity());
            _log.log(Level.SEVERE, notification);
        }

        if(null == notification)
        {
            CommodityUser userCommodity = userCommodities.get(response.getRequest().getCommodity());
            if(
                    response.getRequest().getRequestedLot().compareTo(userCommodity.getMinamount()) < 0
                    ||
                    response.getRequest().getRequestedLot().compareTo(userCommodity.getMaxamount()) > 0
            ){
                notification = String.format(
                        "Requested amount outside of allowed range [%s] to [%s]",
                        userCommodity.getMinamount().toString(),
                        userCommodity.getMaxamount().toString()
                        );
            }
        }
        

        if(null == notification) {
            try (Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()) {

                String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());
                Type mapType = Constants.TYPE_POSITIONS_MAP;

                JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
                try {
                    if (lock.acquire()) {

                        //get client positions
                        Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName), mapType);
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


                            if (position != null) {

                                Position tempPosition = position.clone();

                                //check if the used margin of this order < usable margin
                                tempPosition.processQuote(quotesForClient);

                                BigDecimal usableMargin = _account.get_usableMargin(); //avoid successive locks
                                if (tempPosition.getUsedMargin().compareTo(usableMargin) <= 0) {

                                    notification = String.format(
                                            "[%s] requested new trade position [%d] [%s] [%s] [%f] ",
                                            _user.getUsername(),
                                            position.getFriendlyorderid(),
                                            position.getCommodity(),
                                            position.getOrderType(),
                                            position.getAmount()
                                    );

                                    positions.put(position.getOrderId(), position);
                                    response.setResposneOk();
                                } else {

                                    notification = String.format("New Trade declined because of insufficient usable margin available. [%s] requested [%s] required [%f] available [%f]",
                                            _user.getUsername(),
                                            position.getCommodity(),
                                            tempPosition.getUsedMargin(),
                                            usableMargin
                                    );
                                    _log.log(Level.INFO, notification);
                                }
                            }

                        } catch (Exception ex) {
                            _log.log(Level.SEVERE, ex.getMessage(), ex);
                        }

                        if (null != position) {
                            //set client positions
                            String serializedPositions = gson.toJson(positions);
                            jedis.set(userPositionsKeyName, serializedPositions);
                        }

                        lock.release();

                    } else {
                        _log.log(Level.SEVERE, null, "Could not lock user positions " + user.getUsername());
                    }
                } catch (Exception ex) {
                    _log.log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
                }
            }
        }


        if(null != notification) {

            publishNotificationMessage(
                    _user.getId(),
                    -1,
                    _exchangeName,
                    "response",
                    notification
            );

            publishNotificationMessage(
                    -1,
                    _user.getUserOffice().getId(),
                    _officeExchangeName,
                    "todealer",
                    notification
            );
        }
    }


}
