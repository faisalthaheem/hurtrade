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
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.LedgerModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.PositionModel;
import com.computedsynergy.hurtrade.sharedcomponents.util.GeneralUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.*;

//todo: move this class out of poco package
/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 * 
 * Represents a trade
 */
public class Position {
    

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
    private BigDecimal requoteprice;
    private long friendlyorderid;
    private int user_id;


    private BigDecimal usedMargin;
    private BigDecimal ratio;
    private User _user;

    private boolean _wasPendingClose; //this is used with order requotes to apply the requoted price to the appropriate field

    //logging
    private static final Logger _log = Logger.getLogger(Position.class.getName());
    
    public Position(UUID orderId, String orderType, String commodity, BigDecimal amount, BigDecimal requestedPrice, BigDecimal ratio, User user){
        
        this.orderId = orderId;
        this.orderType = orderType;
        this.commodity = commodity;
        this.amount = amount;
        this.currentPl = BigDecimal.ZERO;
        this.openPrice = requestedPrice;
        this.setOrderState(ORDER_STATE_PENDING_OPEN);
        this.ratio = ratio; //ratio of leverage allowed on this instrument
        this._user = user;
        friendlyorderid = GeneralUtil.GetNextFriendlyOrderId();
        this.user_id = user.getId();

        this.usedMargin = BigDecimal.ZERO;
    }

    public Position clone(){
        Position p = new Position(orderId, orderType, commodity, amount, openPrice, ratio, _user);
        p.setFriendlyorderid(friendlyorderid);
        return p;
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
            BigDecimal closingPrice;
            
            BigDecimal exchangeRate = BigDecimal.ONE;
            String baseCurrency = commodity.substring(0,3);
            
            
            if(orderType.equals(ORDER_TYPE_BUY)){
                closingPrice = clientQuotes.get(commodity).bid;
                if(!baseCurrency.equals("USD")){
                   exchangeRate = clientQuotes.get(baseCurrency + "USD").bid;
                   usedMargin = clientQuotes.get(baseCurrency + "USD").bid.multiply(clientQuotes.get(commodity).lotSize).multiply(amount).multiply(ratio);
                }else{
                   usedMargin = clientQuotes.get(commodity).lotSize.multiply(amount).multiply(ratio);
                }
                currentPl = closingPrice.subtract(openPrice).multiply(exchangeRate).multiply(amount).multiply(clientQuotes.get(commodity).lotSize);
            }else{
                closingPrice = clientQuotes.get(commodity).ask;
                if(!baseCurrency.equals("USD")){
                   exchangeRate = clientQuotes.get(baseCurrency + "USD").ask;
                   usedMargin = clientQuotes.get(baseCurrency + "USD").ask.multiply(clientQuotes.get(commodity).lotSize).multiply(amount).multiply(ratio);
                }
                else{
                   usedMargin = clientQuotes.get(commodity).lotSize.multiply(amount).multiply(ratio);
                }
                currentPl = openPrice.subtract(closingPrice).multiply(exchangeRate).multiply(amount).multiply(clientQuotes.get(commodity).lotSize);
            }


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
                _wasPendingClose = false;
                setCreatedat(new Date());
            }
            break;

            case ORDER_STATE_OPEN:
            {
                _wasPendingClose = false;
                setApprovedopenat(new Date());
            }
            break;

            case ORDER_STATE_PENDING_CLOSE:
            {
                _wasPendingClose = true;
                setClosedat(new Date());
            }
            break;

            case ORDER_STATE_CLOSED:
            {
                setApprovedcloseat(new Date());

                Map<String, CommodityUser> userCommodities = RedisUtil
                        .getInstance()
                        .getCachedUserCommodities(_user.getUseruuid());

                //charge commmission and fee and post p/l
                LedgerModel ledger = new LedgerModel();

                //realize position P/L
                ledger.SaveRealizedPositionPL(_user.getId(), getOrderId(), getCurrentPl());

                ledger.saveFeeForPosition(
                        _user.getId(),
                        orderId,
                        userCommodities.get(commodity).getFee()
                );

                //charge commmission only if profitable to the client
                if(currentPl.compareTo(BigDecimal.ZERO) > 0) {
                    ledger.saveCommissionForPosition(
                            _user.getId(),
                            orderId,
                            currentPl.multiply(userCommodities.get(commodity).getCommission())
                    );
                }
            }
            break;

            case ORDER_STATE_REJECTED_OPEN:
            {

            }
            break;

            case ORDER_STATE_CANCELED:
            {

            }
            break;
        }

        PositionModel positionModel = new PositionModel();
        positionModel.saveUpdatePosition(this);

        String logMessage = String.format("[%d] %s [%s]", friendlyorderid, orderId.toString(), orderState);
        _log.log(Level.INFO, logMessage);

    }

    public boolean isOpen(){
        return this.orderState.equalsIgnoreCase(ORDER_STATE_OPEN) ||
                this.orderState.equalsIgnoreCase(ORDER_STATE_PENDING_CLOSE);
    }

    public boolean isPendingOpen(){
        return
                this.orderState.equalsIgnoreCase(ORDER_STATE_PENDING_OPEN);
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

    public long getFriendlyorderid() {
        return friendlyorderid;
    }

    public void setFriendlyorderid(long friendlyorderid) {
        this.friendlyorderid = friendlyorderid;
    }

    public boolean isRequoted() {

        return orderState.equalsIgnoreCase(ORDER_STATE_REQUOTED);
    }

    public BigDecimal getRequoteprice() {
        return requoteprice;
    }

    public void setRequoteprice(BigDecimal requoteprice) {
        this.requoteprice = requoteprice;
    }

    public boolean is_wasPendingClose() {
        return _wasPendingClose;
    }

    public void set_wasPendingClose(boolean _wasPendingClose) {
        this._wasPendingClose = _wasPendingClose;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}
