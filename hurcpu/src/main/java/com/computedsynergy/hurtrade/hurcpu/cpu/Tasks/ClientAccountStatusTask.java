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
package com.computedsynergy.hurtrade.hurcpu.cpu.Tasks;

import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class ClientAccountStatusTask extends AmqpBase {

    private final String officeExhcangeName;
    private final int officeId;
    private List<String> officeUsers  = new ArrayList<>();

    private Object lockOfficeUsers = new Object(); //governs access to officeUsers
    private Object lockChannelWrite = new Object(); //governs access to write access on mq channel

    private Timer tmrUserKeysUpdateTimer = new Timer(true);
    private Timer tmrOfficePositionsDispatchTimer = new Timer(true);


    private Gson gson = new Gson();

    public void initialize() throws IOException, TimeoutException {

        try{
            super.setupAMQP();
        }catch (Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

    }

    public ClientAccountStatusTask(
                                       String officeExchangeName,
                                       int officeId
    ) {

        this.officeExhcangeName = officeExchangeName;
        this.officeId = officeId;

        //get list of all users for the office and store them for faster access, we will later require this
        //list to create update events to send to the dealers
        updateOfficeUsers();

        SetupTimers();
    }

    private void SetupTimers(){

        //update list as defined
        tmrUserKeysUpdateTimer.schedule(
            new TimerTask() {
                @Override
                public void run() {
                    updateOfficeUsers();
                }
            },
            CommandLineOptions.getInstance().usersKeysUpdateTimerInterval,
            CommandLineOptions.getInstance().usersKeysUpdateTimerInterval
        );

        //send office positions to dealers
        tmrOfficePositionsDispatchTimer.schedule(
            new TimerTask(){
                @Override
                public void run() {
                dispatchPositions();
                }
            },
            CommandLineOptions.getInstance().officePositionsUpdateTimer,
            CommandLineOptions.getInstance().officePositionsUpdateTimer
        );
    }

    private void updateOfficeUsers(){

        try {
            UserModel uModel = new UserModel();

            List<User> users = uModel.getAllUsersForOffice(officeId);
            synchronized (lockOfficeUsers) {

                officeUsers.clear(); //to ensure we remove users who have been closed, disabled etc

                for (User uItem : users) {
                    officeUsers.add(uItem.getUsername());
                }
            }
        }catch(Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void dispatchPositions(){

        try {
            //get positions for all clients
            List<String> usernames = null;
            Map<String, List<Position>> userPositions = new HashMap<>();

            synchronized (lockOfficeUsers) {
                usernames = new ArrayList<>(officeUsers);
            }
            for (String username : usernames) {
                List<Position> positions = RedisUtil.getInstance().GetUserPositions(username);
                if (positions.size() > 0) {
                    userPositions.put(username, positions);
                }
            }

            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            builder.type("officePositions");
            AMQP.BasicProperties props = builder.build();

            String json = gson.toJson(userPositions);
            synchronized (lockChannelWrite) {
                try {
                    channel.basicPublish(officeExhcangeName, "todealer", props, json.getBytes());
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }catch (Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
