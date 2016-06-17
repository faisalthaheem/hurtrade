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
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.TradeRequest;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.OfficeModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Office;
import com.computedsynergy.hurtrade.sharedcomponents.util.HurUtil;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class ClientRequestProcessor extends AmqpBase {
    
    //basic consumes on all of the office request queues
    public void initialize() throws IOException{
        
        //fetch all offices
        OfficeModel offices = new OfficeModel();
        List<Office> officeList = offices.getAllOffices();
        //fetch all users for each office
        UserModel users = new UserModel();
        for(Office o:officeList){
            
            String officeExchangeName = HurUtil.getOfficeExchangeName(o.getOfficeuuid());
            String officeClientRequestQueueName = HurUtil.getOfficeClientRequestQueueName(o.getOfficeuuid());
            
            channel.basicConsume(officeClientRequestQueueName, false, officeExchangeName, 
                    new DefaultConsumer(channel){
                        @Override
                        public void handleDelivery(String consumerTag,
                                    Envelope envelope,
                                    AMQP.BasicProperties properties,
                                    byte[] body)
                            throws IOException
                        {
                            String routingKey = envelope.getRoutingKey();
                            String contentType = properties.getContentType();
                            String clientName = properties.getUserId();
                            long deliveryTag = envelope.getDeliveryTag();
                            // (process the message components here ...)
                            
                            try{
                                
                                TradeRequest request = TradeRequest.fromJson(new String(body));
                                switch(request.getRequestType()){
                                    case TradeRequest.REQUEST_TYPE_BUY:
                                        break;
                                    case TradeRequest.REQUEST_TYPE_SELL:
                                        break;
                                }
                                
                            }catch(Exception ex){
                                //todo log here
                            }
                            
                            channel.basicAck(deliveryTag, false);
                        }
                    }
            );
        }
    }

}
