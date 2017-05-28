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
package com.computedsynergy.hurtrade.fxyahoo;

import com.beust.jcommander.JCommander;
import com.computedsynergy.hurtrade.sharedcomponents.amqp.AmqpBase;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.CommodityModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Commodity;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.Quote;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.SourceQuote;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class FxYahoo extends AmqpBase implements Runnable {
 
    private Object lockCommodities = new Object();
    Map<String, Commodity> commodities;
    
    public static void main(String[] args) throws IOException, TimeoutException{
        
        new JCommander(CommandLineOptions.getInstance(), args);
        FxYahoo yahoo = new FxYahoo();
        yahoo.refreshCommodities();
        yahoo.setupAMQP();
        yahoo.run();
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
                
        while(true){
            try{
                
                //todo exit on shutdown signal reception
                
                //build a list of quotes to query
                String symbolsToQuery = "";
                synchronized(lockCommodities){
                    for(Commodity commodity : commodities.values()){
                        symbolsToQuery += "\"" + commodity.getCommodityname() + "\",";
                    }
                }
                
                System.out.println(symbolsToQuery);
                
                symbolsToQuery = symbolsToQuery.substring(0, symbolsToQuery.length()-1);
                
                String commodityQuery = URLEncoder.encode("select * from yahoo.finance.xchange where pair in (" + symbolsToQuery + ")");
                String url = "http://query.yahooapis.com/v1/public/yql?q=" + commodityQuery + "&env=store://datatables.org/alltableswithkeys";
                
                SAXBuilder saxBuilder = new SAXBuilder();
                org.jdom2.Document doc = buildDoc(saxBuilder, url);
                
                Element docRoot = doc.getRootElement();
                List<Element> results = docRoot.getChild("results").getChildren("rate");
                
                QuoteList quotes = new QuoteList();
                for(Element result : results){
                    
                    String commodity = result.getAttributeValue("id");
                    
                    Quote quote = new Quote(
                            result.getChildText("Bid"),
                            result.getChildText("Ask"),
                            result.getChildText("Date"),
                            result.getChildText("Time"),
                            result.getChildText("Rate"),
                            result.getChildText("Name"),
                            commodities.get(commodity).getLotsize()
                    );
                    
                    quotes.put(commodity, quote);
                }
                
                Gson gson = new Gson();
                
                UserModel userModel = new UserModel();
                List<User> users = userModel.getAllUsers();
                for(User u: users){
                    
                    SourceQuote quote = new SourceQuote(quotes, u);
                    String serialized = gson.toJson(quote);
                    channel.basicPublish(Constants.EXCHANGE_NAME_RATES, "", null, serialized.getBytes());
                }
                
                Thread.sleep(CommandLineOptions.getInstance().yahooFxFrequency);
            }catch(Exception ex){
                //todo log here
                ex.printStackTrace();
            }
        }
    }
    
    private org.jdom2.Document buildDoc(SAXBuilder saxBuilder, String urlToLoad) throws MalformedURLException, JDOMException, IOException{
        
        URL url = new URL(urlToLoad);
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        return saxBuilder.build(in);
    }
    
    
    
    @Override
    public void setupAMQP() throws IOException, TimeoutException {
        
        super.setupAMQP();
        
        channel.exchangeDeclare(Constants.EXCHANGE_NAME_RATES, "direct", true);
        channel.queueDeclare(Constants.QUEUE_NAME_RATES, true, false, false, null);
        channel.queueBind(Constants.QUEUE_NAME_RATES, Constants.EXCHANGE_NAME_RATES, "");
    }
}
