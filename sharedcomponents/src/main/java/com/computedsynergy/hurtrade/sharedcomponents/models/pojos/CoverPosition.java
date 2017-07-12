package com.computedsynergy.hurtrade.sharedcomponents.models.pojos;

import java.util.Date;
import java.math.BigDecimal;
import java.util.UUID;

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
