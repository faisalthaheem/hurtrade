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

import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.Quote;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.SourceQuote;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.updates.ClientUpdate;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.SavedPositionModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.HurUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.EXPIRY_LOCK_USER_POSITIONS;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.TIMEOUT_LOCK_USER_POSITIONS;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.getLockNameForUserPositions;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.getUserPositionsKeyName;
import com.github.jedis.lock.JedisLock;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 *
 * Subscribes to the rate queue and notifies the clients in case there is an
 * update after applying client specific spreads
 */
public class CommodityUpdateProcessor extends AmqpBase {
    
    SavedPositionModel savedPositionModel = new SavedPositionModel();
    Gson gson = new Gson();

    public void init() throws Exception {
        
        super.setupAMQP();
        
        channel.queueDeclare(Constants.QUEUE_NAME_RATES, true, false, false, null);

        channel.basicConsume(Constants.QUEUE_NAME_RATES, false, "CommodityPricePump",
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

                        
                        SourceQuote quote = new Gson().fromJson(new String(body), SourceQuote.class);
                        processQuote(quote);

                        channel.basicAck(deliveryTag, false);
                    }
                });
    }

    private void processQuote(SourceQuote quote) {
        
        String clientExchangeName = HurUtil.getClientExchangeName(quote.getUser().getUseruuid());
        
        //introduce the spread as defined for this client for the symbols in the quote list
        Map<String, BigDecimal> userSpread = RedisUtil
                                                .getInstance()
                                                .getUserSpreadMap(RedisUtil.getUserSpreadMapName(quote.getUser().getUseruuid()));
        
        
        QuoteList sourceQuotes = quote.getQuoteList();
        QuoteList clientQuotes = new QuoteList();
        for(String k:sourceQuotes.keySet()){
            
            Quote sourceQuote = sourceQuotes.get(k);

            Quote q = null;
            //include propagation to client only if they are allowed this symbol
            if(null != userSpread && userSpread.containsKey(k)){
                q = new Quote(
                        sourceQuote.bid,
                        sourceQuote.bid.add(userSpread.get(k)),
                        sourceQuote.quoteTime,
                        BigDecimal.ZERO,
                        sourceQuote.name,
                        sourceQuote.lotSize
                );
                
            }else{
                q = new Quote(
                        sourceQuote.bid,
                        sourceQuote.ask,
                        sourceQuote.quoteTime,
                        BigDecimal.ZERO,
                        sourceQuote.name,
                        sourceQuote.lotSize
                );
            }
            clientQuotes.put(k, q);
        }
        
        
        Map<UUID, Position> clientPositions = null;
        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(
                        jedis, 
                        RedisUtil.getLockNameForUserProcessing(quote.getUser().getUseruuid()),
                        RedisUtil.TIMEOUT_LOCK_USER_PROCESSING, 
                        RedisUtil.EXPIRY_LOCK_USER_PROCESSING
                    );

            
            try {
                if(lock.acquire()){

                    //process client's positions
                    clientPositions = updateClientPositions(quote.getUser(), clientQuotes);
                    lock.release();

                }else{
                    Logger.getLogger(CommodityUpdateProcessor.class.getName()).log(Level.SEVERE, "Could not lock user position in redis {0}",  quote.getUser().getUsername());
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(CommodityUpdateProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        

        //set the quotes as they are needed when trading
        String serializedQuotes = gson.toJson(clientQuotes);
        RedisUtil.getInstance().setSeriaizedQuotesForClient(serializedQuotes, quote.getUser().getUseruuid());
        
        //remove the quotes not allowed for this client
        for(String k:sourceQuotes.keySet()){
            if(!userSpread.containsKey(k)){
                clientQuotes.remove(k);
            }
        }
        
        ClientUpdate update = new ClientUpdate(clientPositions, clientQuotes);
        
        String serializedQuotesForClient = gson.toJson(update);
        //finally send out the quotes and updated positions to the client
        try{
            channel.basicPublish(clientExchangeName, "response", null, serializedQuotesForClient.getBytes());
        }catch(Exception ex){

            Logger.getLogger(CommodityUpdateProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    private Map<UUID, Position> updateClientPositions(User user, QuoteList clientQuotes)
    {
        
        String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());
        Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();
        
        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            
            JedisLock lock = new JedisLock(jedis, getLockNameForUserPositions(userPositionsKeyName), TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
            try{
                if(lock.acquire()){

                    //get client positions
                    Map<UUID, Position> positions = gson.fromJson(jedis.get(userPositionsKeyName),mapType);
                    if(positions == null){
                        positions = new HashMap<>();
                    }

                    for(Position p:positions.values()){
                        p.processQuote(clientQuotes);
                    }
                    
                    //set client positions
                    String serializedPositions = gson.toJson(positions);
                    jedis.set(userPositionsKeyName, serializedPositions);

                    //todo - on a different thread not so often.
                    //dump client's positions to db only if there are any positions
//                    if(positions.size() > 0){
//                        SavedPosition p = new SavedPosition(user.getId(), serializedPositions);
//                        savedPositionModel.savePosition(p);
//                    }

                    lock.release();
                    
                    return positions;

                }else{
                    Logger.getLogger(CommodityUpdateProcessor.class.getName()).log(Level.SEVERE, "Could not get lock to process user positions for {0}", user.getUsername());
                }
            }catch(Exception ex){
                Logger.getLogger(CommodityUpdateProcessor.class.getName()).log(Level.SEVERE, "Error processing user positions for {0}", user.getUsername());
                Logger.getLogger(CommodityUpdateProcessor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
            //or return null if problemms
            return null;
        }
    }
    
}
