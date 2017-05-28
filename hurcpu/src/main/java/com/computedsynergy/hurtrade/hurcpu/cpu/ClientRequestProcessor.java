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

import com.computedsynergy.hurtrade.hurcpu.cpu.RequestProcessor.RequestConsumer;
import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.OfficeModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Office;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.HurUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class ClientRequestProcessor extends AmqpBase {

    //basic consumes on all of the office request queues
    public void initialize() throws IOException, TimeoutException {

        super.setupAMQP();

        //fetch all offices
        OfficeModel offices = new OfficeModel();
        List<Office> officeList = offices.getAllOffices();
        UserModel uModel = new UserModel();


        for (Office o : officeList) {

            String officeExchangeName = HurUtil.getOfficeExchangeName(o.getOfficeuuid());
            String officeClientRequestQueueName = HurUtil.getOfficeClientRequestQueueName(o.getOfficeuuid());
            RequestConsumer consumer = new RequestConsumer(channel, officeExchangeName, officeClientRequestQueueName);

            channel.basicConsume(officeClientRequestQueueName, false, officeExchangeName, consumer);

            //update info
            List<User> users = uModel.getAllUsersForOffice(o.getId());
            for(User uItem : users){
                User u = RedisUtil.getInstance().GetUserInfo(uItem.getUsername());
                if(u == null){
                    u = uItem;
                }
                uItem.setUserOffice(o);
                RedisUtil.getInstance().SetUserInfo(uItem);
            }

        }
    }

}
