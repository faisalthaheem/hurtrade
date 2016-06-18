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
package com.computedsynergy.hurtrade.sharedcomponents.dataexchange.positions;

import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import java.math.BigDecimal;
import java.util.UUID;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 * 
 * Represents a trade
 */
public class Position {
    
    public static final String ORDER_TYPE_BUY = "b";
    public static final String ORDER_TYPE_SELL = "s";
    
    //is this buy or sell?
    private final String orderType;
    //what commodity are we trading?
    private final String commodity;
    //how much are we trading, can change in cases of hedge so not final
    private BigDecimal amount;
    //p/l of this position
    private BigDecimal currentPl;
    
    //a unique identifier for this order
    private UUID orderId = UUID.randomUUID();
    
    public Position(String orderType, String commodity, BigDecimal amount){
        this.orderType = orderType;
        this.commodity = commodity;
        this.amount = amount;
        this.currentPl = BigDecimal.ZERO;
    }
    
    public void processQuote(QuoteList clientQuotes){
        
    }
    
}
