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

import com.computedsynergy.hurtrade.hurcpu.cpu.RequestConsumers.BackOfficeRequestConsumer;
import com.computedsynergy.hurtrade.hurcpu.cpu.Tasks.OfficePositionsDispatchTask;
import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.OfficeModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Office;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class BackOfficeRequestProcessor extends AmqpBase {

    //basic consumes on all of the office request queues
    public void initialize() throws IOException, TimeoutException {

        super.setupAMQP();

        //fetch all offices
        OfficeModel offices = new OfficeModel();
        List<Office> officeList = offices.getAllOffices();

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-max-length", CommandLineOptions.getInstance().maxQueuedMessages); //retain only x messages
        args.put("x-message-ttl", CommandLineOptions.getInstance().maxQueueTtl); //retain only for x seconds

        //as we will be receiving information on connected users, don't want to crash first time app is run
        channel.exchangeDeclare(
                CommandLineOptions.getInstance().mqExchangeNameStats,
                "fanout",
                true
        );


        for (Office o : officeList) {

            String officeExchangeName = MqNamingUtil.getOfficeExchangeName(o.getOfficeuuid());
            String officeDealerInQName = MqNamingUtil.getOfficeDealerINQueueName(o.getOfficeuuid());
            String officeDealerOutQName = MqNamingUtil.getOfficeDealerOutQueueName(o.getOfficeuuid());

            //start office position dispatch task
            OfficePositionsDispatchTask officePositionsDispatchTask = new OfficePositionsDispatchTask(officeExchangeName, o.getId());
            officePositionsDispatchTask.initialize();

            //declare and bind to queue
            channel.exchangeDeclare(officeExchangeName, "direct", true);
            channel.queueDeclare(officeDealerOutQName, true, false, false, args);
            channel.queueDeclare(officeDealerInQName, true, false, false, args);

            //bind the stats exchange to this office's exchange
            channel.exchangeBind(officeExchangeName, CommandLineOptions.getInstance().mqExchangeNameStats, "connections");

            channel.queueBind(officeDealerOutQName, officeExchangeName, "connections");
            channel.queueBind(officeDealerOutQName, officeExchangeName, "todealer");
            channel.queueBind(officeDealerInQName, officeExchangeName, "fromdealer");

            Channel consumerChannel = CreateNewChannel();
            BackOfficeRequestConsumer consumer = new BackOfficeRequestConsumer(
                    consumerChannel,
                    officeExchangeName,
                    officeDealerInQName,
                    officeDealerOutQName,
                    o
            );
            consumerChannel.basicConsume(officeDealerInQName, false, officeExchangeName, consumer);

        }
    }
}
