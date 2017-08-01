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

import com.computedsynergy.hurtrade.sharedcomponents.models.impl.NotificationModel;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */

public class CustomDefaultConsumer extends DefaultConsumer {

    protected Channel channel = null;
    private AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();

    //lock for accessing this instance's channel
    private Object lockChannel = new Object();

    //logging
    protected Logger _log = Logger.getLogger(this.getClass().getName());
    NotificationModel dbNotification = new NotificationModel();

    public CustomDefaultConsumer(Channel channel) {
        super(channel);
        this.channel = channel;
    }


    /**
     * Prepends time to the notification message and sends it out
     * message type property is set to 'notification'
     *
     * @param notificationForUser if > 0 then notificationForOffice is ignored and data row refers to an event specifically for the trader
     * @param notificationForOffice if > 0 then notificationForUser is ignored and data row refers to an event for the office
     * @param exchange
     * @param routingKey
     * @param notification
     */
    protected void publishNotificationMessage(
                                int notificationForUser,
                                int notificationForOffice,
                                String exchange,
                                String routingKey,
                                String notification)
    {

        notification = String.format("%s:\n%s\n", new Date().toString(), notification);

        publishMessage(
                exchange,
                routingKey,
                "notification",
                notification
        );

        dbNotification.saveNotification(notificationForUser, notificationForOffice, notification);
    }

    protected void publishMessage(String exchange,
                                  String routingKey,
                                  String messageTypeProperty,
                                  String payload)
    {
        synchronized (lockChannel) {

            if(null != messageTypeProperty) {
                propsBuilder.type(messageTypeProperty);
            }

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
