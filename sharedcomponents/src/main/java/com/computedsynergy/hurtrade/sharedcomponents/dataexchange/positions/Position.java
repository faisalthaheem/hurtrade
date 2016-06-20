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
    
    public static final String ORDER_STATE_PENDING_OPEN = "pending_dealer_open";
    public static final String ORDER_STATE_OPEN = "open";
    public static final String ORDER_STATE_PENDING_CLOSE = "pending_dealer_close";
    public static final String ORDER_STATE_CLOSED = "closed";
    
    //is this buy or sell?
    private final String orderType;
    //what commodity are we trading?
    private final String commodity;
    //how much are we trading, can change in cases of hedge so not final
    private BigDecimal amount;
    //p/l of this position
    private BigDecimal currentPl;
    //a unique identifier for this order
    private final UUID orderId;
    //the price at which the commodity was requested
    private final BigDecimal requestedPrice;
    //what state the order currently is in
    private String orderState;
    
    public Position(UUID orderId, String orderType, String commodity, BigDecimal amount, BigDecimal requestedPrice){
        
        this.orderId = orderId;
        this.orderType = orderType;
        this.commodity = commodity;
        this.amount = amount;
        this.currentPl = BigDecimal.ZERO;
        this.requestedPrice = requestedPrice;
        this.orderState = ORDER_STATE_PENDING_OPEN;
    }
    
    public void processQuote(QuoteList clientQuotes){
        
    }
    
}
