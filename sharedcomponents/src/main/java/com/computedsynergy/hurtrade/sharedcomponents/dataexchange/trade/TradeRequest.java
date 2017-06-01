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
package com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class TradeRequest {
    
    public static final String REQUEST_TYPE_BUY = "buy";
    public static final String REQUEST_TYPE_SELL = "sell";
    
    private final String requestType;
    private final String commodity;
    private final BigDecimal requestedPrice;
    private final BigDecimal requestedLot;
    private final Date requestTime;
    private final UUID tradeId;
    
    public TradeRequest(String requestType, String commodity, BigDecimal requestedPrice, BigDecimal requestedLot, Date requestedTime,UUID tradeId)
    {
        this.requestType = requestType;
        this.commodity = commodity;
        this.requestedPrice = requestedPrice;
        this.requestedLot = requestedLot;
        this.requestTime = requestedTime;
        this.tradeId = tradeId;
    }

    /**
     * @return the commodity
     */
    public String getCommodity() {
        return commodity;
    }

    /**
     * @return the requestedPrice
     */
    public BigDecimal getRequestedPrice() {
        return requestedPrice;
    }

    /**
     * @return the requestedLot
     */
    public BigDecimal getRequestedLot() {
        return requestedLot;
    }

    /**
     * @return the requestTime
     */
    public Date getRequestTime() {
        return requestTime;
    }

    /**
     * @return the tradeId
     */
    public UUID getTradeId() {
        return tradeId;
    }

    /**
     * @return the requestType
     */
    public String getRequestType() {
        return requestType;
    }
    
    public String toJson(){
        
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    
    
    public static TradeRequest fromJson(String json){
        
        Gson gson = new GsonBuilder().setDateFormat("MM/dd/yyyy HH:mm").create();;
        return gson.fromJson(json, TradeRequest.class);
    }
}
