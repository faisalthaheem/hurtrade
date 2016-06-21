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

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class TradeResponse {

    private static String RES_COMMODITY_NOT_ALLOWED = "The requested commodity is not allowed";
    private static String RES_INVALID_USER = "Authentication failure";
    private static String RES_SUCCESS = "ok";

    private TradeRequest request;
    private String response;

    public TradeResponse(TradeRequest request) {
        this.request = request;
    }

    public String toJson() {

        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static TradeResponse fromJson(String json) {

        Gson gson = new Gson();
        return gson.fromJson(json, TradeResponse.class);
    }

    public void setResponseCommodityNotAllowed() {
        this.response =  RES_COMMODITY_NOT_ALLOWED;
    }
    
    public void setResponseInvalidUser(){
        this.response = RES_INVALID_USER;
    }
    
    public void setResposneOk(){
        this.response = RES_SUCCESS;
    }

    /**
     * @return the request
     */
    public TradeRequest getRequest() {
        return request;
    }

    /**
     * @return the response
     */
    public String getResponse() {
        return response;
    }

    /**
     * @param response the response to set
     */
    public void setResponse(String response) {
        this.response = response;
    }

}
