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

import com.computedsynergy.hurtrade.hurcpu.cpu.ClientRequestProcessor;
import com.computedsynergy.hurtrade.hurcpu.cpu.CommodityUpdateProcessor;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.positions.Position;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade.TradeRequest;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade.TradeResponse;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.updates.ClientUpdate;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.HurUtil;
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
    private List<String> officeUsers  = new ArrayList<>();

    private Object lockOfficeUsers = new Object(); //governs access to officeUsers
    private Object lockChannelWrite = new Object(); //governs access to write access on mq channel

    private Timer tmrUserKeysUpdateTimer = new Timer(true);
    private Timer tmrOfficePositionsDispatchTimer = new Timer(true);


    private Gson gson = new Gson();

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

            User user = null;
            Map<String, String> clientUpdate = null;

            Type requestMapType = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> request = gson.fromJson(new String(body),requestMapType);

            UserModel userModel = new UserModel();
            user = userModel.getByUsername(request.get("client"));
            if (user == null) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Could not resolve user for name: ", properties.getUserId());
                return;
            }

            clientUpdate = processCommand(user, properties.getType(), UUID.fromString(request.get("orderId")), properties.getUserId());

            String clientExchangeName = HurUtil.getClientExchangeName(user.getUseruuid());
            String serializedResponse = gson.toJson(clientUpdate);

            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            builder.type("orderUpdate");
            getChannel().basicPublish(clientExchangeName, "response", builder.build(), serializedResponse.getBytes());

        }catch (Exception ex) {

            Logger.getLogger(CommodityUpdateProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Map<String, String>  processCommand(User user, String commandVerb, UUID orderid, String dealerName)
    {
        Map<String, String> clientUpdate = new HashMap<>();

        String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());
        Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();
        
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
                            }
                            break;
                            case "requote": {
                                //position.setOrderState(Position.ORDER_STATE_OPEN);
                            }
                            break;
                        }
                        //post the pl
                        if(pl != BigDecimal.ZERO) {
                            //todo post to account
                        }
                        //todo update the current order state to db

                        //update client positions
                        String serializedPositions = gson.toJson(positions);
                        jedis.set(userPositionsKeyName, serializedPositions);

                    }catch(Exception ex){
                        Logger.getLogger(CommodityUpdateProcessor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
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
