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

import com.computedsynergy.hurtrade.sharedcomponents.models.impl.CoverAccountModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.LedgerModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.PositionModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.*;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import com.github.jedis.lock.JedisLock;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.*;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.TYPE_COV_POSITIONS_MAP;
import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.TYPE_POSITIONS_MAP;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.*;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class BackOfficeRequestConsumer extends DefaultConsumer {

    private final String officeExhcangeName;
    private final String dealerInQueueName;
    private final String dealerOutQueueName;
    //private final int officeId;
    private final Office _office;

    UserModel userModel = new UserModel();
    private Object lockChannelWrite = new Object(); //governs access to write access on mq channel
    private Gson gson = new Gson();

    //logging
    protected Logger _log = Logger.getLogger(this.getClass().getName());

    public BackOfficeRequestConsumer(Channel channel,
                         String officeExchangeName,
                         String dealerInQueueName,
                         String dealerOutQueueName,
                         Office office
    ) {
        super(channel);
        this.officeExhcangeName = officeExchangeName;
        this.dealerInQueueName = dealerInQueueName;
        this.dealerOutQueueName = dealerOutQueueName;
        _office = office;

    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body); //To change body of generated methods, choose Tools | Templates.

        long deliveryTag = envelope.getDeliveryTag();
        getChannel().basicAck(deliveryTag, false);

        try {


            String exchangeName = null;
            String routingKey = "response";
            Map<String, String> response = null;
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();


            User user = null;

            String requestType = properties.getType();
            String dealerName = properties.getUserId();

            Map<String, String> request = gson.fromJson(new String(body), Constants.TYPE_DICTIONARY);


            if(requestType.equalsIgnoreCase("client")) {

                user = userModel.getByUsername(request.get("client"));
                if (user == null) {
                    _log
                            .log(Level.SEVERE,
                                    "Could not resolve user for name: ", properties.getUserId());
                    return;
                }

                response = processClientCommand(user, request, dealerName);
                exchangeName = MqNamingUtil.getClientExchangeName(user.getUseruuid());
                builder.type("orderUpdate");

            }else if(requestType.equalsIgnoreCase("office")) {

                user = userModel.getByUsername(dealerName);
                exchangeName = MqNamingUtil.getClientExchangeName(user.getUseruuid());
                response = processOfficeCommand(request, dealerName);

                if(response.containsKey("type")) {
                    builder.type(response.get("type"));
                }

                if(request.containsKey("responseQueue")){
                    routingKey = request.get("responseQueue");
                    exchangeName = ""; //use default exchange
                }
            }

            if(null != response) {
                String serializedResponse = gson.toJson(response);

                synchronized (lockChannelWrite) {
                    getChannel().basicPublish(
                            exchangeName,
                            routingKey,
                            builder.build(),
                            serializedResponse.getBytes()
                    );
                }
            }

        }catch (Exception ex) {

            _log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private Map<String, String> processOfficeCommand(Map<String, String> request, String dealerName)
            throws Exception
    {
        Map<String, String> ret = new HashMap<>();

        String commandVerb = request.get("command");

        if(commandVerb.equalsIgnoreCase("listCoverAccounts")){

            List<CoverAccount> coverAccounts =
                    new CoverAccountModel().listCoverAccountsForOffice(_office.getId());

            String serializedResponse = gson.toJson(coverAccounts);
            ret.put("CoverAccounts", serializedResponse);
            ret.put("type", "CoverAccounts");

        }else if(
                commandVerb.equalsIgnoreCase("createUpdateCoverPosition")
                ||
                commandVerb.equalsIgnoreCase("closeCoverPosition")
        ){
            CoverPosition position = gson.fromJson(
                    request.get("position"),
                    CoverPosition.class);

            String officeCoverPositionsKeyName = getKeyNameForOffCovPos(_office.getOfficeuuid());
            String officeCoverPositionsLockName = getLockNameForOffCovPos(_office.getOfficeuuid());

            try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
                JedisLock lock = new JedisLock(
                        jedis,
                        officeCoverPositionsLockName,
                        TIMEOUT_LOCK_COVER_POSITIONS,
                        EXPIRY_LOCK_COVER_POSITIONS);
                try{
                    if(lock.acquire()){

                        Map<UUID, CoverPosition> positions = null;
                        if(!jedis.exists(officeCoverPositionsKeyName)){
                            positions = new HashMap<UUID, CoverPosition>();
                        }else {
                            positions = gson.fromJson(
                                    jedis.get(officeCoverPositionsKeyName),
                                    TYPE_COV_POSITIONS_MAP);
                        }

                        if(positions.containsKey(position.getInternalid())){

                            if(commandVerb.equalsIgnoreCase("createUpdateCoverPosition")){

                                positions.replace(position.getInternalid(), position);


                            }else if(commandVerb.equalsIgnoreCase("closeCoverPosition")){

                                positions.remove(position.getInternalid());
                            }

                        }else{

                            if(commandVerb.equalsIgnoreCase("createUpdateCoverPosition")){

                                position.setInternalid(UUID.randomUUID());
                                position.setOpentime(new Date());
                                positions.put(position.getInternalid(), position);

                            }else if(commandVerb.equalsIgnoreCase("closeCoverPosition")){

                                if(!positions.containsKey(position.getInternalid())) {

                                    Logger.getLogger(
                                            this.getClass().getName()
                                    ).log(Level.INFO,
                                            "Office cover position",
                                            "position does not exist " + position.getInternalid());
                                }else{
                                    positions.remove(position.getInternalid());
                                }
                            }
                        }

                        String serializedPositions = gson.toJson(positions);
                        jedis.set(officeCoverPositionsKeyName, serializedPositions);

                        lock.release();
                    }else{
                        _log.log(Level.INFO, "Office cover position", "Unable to lock " + officeCoverPositionsLockName);
                    }
                }catch(Exception ex){
                    _log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }

        }

        return ret;
    }
    
    private Map<String, String> processClientCommand(User user, Map<String, String> request, String dealerName)
    {
        Map<String, String> clientUpdate = new HashMap<>();

        String commandVerb = request.get("command");
        UUID orderid = UUID.fromString(request.get("orderId"));

        String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());
        //Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();

        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName), TYPE_POSITIONS_MAP);
                    Position position = positions.get(orderid);
                    BigDecimal pl = BigDecimal.ZERO;

                    if(null == position){
                        clientUpdate.put("order_status","Order " + orderid + " not found.");
                        return clientUpdate;
                    }

                    try {

                        switch (commandVerb) {
                            case "approve": {
                                if(position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_PENDING_OPEN)) {
                                    position.setOrderState(Constants.ORDER_STATE_OPEN);
                                    clientUpdate.put("order_status", "Order [" + orderid + "] approved open by " + dealerName);

                                }else if(position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_PENDING_CLOSE)){
                                    position.setOrderState(Constants.ORDER_STATE_CLOSED);
                                    clientUpdate.put("order_status", "Order [" + orderid + "] approved close by " + dealerName);
                                    positions.remove(orderid);



                                }
                            }
                            break;
                            case "reject": {
                                if(position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_PENDING_OPEN)) {
                                    position.setOrderState(Constants.ORDER_STATE_REJECTED_OPEN);
                                    clientUpdate.put("order_status", "Order [" + orderid + "] rejected open by " + dealerName);
                                    positions.remove(orderid);

                                }else if(position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_PENDING_CLOSE)){
                                    position.setOrderState(Constants.ORDER_STATE_OPEN);
                                    clientUpdate.put("order_status", "Order [" + orderid + "] rejected close by " + dealerName);
                                }
                                //post the pl
                            }
                            break;
                            case "requote": {
                                //position.setOrderState(Position.ORDER_STATE_OPEN);
                            }
                            break;
                        }

                        //update client positions
                        String serializedPositions = gson.toJson(positions);
                        jedis.set(userPositionsKeyName, serializedPositions);

                    }catch(Exception ex){
                        _log.log(Level.SEVERE, ex.getMessage(), ex);
                    }

                    lock.release();

                }else{
                    _log.log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
                }
            }catch(Exception ex){
                _log.log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
            }
        }

        return clientUpdate;
    }

}
