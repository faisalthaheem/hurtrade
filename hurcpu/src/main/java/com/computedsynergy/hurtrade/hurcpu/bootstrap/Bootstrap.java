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
package com.computedsynergy.hurtrade.hurcpu.bootstrap;

import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.CommodityUserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.OfficeModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CommodityUser;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Office;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.computedsynergy.hurtrade.sharedcomponents.util.HurUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class Bootstrap extends AmqpBase{
    
    public void bootstrap() throws Exception{
        
        //setup amqp
        setupAMQP();
        //setup queues and exchanges
        bootstrapExchanges();
        //setup cached user data
        setupRedisUserSpreads();
        
        cleanup();
    }
    
    /**
     * 
     * @throws Exception 
     */
    protected void bootstrapExchanges() throws Exception{


        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-max-length", 5); //retain only 5 latest messages for clients

        //fetch all offices
        OfficeModel offices = new OfficeModel();
        List<Office> officeList = offices.getAllOffices();
        //fetch all users for each office
        UserModel users = new UserModel();
        for(Office o:officeList){
            
            String officeExchangeName = HurUtil.getOfficeExchangeName(o.getOfficeuuid());
            String officeClientRequestQueueName = HurUtil.getOfficeClientRequestQueueName(o.getOfficeuuid());
            String officeDealerOutQName = HurUtil.getOfficeDealerOutQueueName(o.getOfficeuuid());
            String officeDealerInQName = HurUtil.getOfficeDealerINQueueName(o.getOfficeuuid());
            
            //declare a queue for this office for incoming messages from the clients
            channel.exchangeDeclare(officeExchangeName, "fanout", true);
            channel.queueDeclare(officeDealerOutQName, true, false, false, args);
            channel.queueDeclare(officeDealerInQName, true, false, false, null);
            
            channel.queueBind(officeDealerOutQName, officeExchangeName, "todealer");
            channel.queueBind(officeDealerInQName, officeExchangeName, "fromdealer");

            //requests sent by client on their exchanges are delivered to this queue bound in following code
            channel.queueDeclare(officeClientRequestQueueName, true, false, false, null); //used below with client exchanges
            
            //get all clients of this office
            List<User> userList = users.getAllUsersForOffice(o.getId());
            //foreach user declare an exchange where user publishes
            //bind the exchange to the office queue this user belongs to on routing key "request" and create an additional "out" queue for the messages
            //sent to the user bound by "response"
            for(User u : userList){
                String clientExchangeName = HurUtil.getClientExchangeName(u.getUseruuid());
                channel.exchangeDeclare(clientExchangeName, "direct", true);
                channel.queueBind(officeClientRequestQueueName, clientExchangeName, "request");
                
                String clientOutgoingQueueName = HurUtil.getClientOutgoingQueueName(u.getUseruuid());
                channel.queueDeclare(clientOutgoingQueueName, true, false, false, args);
                channel.queueBind(clientOutgoingQueueName, clientExchangeName, "response");
            }
        }
        
    }
    
    /**
     * Loads the users 
     */
    protected void setupRedisUserSpreads(){
        
        
        UserModel userModel = new UserModel();
        List<User> users = userModel.getAllUsers();
        
        CommodityUserModel cuModel = new CommodityUserModel();
                
        for(User u:users){
            
            List<CommodityUser> userCommodities = cuModel.getCommoditiesForUser(u.getId());
            RedisUtil.getInstance().setUserSpreadMap(RedisUtil.getUserSpreadMapName(u.getUseruuid()), userCommodities);

        }
                
    }
}
