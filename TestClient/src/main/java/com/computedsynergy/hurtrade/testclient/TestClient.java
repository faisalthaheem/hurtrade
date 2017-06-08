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
package com.computedsynergy.hurtrade.testclient;

import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.updates.ClientUpdate;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class TestClient extends AmqpBase {
    
    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException{
        
        TestClient tc = new TestClient();
        tc.initialize();
        
        while(true)
        {
            Thread.sleep(100000);
        }
    }
    
    Gson gson = new Gson();
    public void initialize() throws IOException, TimeoutException{
        
        super.setupAMQP();
        
        String clientQueueName = "9be1cd93-cec6-472b-be9d-3c2420c62b5a_client_outgoing";
        
        channel.queueDeclare(clientQueueName, true, false, false, null);

        channel.basicConsume(clientQueueName, true, "ClientConsumer_9be1cd93-cec6-472b-be9d-3c2420c62b5a",
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

                        ClientUpdate update = gson.fromJson(new String(body), ClientUpdate.class);
                        
                        for(Position p :update.getPositions().values()){
                            String printLine = String.format("%s -> %s\tl:%f\tp/l:%f", p.getOrderId().toString(), p.getCommodity(), p.getAmount(), p.getCurrentPl());
                            System.out.println(printLine);
                        }

                        
                    }
                });
    }
}
