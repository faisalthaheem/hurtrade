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

import java.util.Date;
import java.math.BigDecimal;
import java.util.UUID;

import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.*;

/**
 * Created by faisal.t on 7/12/2017.
 */
public class CoverPosition {

    private int id;
    private int coveraccount_id;
    private String commodity;
    private String orderType;
    private String openedBy;
    private String closedBy;
    private BigDecimal currentPL;
    private BigDecimal amount;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private Date opentime;
    private Date closetime;
    private Date created;
    private Date endedat;
    private UUID internalid;
    private String remoteid;

    public void processQuote(QuoteList clientQuotes) {


        if(clientQuotes.containsKey(commodity)){
            BigDecimal closingPrice = BigDecimal.ZERO;

            BigDecimal exchangeRate = BigDecimal.ONE;
            String baseCurrency = commodity.substring(0,3);


            if(orderType.equals(ORDER_TYPE_BUY)){
                closingPrice = clientQuotes.get(commodity).bid;
                if(!baseCurrency.equals("USD")) {
                    exchangeRate = clientQuotes.get(baseCurrency + "USD").bid;
                }
            }else{
                closingPrice = clientQuotes.get(commodity).ask;
                if(!baseCurrency.equals("USD")) {
                    exchangeRate = clientQuotes.get(baseCurrency + "USD").ask;
                }
            }
            currentPL = closingPrice.subtract(getOpenPrice()).multiply(exchangeRate).multiply(amount).multiply(clientQuotes.get(commodity).lotSize);

        }
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCoveraccount_id() {
        return coveraccount_id;
    }

    public void setCoveraccount_id(int coveraccount_id) {
        this.coveraccount_id = coveraccount_id;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(String openedBy) {
        this.openedBy = openedBy;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public BigDecimal getCurrentPL() {
        return currentPL;
    }

    public void setCurrentPL(BigDecimal currentPL) {
        this.currentPL = currentPL;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public Date getOpentime() {
        return opentime;
    }

    public void setOpentime(Date opentime) {
        this.opentime = opentime;
    }

    public Date getClosetime() {
        return closetime;
    }

    public void setClosetime(Date closetime) {
        this.closetime = closetime;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getEndedat() {
        return endedat;
    }

    public void setEndedat(Date endedat) {
        this.endedat = endedat;
    }

    public UUID getInternalid() {
        return internalid;
    }

    public void setInternalid(UUID internalid) {
        this.internalid = internalid;
    }

    public String getRemoteid() {
        return remoteid;
    }

    public void setRemoteid(String remoteid) {
        this.remoteid = remoteid;
    }
}
