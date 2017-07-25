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
package com.computedsynergy.hurtrade.hurcpu.cpu;

import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 *
 * Subscribes to the rate queue and notifies the clients in case there is an
 * update after applying client specific spreads
 */
public class AuthRequestProcessor extends AmqpBase {

    Gson gson = new Gson();

    public void init() throws Exception {
        
        super.setupAMQP();

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-max-length", CommandLineOptions.getInstance().maxQueuedMessages); //retain only x messages
        args.put("x-message-ttl", CommandLineOptions.getInstance().maxQueueTtl); //retain only for x seconds

        channel.exchangeDeclare(Constants.EXCHANGE_NAME_AUTH, "direct",true, false, null);

        channel.queueDeclare(Constants.QUEUE_NAME_AUTH, true, true, false, args);

        channel.queueBind(Constants.QUEUE_NAME_AUTH, Constants.EXCHANGE_NAME_AUTH, "");

        channel.basicConsume(Constants.QUEUE_NAME_AUTH, false, "AuthConsumer",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag,
                            Envelope envelope,
                            AMQP.BasicProperties properties,
                            byte[] body)
                    throws IOException {
                        String routingKey = envelope.getRoutingKey();
                        String contentType = properties.getContentType();
                        String replyToQueue = properties.getReplyTo();
                        long deliveryTag = envelope.getDeliveryTag();

                        Map<String,String> dic = new Gson().fromJson(new String(body), new TypeToken<HashMap<String, String>>() {}.getType());
                        processAuthRequest(dic, properties.getUserId(), replyToQueue);

                        channel.basicAck(deliveryTag, false);
                    }
                });
    }

    private void processAuthRequest(Map<String,String> reqMap, String username, String replyTo) {

        Map<String,String> res = new HashMap<String, String>();

        if(reqMap!=null && reqMap.keySet().size() > 0){

            User u =RedisUtil.getInstance().GetUserInfo(username);
            if(null != u) {


                String clientExchangeName = MqNamingUtil.getClientExchangeName(u.getUseruuid());
                res.put("clientExchangeName", clientExchangeName);
                String responseQueueName = MqNamingUtil.getClientOutgoingQueueName(u.getUseruuid());
                res.put("responseQueueName", responseQueueName);


                if(u.getUsertype().equalsIgnoreCase(Constants.USERTYPE_DEALER)){
                    String officeExchangeName = MqNamingUtil.getOfficeExchangeName(u.getUserOffice().getOfficeuuid());
                    String officeDealerOutQName = MqNamingUtil.getOfficeDealerOutQueueName(u.getUserOffice().getOfficeuuid());
                    String officeDealerInQName = MqNamingUtil.getOfficeDealerINQueueName(u.getUserOffice().getOfficeuuid());
                    res.put("officeExchangeName", officeExchangeName);
                    res.put("officeDealerOutQName", officeDealerOutQName);
                        res.put("officeDealerInQName", officeDealerInQName);
                }


                String json = new Gson().toJson(res);
                try {
                    channel.basicPublish(
                            Constants.EXCHANGE_NAME_AUTH,
                            username,
                            null,
                            json.getBytes());
                } catch (IOException ex) {
                    _log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }

        }else{
            res.put("officeExchange","unauthorized");
        }
    }

    
}
