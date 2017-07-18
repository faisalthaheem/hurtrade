package com.computedsynergy.hurtrade.sharedcomponents.models.impl;

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.ILedgerModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.LedgerRow;
import org.sql2o.Connection;
import org.sql2o.converters.BigDecimalConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by faisal.t on 7/3/2017.
 */
public class LedgerModel extends ModelBase implements ILedgerModel {
    @Override
    public BigDecimal GetAvailableCashForUser(int user_id) {

        String query = "select coalesce(((sum(deposit)-sum(withdrawal)) + (sum(credit) - sum(debit))),0) as cash from ledgers where user_id = " + user_id;

        try (Connection conn = sql2o.open()) {

            return new BigDecimalConverter().convert(conn.createQuery(query).executeScalar());
        }
    }

    @Override
    public Boolean SaveRealizedPositionPL(int user_id, UUID orderId, BigDecimal realizedPL) {

        Boolean bRet = false;

        try {
            String description = String.format("P/L for order [%s].", orderId.toString());
            String query = String.format("INSERT into ledgers(user_id, deposit, created, description) values(%1$d, %2$.2f, '%3$s', '%4$s')", user_id, realizedPL, new Date(), description);

            Logger.getLogger(this.getClass().getName()).log(Level.INFO, query);

            try (Connection conn = sql2o.open()) {

                conn.createQuery(query).executeUpdate();
            }

        }catch(Exception ex){
            _log.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return bRet;
    }

    @Override
    public List<LedgerRow> GetLedgerForUser(int user_id) {

        List<LedgerRow> ret =new ArrayList<>();

        return ret;
    }

    @Override
    public boolean saveFeeForPosition(int user_id, UUID orderId, BigDecimal amt) {

        boolean bRet = false;

        try {
            String description = String.format("Fee for order [%s].", orderId.toString());
            String query = String.format("INSERT into ledgers(user_id, withdrawal, created, description) values(%1$d, %2$.2f, '%3$s', '%4$s')", user_id, amt, new Date(), description);

            Logger.getLogger(this.getClass().getName()).log(Level.INFO, query);

            try (Connection conn = sql2o.open()) {

                conn.createQuery(query).executeUpdate();
            }

        }catch(Exception ex){
            _log.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return bRet;

    }

    @Override
    public boolean saveCommissionForPosition(int user_id, UUID orderId, BigDecimal amt) {

        boolean bRet = false;

        try {
            String description = String.format("Commission for order [%s].", orderId.toString());
            String query = String.format("INSERT into ledgers(user_id, withdrawal, created, description) values(%1$d, %2$.2f, '%3$s', '%4$s')", user_id, amt, new Date(), description);

            Logger.getLogger(this.getClass().getName()).log(Level.INFO, query);

            try (Connection conn = sql2o.open()) {

                conn.createQuery(query).executeUpdate();
            }

        }catch(Exception ex){
            _log.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return bRet;

    }
}
