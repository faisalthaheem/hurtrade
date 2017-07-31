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
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

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
public class AmqpConnectionFactory {

    protected ConnectionFactory factory = null;
    protected Connection connection = null;
    private static AmqpConnectionFactory _instance = null;

    //logging
    private Logger _log = Logger.getLogger(this.getClass().getName());

    public static AmqpConnectionFactory GetInstance(){

        if(null == _instance){
            _instance = new AmqpConnectionFactory();
            _instance.setupAMQP();
        }

        return _instance;
    }

    private AmqpConnectionFactory(){

    }

    /**
     * Sets the ConnectionFactory parameters
     * @throws IOException
     * @throws TimeoutException
     */
    private void setupAMQP() {

        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }

            factory = new ConnectionFactory();
            factory.setHost(CommandLineOptions.getInstance().mqHost);
            _log.info("mqHosts: " + CommandLineOptions.getInstance().mqHost);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setUsername(CommandLineOptions.getInstance().mqUsername);
            factory.setPassword(CommandLineOptions.getInstance().mqPassword);
            //are we targeting a cluster?
            if (CommandLineOptions.getInstance().mqHost.contains(",")) {

                //conver the string into "Address" objects
                List<Address> nodeAddresses = new ArrayList<>();

                String[] addresses = CommandLineOptions.getInstance().mqHost.split(",");
                for (String address : addresses) {
                    nodeAddresses.add(new Address(address));
                }
                //try to connect
                connection = factory.newConnection(nodeAddresses.toArray(new Address[0]));
            } else {
                connection = factory.newConnection();
            }


        }catch(Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

    }

    public Channel CreateChannel(){

        Channel chan = null;

        try{
            if(connection == null){
                setupAMQP();
            }
            chan = connection.createChannel();
        }catch(Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

        return chan;
    }
    
    protected void cleanup() throws Exception
    {
        connection.close();
    }
}
