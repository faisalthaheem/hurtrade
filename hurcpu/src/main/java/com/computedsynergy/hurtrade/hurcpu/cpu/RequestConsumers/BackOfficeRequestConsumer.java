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

import com.computedsynergy.hurtrade.sharedcomponents.amqp.CustomDefaultConsumer;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.updates.ClientUpdate;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.CoverAccountModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.*;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import com.github.jedis.lock.JedisLock;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
public class BackOfficeRequestConsumer extends CustomDefaultConsumer {

    private final String officeExhcangeName;
    private final String dealerInQueueName;
    private final String dealerOutQueueName;
    //private final int officeId;
    private final Office _office;

    UserModel userModel = new UserModel();
    private Object lockChannelWrite = new Object(); //governs access to write access on mq channel
    private Gson gson = new Gson();

    private DecimalFormat decimalParser = new DecimalFormat();

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

        decimalParser.setParseBigDecimal(true);

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


            User user;

            String requestType = properties.getType();
            String dealerName = properties.getUserId();

            Map<String, String> request = gson.fromJson(new String(body), Constants.TYPE_DICTIONARY);


            if(requestType.equalsIgnoreCase("client")) {

                user = resolveUser(request.get("client"));
                if (user == null) {
                    _log.log(Level.SEVERE,
                            "Could not resolve user for name: ", properties.getUserId());
                    return;
                }

                processClientCommand(user, request, dealerName);

            }else if(requestType.equalsIgnoreCase("office")) {

                user = resolveUser(dealerName);
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
        String notification = null;

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

                                notification = String.format("Cover position [%s] updated.", position.getRemoteid());

                            }else if(commandVerb.equalsIgnoreCase("closeCoverPosition")){

                                positions.remove(position.getInternalid());

                                notification = String.format("Cover position [%s] closed.", position.getRemoteid());
                            }

                        }else{

                            if(commandVerb.equalsIgnoreCase("createUpdateCoverPosition")){

                                position.setInternalid(UUID.randomUUID());
                                position.setOpentime(new Date());
                                positions.put(position.getInternalid(), position);

                                notification = String.format("Cover position [%s] created.", position.getRemoteid());

                            }else if(commandVerb.equalsIgnoreCase("closeCoverPosition")){

                                if(!positions.containsKey(position.getInternalid())) {

                                    Logger.getLogger(
                                            this.getClass().getName()
                                    ).log(Level.INFO,
                                            "Office cover position",
                                            "position does not exist " + position.getInternalid());

                                    notification = String.format("Cover position [%s] does not exist.", position.getRemoteid());
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


        }else if(commandVerb.equalsIgnoreCase("lockuser"))
        {

            //todo: publish to global notification exchange so that user information is updated on
            //all services and nodes

            //lock user in db
            User u = userModel.getByUsername(request.get("username"));
            if(u == null){
                notification = String.format("Received lock request for non-existent user [%s].", request.get("username"));
                _log.info(notification);
            }else{
                u.setLocked(true);
                userModel.updateUser(u);

                //update cache
                RedisUtil.getInstance().SetUserInfo(u);

                //dispatch to upstream to disconnect user
                publishMessage(
                        CommandLineOptions.getInstance().mqExchangeNameStats,
                        "command",
                        "disconnect",
                        request.get("mqName")
                );


                notification = String.format("Successfully locked user [%s].", request.get("username"));
                _log.info(notification);
            }
        }

        if(null != notification){

            publishNotificationMessage(
                    -1,
                    _office.getId(),
                    officeExhcangeName,
                    "todealer",
                    notification
            );
        }

        return ret;
    }

    private User resolveUser(String username){

        User ret = RedisUtil.getInstance().GetUserInfo(username);

        if(null == ret){
            ret = userModel.getByUsername(username);
        }

        return ret;
    }

    private void processClientCommand(User user, Map<String, String> request, String dealerName)
    {

        String commandVerb = request.get("command");
        UUID orderid = UUID.fromString(request.get("orderId"));
        String clientExchangeName = MqNamingUtil.getClientExchangeName(user.getUseruuid());

        String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());

        String notification = null;


        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName), TYPE_POSITIONS_MAP);
                    Position position = positions.get(orderid);

                    if(null == position){
                        notification = String.format("[%s] for [%s] not found.", orderid.toString(), user.getUsername());
                    }else {

                        try {

                            switch (commandVerb) {
                                case "approve": {
                                    if (position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_PENDING_OPEN)) {
                                        position.setOrderState(Constants.ORDER_STATE_OPEN);

                                        notification = String.format(
                                          "[%s][%d] approved open by [%s]",
                                          user.getUsername(),
                                          position.getFriendlyorderid(),
                                          dealerName
                                        );

                                    } else if (position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_PENDING_CLOSE)) {
                                        position.setOrderState(Constants.ORDER_STATE_CLOSED);
                                        positions.remove(orderid);

                                        notification = String.format(
                                                "[%s][%d] approved close by [%s]",
                                                user.getUsername(),
                                                position.getFriendlyorderid(),
                                                dealerName
                                        );

                                    }
                                }
                                break;
                                case "reject": {
                                    if (position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_PENDING_OPEN)) {
                                        position.setOrderState(Constants.ORDER_STATE_REJECTED_OPEN);
                                        positions.remove(orderid);

                                        notification = String.format(
                                                "[%s][%d] rejected open by [%s]",
                                                user.getUsername(),
                                                position.getFriendlyorderid(),
                                                dealerName
                                        );

                                    } else if (position.getOrderState().equalsIgnoreCase(Constants.ORDER_STATE_PENDING_CLOSE)) {
                                        position.setOrderState(Constants.ORDER_STATE_OPEN);

                                        notification = String.format(
                                                "[%s][%d] rejected close by [%s]",
                                                user.getUsername(),
                                                position.getFriendlyorderid(),
                                                dealerName
                                        );
                                    }
                                }
                                break;
                                case "requote": {
                                    position.setRequoteprice(
                                            (BigDecimal) decimalParser.parse(request.get("requoted_price"))
                                    );

                                    position.setOrderState(Constants.ORDER_STATE_REQUOTED);

                                    //set requoted price in redis with an expiry as defined in params
                                    int tiemout = CommandLineOptions.getInstance().requoteNetworkDelay
                                            +
                                            CommandLineOptions.getInstance().requoteTimeout;

                                    RedisUtil.getInstance().SetOrderRequoted(
                                            position.getOrderId(),
                                            tiemout
                                    );

                                    Map<String, String> clientUpdate = new HashMap<>();
                                    //send to client
                                    clientUpdate.put("requote", "yes");
                                    clientUpdate.put("orderid", position.getOrderId().toString());
                                    clientUpdate.put("timeout", "" + tiemout);
                                    clientUpdate.put("requoteprice", "" + position.getRequoteprice());
                                    if (position.is_wasPendingClose()) {
                                        clientUpdate.put("pendingclose", "yes");
                                    } else {
                                        clientUpdate.put("pendingclose", "");
                                    }

                                    //send to client
                                    publishMessage(
                                            clientExchangeName,
                                            "response",
                                            "orderUpdate",
                                            new Gson().toJson(clientUpdate)
                                    );

                                    notification = String.format(
                                            "[%s][%d] requoted by [%s]",
                                            user.getUsername(),
                                            position.getFriendlyorderid(),
                                            dealerName
                                    );
                                }
                                break;
                            }

                            //update client positions
                            String serializedPositions = gson.toJson(positions);
                            jedis.set(userPositionsKeyName, serializedPositions);

                        } catch (Exception ex) {
                            _log.log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    }

                    lock.release();

                }else{
                    _log.log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
                }
            }catch(Exception ex){
                _log.log(Level.SEVERE, null, "Could not process user positions " + user.getUsername());
            }
        }

        if(null != notification) {
            publishNotificationMessage(
                    user.getId(),
                    -1,
                    clientExchangeName,
                    "response",
                    notification
            );

            publishNotificationMessage(
                    -1,
                    _office.getId(),
                    officeExhcangeName,
                    "todealer",
                    notification
            );
        }

    }

}
