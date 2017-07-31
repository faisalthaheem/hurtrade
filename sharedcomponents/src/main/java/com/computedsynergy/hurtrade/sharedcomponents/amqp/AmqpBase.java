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
package com.computedsynergy.hurtrade.sharedcomponents.amqp;

import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.AMQChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class AmqpBase {

    protected Channel channel = null;
    private AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();

    //lock for accessing this instance's channel
    private Object lockChannel = new Object();

    //logging
    protected Logger _log = Logger.getLogger(this.getClass().getName());


    /**
     * Sets the ConnectionFactory parameters
     * @throws java.io.IOException
     * @throws java.util.concurrent.TimeoutException
     */
    protected void setupAMQP() {

        channel = CreateNewChannel();

    }
    
    protected void cleanup()
    {

    }

    protected Channel CreateNewChannel()
    {

        Channel ret;

        int triesLeft = 10;
        boolean ready = false;

        do {

            ret = AmqpConnectionFactory.GetInstance().CreateChannel();

            if(ret !=  null){
                ready = true;

                _log.info("AMQP Connectivty Good to go.");
                break;
            }

            String message = String.format("Unable to connect to amqp, will wait for 30 seconds before trying again [%d] times.", triesLeft);
            _log.severe(message);

            try {

                if(!ready) {
                    Thread.sleep(30 * 1000);
                }

            }catch(Exception ex){

                _log.severe(ex.getMessage());
            }

        }while(triesLeft-- > 0);


        return ret;
    }


    protected void publishMessage(String exchange,
                                String routingKey,
                                String messageTypeProperty,
                                String payload)
    {
        synchronized (lockChannel) {

            propsBuilder.type(messageTypeProperty);
            AMQP.BasicProperties props = propsBuilder.build();

            try{
                channel.basicPublish(
                        exchange,
                        routingKey,
                        props,
                        payload.getBytes()
                );
            }catch(Exception ex){

                _log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
}
