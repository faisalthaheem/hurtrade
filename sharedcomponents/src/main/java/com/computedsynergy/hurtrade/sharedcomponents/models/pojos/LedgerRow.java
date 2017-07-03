package com.computedsynergy.hurtrade.sharedcomponents.models.pojos;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by faisal.t on 7/3/2017.
 */
public class LedgerRow {
    private int id;
    private int user_id;
    private BigDecimal deposit;
    private BigDecimal widthdrawal;
    private BigDecimal credit;
    private BigDecimal debit;
    private Date created;
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public void setDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    public BigDecimal getWidthdrawal() {
        return widthdrawal;
    }

    public void setWidthdrawal(BigDecimal widthdrawal) {
        this.widthdrawal = widthdrawal;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
