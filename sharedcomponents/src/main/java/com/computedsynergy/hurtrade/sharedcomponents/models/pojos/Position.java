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
package com.computedsynergy.hurtrade.sharedcomponents.models.pojos;

import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 * 
 * Represents a trade
 */
public class Position {
    
    public static final String ORDER_TYPE_BUY = "buy";
    public static final String ORDER_TYPE_SELL = "sell";
    
    public static final String ORDER_STATE_PENDING_OPEN = "pending_dealer_open";
    public static final String ORDER_STATE_OPEN = "open";
    public static final String ORDER_STATE_PENDING_CLOSE = "pending_dealer_close";
    public static final String ORDER_STATE_CLOSED = "closed";
    public static final String ORDER_STATE_REJECTED_OPEN = "rejected_open";

    //is this buy or sell?
    private String orderType;
    //what commodity are we trading?
    private String commodity;
    //how much are we trading, can change in cases of hedge so not final
    private BigDecimal amount;
    //p/l of this position
    private BigDecimal currentPl;
    //a unique identifier for this order
    private final UUID orderId;
    //the price at which the commodity was requested
    private BigDecimal openPrice;
    //what state the order currently is in
    private String orderState;
    //what price was the order closed at
    private BigDecimal closePrice;
    //date and time fields
    private Date createdat;
    private Date endedat;
    private Date closedat;
    private Date approvedopenat;
    private Date approvedcloseat;

    private BigDecimal usedMargin;
    private BigDecimal ratio;
    
    public Position(UUID orderId, String orderType, String commodity, BigDecimal amount, BigDecimal requestedPrice, BigDecimal ratio){
        
        this.orderId = orderId;
        this.orderType = orderType;
        this.commodity = commodity;
        this.amount = amount;
        this.currentPl = BigDecimal.ZERO;
        this.setOpenPrice(requestedPrice);
        this.setOrderState(ORDER_STATE_PENDING_OPEN);
        this.ratio = ratio; //ratio of leverage allowed on this instrument
    }
    
    public void processQuote(QuoteList clientQuotes) {

        if (
                orderState.equalsIgnoreCase(ORDER_STATE_PENDING_CLOSE) ||
                        orderState.equalsIgnoreCase(ORDER_STATE_CLOSED) ||
                        orderState.equalsIgnoreCase(ORDER_STATE_REJECTED_OPEN)
                )
        {
            return;
        }
        
        if(clientQuotes.containsKey(commodity)){
            BigDecimal closingPrice = BigDecimal.ZERO;
            
            BigDecimal exchangeRate = BigDecimal.ONE;
            String baseCurrency = commodity.substring(0,3);
            
            
            if(orderType.equals(ORDER_TYPE_BUY)){
                closingPrice = clientQuotes.get(commodity).bid;
                if(!baseCurrency.equals("USD")){
                   exchangeRate = clientQuotes.get(baseCurrency + "USD").bid;
                    setUsedMargin(clientQuotes.get(baseCurrency + "USD").bid.multiply(clientQuotes.get(commodity).lotSize).multiply(amount));
                }else{
                    setUsedMargin(closingPrice.multiply(clientQuotes.get(commodity).lotSize).multiply(amount));
                }
            }else{
                closingPrice = clientQuotes.get(commodity).ask;
                if(!baseCurrency.equals("USD")){
                   exchangeRate = clientQuotes.get(baseCurrency + "USD").ask;
                    setUsedMargin(clientQuotes.get(baseCurrency + "USD").ask.multiply(clientQuotes.get(commodity).lotSize).multiply(amount));
                }else{
                    setUsedMargin(closingPrice.multiply(clientQuotes.get(commodity).lotSize).multiply(amount));
                }
            }
            currentPl = closingPrice.subtract(getOpenPrice()).multiply(exchangeRate).multiply(amount).multiply(clientQuotes.get(commodity).lotSize);

        }
    }

    /**
     * @return the orderType
     */
    public String getOrderType() {
        return orderType;
    }

    /**
     * @param orderType the orderType to set
     */
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    /**
     * @return the commodity
     */
    public String getCommodity() {
        return commodity;
    }

    /**
     * @param commodity the commodity to set
     */
    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    /**
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * @param amount the amount to set
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * @return the currentPl
     */
    public BigDecimal getCurrentPl() {
        return currentPl;
    }

    /**
     * @param currentPl the currentPl to set
     */
    public void setCurrentPl(BigDecimal currentPl) {
        this.currentPl = currentPl;
    }

    /**
     * @return the orderId
     */
    public UUID getOrderId() {
        return orderId;
    }

  

    /**
     * @return the openPrice
     */
    public BigDecimal getRequestedPrice() {
        return getOpenPrice();
    }

    /**
     * @param requestedPrice the openPrice to set
     */
    public void setRequestedPrice(BigDecimal requestedPrice) {
        this.setOpenPrice(requestedPrice);
    }

    /**
     * @return the orderState
     */
    public String getOrderState() {
        return orderState;
    }

    /**
     * @param orderState the orderState to set
     */
    public void setOrderState(String orderState) {
        this.orderState = orderState;

        switch (this.orderState){
            case ORDER_STATE_PENDING_OPEN:
            {
                setCreatedat(new Date());
            }
            break;

            case ORDER_STATE_OPEN:
            {
                setApprovedopenat(new Date());
            }
            break;

            case ORDER_STATE_PENDING_CLOSE:
            {
                setClosedat(new Date());
            }
            break;

            case ORDER_STATE_CLOSED:
            {
                setApprovedcloseat(new Date());
            }
            break;

            case ORDER_STATE_REJECTED_OPEN:
            {

            }
            break;
        }
    }


    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public Date getCreatedat() {
        return createdat;
    }

    public void setCreatedat(Date createdat) {
        this.createdat = createdat;
    }

    public Date getEndedat() {
        return endedat;
    }

    public void setEndedat(Date endedat) {
        this.endedat = endedat;
    }

    public Date getClosedat() {
        return closedat;
    }

    public void setClosedat(Date closedat) {
        this.closedat = closedat;
    }

    public Date getApprovedopenat() {
        return approvedopenat;
    }

    public void setApprovedopenat(Date approvedopenat) {
        this.approvedopenat = approvedopenat;
    }

    public Date getApprovedcloseat() {
        return approvedcloseat;
    }

    public void setApprovedcloseat(Date approvedcloseat) {
        this.approvedcloseat = approvedcloseat;
    }

    public BigDecimal getUsedMargin() {
        return usedMargin;
    }

    public void setUsedMargin(BigDecimal usedMargin) {
        this.usedMargin = usedMargin;
    }
}
