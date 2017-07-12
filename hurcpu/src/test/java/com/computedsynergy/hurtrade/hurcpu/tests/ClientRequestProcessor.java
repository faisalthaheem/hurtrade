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
package com.computedsynergy.hurtrade.hurcpu.tests;

import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade.TradeRequest;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class ClientRequestProcessor extends AmqpBase {
    
    String clientExchangeName;
    AMQP.BasicProperties properties;
    Gson gson = new Gson();
            
    public ClientRequestProcessor() {
        
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    @Test
    public void purchaseValidCommodity() throws IOException{
        
        TradeRequest request = new TradeRequest(TradeRequest.REQUEST_TYPE_BUY, "USDEUR", BigDecimal.valueOf(0.48), BigDecimal.valueOf(0.01), new Date(), null);
        String serialized = gson.toJson(request);

        channel.basicPublish(clientExchangeName, "request", properties, serialized.getBytes());
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
        super.setupAMQP();
        
        UserModel userModel = new UserModel();
        User client = userModel.getByUsername("guest");
        clientExchangeName = MqNamingUtil.getClientExchangeName(client.getUseruuid());
        
        properties = new AMQP.BasicProperties();
        properties = properties.builder().userId(client.getUsername()).build();
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
        super.cleanup();
    }
}
