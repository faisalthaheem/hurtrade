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
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverAccount;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
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

import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.*;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class BackOfficeRequestConsumer extends DefaultConsumer {

    private final String officeExhcangeName;
    private final String dealerInQueueName;
    private final String dealerOutQueueName;
    private final int officeId;

    UserModel userModel = new UserModel();
    private Object lockChannelWrite = new Object(); //governs access to write access on mq channel


    public BackOfficeRequestConsumer(Channel channel,
                         String officeExchangeName,
                         String dealerInQueueName,
                         String dealerOutQueueName,
                         int officeId
    ) {
        super(channel);
        this.officeExhcangeName = officeExchangeName;
        this.dealerInQueueName = dealerInQueueName;
        this.dealerOutQueueName = dealerOutQueueName;
        this.officeId = officeId;

    }



    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        super.handleDelivery(consumerTag, envelope, properties, body); //To change body of generated methods, choose Tools | Templates.

        long deliveryTag = envelope.getDeliveryTag();
        getChannel().basicAck(deliveryTag, false);

        try {


            String clientExchangeName = null;
            Map<String, String> response = null;
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();


            User user = null;

            String requestType = properties.getType();
            String dealerName = properties.getUserId();

            Map<String, String> request = new Gson().fromJson(new String(body), Constants.TYPE_DICTIONARY);


            if(requestType.equalsIgnoreCase("client")) {

                user = userModel.getByUsername(request.get("client"));
                if (user == null) {
                    Logger.getLogger(this.getClass().getName())
                            .log(Level.SEVERE,
                                    "Could not resolve user for name: ", properties.getUserId());
                    return;
                }

                response = processClientCommand(user, request, dealerName);
                clientExchangeName = MqNamingUtil.getClientExchangeName(user.getUseruuid());
                builder.type("orderUpdate");

            }else if(requestType.equalsIgnoreCase("office")) {

                user = userModel.getByUsername(dealerName);
                response = processOfficeCommand(request, dealerName);

                builder.type("commandResult");
            }

            if(null != response) {
                String serializedResponse = new Gson().toJson(response);

                synchronized (lockChannelWrite) {
                    getChannel().basicPublish(
                            clientExchangeName,
                            "response",
                            builder.build(),
                            serializedResponse.getBytes()
                    );
                }
            }

        }catch (Exception ex) {

            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Map<String, String> processOfficeCommand(Map<String, String> request, String dealerName)
            throws Exception
    {
        Map<String, String> ret = new HashMap<>();

        String commandVerb = request.get("command");

        if(commandVerb.equalsIgnoreCase("listCoverAccounts")){

            User user = userModel.getByUsername(dealerName);
            String exchangeName = MqNamingUtil.getClientExchangeName(user.getUseruuid());

            List<CoverAccount> coverAccounts =
                    new CoverAccountModel().listCoverAccountsForOffice(officeId);

            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            String serializedResponse = new Gson().toJson(coverAccounts);

            synchronized (lockChannelWrite) {
                getChannel().basicPublish(
                        exchangeName,
                        "response",
                        builder.build(),
                        serializedResponse.getBytes()
                );
            }
            ret = null;
        }else if(
                commandVerb.equalsIgnoreCase("createUpdateCoverPosition")
                ||
                commandVerb.equalsIgnoreCase("closeCoverPosition")
        ){

        }

        return ret;
    }
    
    private Map<String, String> processClientCommand(User user, Map<String, String> request, String dealerName)
    {
        Map<String, String> clientUpdate = new HashMap<>();

        String commandVerb = request.get("command");
        UUID orderid = UUID.fromString(request.get("orderId"));

        String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());
        Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();

        Gson gson = new Gson();

        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName),mapType);
                    Position position = positions.get(orderid);
                    BigDecimal pl = BigDecimal.ZERO;

                    if(null == position){
                        clientUpdate.put("order_status","Order " + orderid + " not found.");
                        return clientUpdate;
                    }

                    try {

                        switch (commandVerb) {
                            case "approve": {
                                if(position.getOrderState().equalsIgnoreCase(Position.ORDER_STATE_PENDING_OPEN)) {
                                    position.setOrderState(Position.ORDER_STATE_OPEN);
                                    clientUpdate.put("order_status", "Order [" + orderid + "] approved open by " + dealerName);

                                }else if(position.getOrderState().equalsIgnoreCase(Position.ORDER_STATE_PENDING_CLOSE)){
                                    position.setOrderState(Position.ORDER_STATE_CLOSED);
                                    clientUpdate.put("order_status", "Order [" + orderid + "] approved close by " + dealerName);
                                    positions.remove(orderid);

                                    //realize position P/L
                                    new LedgerModel().SaveRealizedPositionPL(user.getId(), position.getOrderId(), position.getCurrentPl());

                                }
                            }
                            break;
                            case "reject": {
                                if(position.getOrderState().equalsIgnoreCase(Position.ORDER_STATE_PENDING_OPEN)) {
                                    position.setOrderState(Position.ORDER_STATE_REJECTED_OPEN);
                                    clientUpdate.put("order_status", "Order [" + orderid + "] rejected open by " + dealerName);
                                    positions.remove(orderid);

                                }else if(position.getOrderState().equalsIgnoreCase(Position.ORDER_STATE_PENDING_CLOSE)){
                                    position.setOrderState(Position.ORDER_STATE_OPEN);
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

                        //update the current order state to db
                        PositionModel positionModel = new PositionModel();
                        positionModel.saveUpdatePosition(position);

                        //update client positions
                        String serializedPositions = gson.toJson(positions);
                        jedis.set(userPositionsKeyName, serializedPositions);

                    }catch(Exception ex){
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
                    }

                    lock.release();

                }else{
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
                }
            }catch(Exception ex){
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
            }
        }

        return clientUpdate;
    }

}
