package com.computedsynergy.hurtrade.collectors;

import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.ConnectionInfo;
import com.google.gson.*;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.impl.AMQBasicProperties;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class RabbitMqStats extends AmqpBase {

    private JsonParser _par = new JsonParser();
    private Gson _gson = new Gson();

    public static void main(String[] args) {

        RabbitMqStats mqStats = new RabbitMqStats();

        mqStats.run();
    }

    private void run(){

        try{

            setupAMQP();

            channel.exchangeDeclare(
                    CommandLineOptions.getInstance().mqExchangeNameStats,
                    "fanout",
                    true
                    );

        }catch(Exception ex){
            _log.log(Level.SEVERE,ex.getMessage(), ex);
            return;
        }

        while(true){
            queryAndPublish();

            try{
                Thread.sleep(CommandLineOptions.getInstance().mqStatsInterval);
            }catch (Exception ex){
                _log.log(Level.SEVERE, ex.getMessage(), ex);
            }
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
