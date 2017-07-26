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

import com.computedsynergy.hurtrade.hurcpu.cpu.Tasks.ClientAccountTask;
import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.CommodityUserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.OfficeModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CommodityUser;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Office;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.GeneralUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class Bootstrap extends AmqpBase{

    private ArrayList<ClientAccountTask> clientTasks = new ArrayList<>();

    public void bootstrap() throws Exception{

        //make sure we are open before any trades come in
        GeneralUtil.refreshTradingSchedule();

        //setup amqp
        setupAMQP();
        //setup queues and exchanges
        bootstrapExchanges();
        
        cleanup();
    }
    
    /**
     * 
     * @throws Exception 
     */
    protected void bootstrapExchanges() throws Exception{

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-max-length", CommandLineOptions.getInstance().maxQueuedMessages); //retain only x messages
        args.put("x-message-ttl", CommandLineOptions.getInstance().maxQueueTtl); //retain only for x seconds

        //fetch all offices
        OfficeModel offices = new OfficeModel();
        List<Office> officeList = offices.getAllOffices();
        //fetch all users for each office
        UserModel users = new UserModel();

        CommodityUserModel cuModel = new CommodityUserModel();

        for(Office o:officeList){

            //get all clients of this office
            List<User> userList = users.getAllUsersForOffice(o.getId());

            for(User u : userList){

                List<CommodityUser> userCommodities = cuModel.getCommoditiesForUser(u.getId());
                RedisUtil.getInstance().cacheUserCommodities(u.getUseruuid(), userCommodities);

                //associate as we are not fetching relationship from db
                u.setUserOffice(o);

                //restore saved positions from db to redis
                if(u.getUsertype().equalsIgnoreCase(Constants.USERTYPE_TRADER))
                GeneralUtil.loadClientPositions(u);

                //
                clientTasks.add(new ClientAccountTask(u));
            }
        }
    }
}
