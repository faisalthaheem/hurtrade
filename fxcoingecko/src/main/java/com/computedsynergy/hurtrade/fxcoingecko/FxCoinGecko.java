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
package com.computedsynergy.hurtrade.fxcoingecko;

import com.beust.jcommander.JCommander;
import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.Quote;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.SourceQuote;
import com.computedsynergy.hurtrade.sharedcomponents.db.DbConnectivityChecker;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.CommodityModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Commodity;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.google.gson.Gson;
import org.apache.log4j.BasicConfigurator;

import java.math.BigDecimal;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.ExchangeRates.ExchangeRates;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.ExchangeRates.Rate;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;

import java.util.Map;


/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class FxCoinGecko extends AmqpBase implements Runnable {

    private static boolean _keepRunning = true;
    private Object lockCommodities = new Object();
    Map<String, Commodity> commodities;
    
    public static void main(String[] args) throws IOException, TimeoutException{

        new JCommander(CommandLineOptions.getInstance(), args);
        BasicConfigurator.configure();

        FxCoinGecko fx = new FxCoinGecko();

        if( !new DbConnectivityChecker().IsDbReady()){
            fx._log.info("Unable to connect to db. Exiting.");
            return;
        }

        //ensure we exit gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> _keepRunning = false));


        fx._log.info("Fetching commodities");
        fx.refreshCommodities();
        
        fx._log.info("Setting up AMQP.");
        fx.setupAMQP();

        fx._log.info("Begin loop");
        fx.run();
    }
    
    public void refreshCommodities(){
        
        commodities = new HashMap<String, Commodity>();

        //fetch all the commodities of type FX and store
        CommodityModel commodityModel = new CommodityModel();
        
        synchronized(lockCommodities){
            List<Commodity> commoditiesList = commodityModel.getCommodities(CommodityModel.COMMODITY_TYPE_FX);
            for(Commodity c:commoditiesList){
                commodities.put(c.getCommodityname(), c);
            }
        }
    }

    @Override
    public void run() {
                
        do{
            _log.info("Create Gecko client");
            CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
            boolean errCondition = false;

            while(!errCondition)
            {

                try{

                    Date startTime = new Date();

                    _log.info("Fetch rates");
                    ExchangeRates exchangeRates = client.getExchangeRates();
                    
                    QuoteList quotes = new QuoteList();
                    Date rateTime = new Date();

                    _log.info("Begin processing rates.");
                    for(Commodity commodity : commodities.values()){

                        String quoteSymbol = commodity.getCommodityname().substring(3,6).toLowerCase();

                        _log.info("Process rate for " + quoteSymbol);
                        Rate r = exchangeRates.getRates().get(quoteSymbol);
                        BigDecimal val = new BigDecimal(r.getValue());
                        

                        Quote quote = new Quote(
                            val,
                            val,
                            rateTime,
                            val,
                            commodity.getCommodityname(),
                            commodity.getLotsize()
                        );

                        _log.info("Commodity: " + quote.name + " B: " + quote.bid + " A: " + quote.ask);
                        quotes.put(quote.name, quote);
                    }
                    
                    Gson gson = new Gson();
                    SourceQuote quote = new SourceQuote(quotes);
                    String serialized = gson.toJson(quote);

                    channel.basicPublish(Constants.EXCHANGE_NAME_RATES, "", null, serialized.getBytes());


                    Date endTime = new Date();
                    _log.info("Publishing took [" + (endTime.getTime() - startTime.getTime()) + "] ms.");
                    
                    Thread.sleep(CommandLineOptions.getInstance().geckoQueryFrequency);

                }catch(Exception ex){

                    java.util.logging.Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    errCondition=true;
                }
            }

            _log.info("Query loop broken, this indicates a problem fetching rates.");
            try{
                Thread.sleep(1000);
            }catch(Exception ex)
            {

            }

        }while(_keepRunning);
    }
    
    @Override
    public void setupAMQP() {

        try {
            super.setupAMQP();

            channel.exchangeDeclare(Constants.EXCHANGE_NAME_RATES, "fanout", true);
            channel.queueDeclare(Constants.QUEUE_NAME_RATES, true, false, false, null);
            channel.queueBind(Constants.QUEUE_NAME_RATES, Constants.EXCHANGE_NAME_RATES, "");
        }catch (Exception ex){
            java.util.logging.Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
}
