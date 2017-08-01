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
package com.computedsynergy.hurtrade.collectors;

import com.beust.jcommander.JCommander;
import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.ConnectionInfo;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.google.gson.*;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */

public class RabbitMqStats extends AmqpBase {

    private final ExecutorService _singleExecutorService;
    private JsonParser _par = new JsonParser();
    private Gson _gson = new Gson();
    private static boolean _keepRunning = true;

    public static void main(String[] args) {

        new JCommander(CommandLineOptions.getInstance(), args);

        RabbitMqStats mqStats = new RabbitMqStats();

        //ensure we exit gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> _keepRunning = false));

        mqStats.run();
    }

    private RabbitMqStats(){
        this._singleExecutorService = Executors.newFixedThreadPool(2);
    }

    private void run(){

        try{

            setupAMQP();

            channel.exchangeDeclare(
                    CommandLineOptions.getInstance().mqExchangeNameStats,
                    "fanout",
                    true
                    );

            channel.queueDeclare(MqNamingUtil.Q_NAME_MQ_STATS_COMMAND, true, false, false, null);
            channel.queueBind(MqNamingUtil.Q_NAME_MQ_STATS_COMMAND, CommandLineOptions.getInstance().mqExchangeNameStats, "command");

            channel.basicConsume(MqNamingUtil.Q_NAME_MQ_STATS_COMMAND, false, "commands_consumer",
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


                            _singleExecutorService.submit(() -> {
                                processCommand(properties, new String(body));
                            });

                            channel.basicAck(deliveryTag, false);
                        }
                    });

        }catch(Exception ex){
            _log.log(Level.SEVERE,ex.getMessage(), ex);
            return;
        }

        while(_keepRunning){
            queryAndPublish();

            try{
                Thread.sleep(CommandLineOptions.getInstance().mqStatsInterval);
            }catch (Exception ex){
                _log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    private void processCommand(AMQP.BasicProperties properties, String payload){

        String type = properties.getType();
        if(type.equalsIgnoreCase("disconnect")){

            String[] users = _gson.fromJson(payload, Constants.TYPE_STRING_ARRAY);

            for(String user : users) {
                disconnectUser(user);
            }
        }
    }

    private void disconnectUser(String username)
    {
        try{
            OkHttpClient client = new OkHttpClient.Builder()
                    .authenticator((route, response) -> {

                        _log.info("Authenticating for response: " + response);
                        _log.info("Challenges: " + response.challenges());

                        String credential = Credentials.basic(
                                CommandLineOptions.getInstance().mqUsername,
                                CommandLineOptions.getInstance().mqPassword
                        );
                        return response.request().newBuilder()
                                .header("Authorization", credential)
                                .build();
                    })
                    .build();



            Request request = new Request.Builder()
                    .url(CommandLineOptions.getInstance().mqRabbitMqManagementBaseUrl + "/api/connections/" + username)
                    .delete()
                    .addHeader("content-type", "application/json")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("X-Reason", "session-killed")
                    .build();

            Response response = client.newCall(request).execute();

            _log.info("Executed disconnect on client [" + username + "] with response code: " + response.code());

        }catch(Exception ex){
            _log.severe(ex.getMessage());
        }
    }

    private void queryAndPublish(){

        try{

            OkHttpClient client = new OkHttpClient.Builder()
                    .authenticator((route, response) -> {

                        _log.info("Authenticating for response: " + response);
                        _log.info("Challenges: " + response.challenges());

                        String credential = Credentials.basic(
                                CommandLineOptions.getInstance().mqUsername,
                                CommandLineOptions.getInstance().mqPassword
                        );
                        return response.request().newBuilder()
                                .header("Authorization", credential)
                                .build();
                    })
                    .build();



            Request request = new Request.Builder()
                    .url(CommandLineOptions.getInstance().mqRabbitMqManagementBaseUrl + "/api/connections")
                    .get()
                    .addHeader("content-type", "application/json")
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();

            List<ConnectionInfo> connections = new ArrayList<>();
            if(response.isSuccessful()){

                JsonElement doc = _par.parse(response.body().string());
                if(doc.isJsonArray()){
                    JsonArray jConns = doc.getAsJsonArray();

                    for(int i=0; i< jConns.size(); i++)
                    {
                        JsonObject jConn = jConns.get(i).getAsJsonObject();

                        ConnectionInfo info = new ConnectionInfo();
                        info.setUsername(jConn.get("user").getAsString());
                        info.setIpaddress(jConn.get("peer_host").getAsString());
                        info.setConnectedat(new Date(jConn.get("connected_at").getAsLong()));
                        info.setMqName(jConn.get("name").getAsString());

                        connections.add(info);
                    }
                }

                if(connections.size() > 0){
                    String serialized = _gson.toJson(connections);

                    publishMessage(
                        CommandLineOptions.getInstance().mqExchangeNameStats,
                        "connections",
                        "connections",
                        serialized
                    );
                }
            }

        }catch (Exception ex){
            _log.log(Level.SEVERE,ex.getMessage(), ex);
        }

    }
}
