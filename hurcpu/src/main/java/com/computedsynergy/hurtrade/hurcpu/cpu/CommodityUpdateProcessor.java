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
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.HurUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 *
 * Subscribes to the rate queue and notifies the clients in case there is an
 * update after applying client specific spreads
 */
public class CommodityUpdateProcessor extends AmqpBase {

    //todo introduce cache layer to reduce hits on db and reduce processing time
    public void init() throws Exception {
        channel.queueDeclare(Constants.RATES_QUEUE_NAME, true, false, false, null);

        channel.basicConsume(Constants.RATES_QUEUE_NAME, false, "CommodityPricePump",
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
        QuoteList userQuotes = new QuoteList();
        for(String k:sourceQuotes.keySet()){
            
            Quote sourceQuote = sourceQuotes.get(k);

            Quote q = new Quote(
                    sourceQuote.bid,
                    sourceQuote.bid.add(userSpread.get(k)),
                    sourceQuote.quoteTime,
                    BigDecimal.ZERO,
                    sourceQuote.name
            );
            
            userQuotes.put(k, q);
        }
        
        String responseBody = new Gson().toJson(userQuotes);

        try{
            channel.basicPublish(clientExchangeName, "response", null, responseBody.getBytes());
        }catch(Exception ex){

        }

    }
}
