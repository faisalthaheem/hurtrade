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
package com.computedsynergy.hurtrade.sharedcomponents.dataexchange;

import com.google.gson.Gson;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class TradeResponse {
    
    private final TradeRequest request;
    private final String response;
    
    public TradeResponse(TradeRequest request, String response){
        this.request = request;
        this.response = response;
    }
    
    public String toJson(){
        
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    
    
    public static TradeResponse fromJson(String json){
        
        Gson gson = new Gson();
        return gson.fromJson(json, TradeResponse.class);
    }
}
