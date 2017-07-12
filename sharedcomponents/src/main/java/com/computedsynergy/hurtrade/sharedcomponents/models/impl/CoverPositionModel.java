package com.computedsynergy.hurtrade.sharedcomponents.models.impl;

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.ICoverPosition;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverPosition;
import org.sql2o.Connection;

import java.util.List;

/**
 * Created by faisal.t on 7/12/2017.
 */
public class CoverPositionModel extends ModelBase implements ICoverPosition {
    @Override
    public List<CoverPosition> listCoverPositionsForAccount(int coverAccountId) {

        List<CoverPosition> ret;

        String query = "select * from coverpositions where coveraccount_id = :coveraccount_id";

        try (Connection conn = sql2o.open()) {
            ret = conn.createQuery(query)
                    .addParameter("coveraccount_id", coverAccountId)
                    .executeAndFetch(CoverPosition.class);
        }

        return ret;
    }

    @Override
    public boolean saveUpdateCoverPosition(CoverPosition p) {

        boolean bRet = false;

        String endPreviousSql = "update coverpositions set endedat = :endedat where internalid = :internalid";

        String insertSql = "INSERT INTO public.coverpositions " +
        "(coveraccount_id, commodity, ordertype, amount, openprice, closeprice, opentime, closetime, openedby, closedby, currentpl, created, endedat, internalid, remoteid) " +
        "VALUES(:coveraccount_id, :commodity, :ordertype, :amount, :openprice, :closeprice, :opentime, :closetime, :openedby, :closedby, :currentpl, :created, :endedat, :internalid, :remoteid)";


        try (Connection con = sql2o.beginTransaction()) {

            con.createQuery(endPreviousSql)
                    .addParameter("internalid", p.getInternalid())
                    .executeUpdate();

            con.createQuery(insertSql)
                    .addParameter("coveraccount_id", p.getCoveraccount_id())
                    .addParameter("commodity", p.getCommodity())
                    .addParameter("ordertype", p.getOrderType())
                    .addParameter("amount", p.getAmount())
                    .addParameter("openprice", p.getOpenPrice())
                    .addParameter("closeprice", p.getClosePrice())
                    .addParameter("opentime", p.getOpentime())
                    .addParameter("closetime", p.getClosetime())
                    .addParameter("openedby", p.getOpenedBy())
                    .addParameter("closedby", p.getClosedBy())
                    .addParameter("currentpl", p.getCurrentPL())
                    .addParameter("created", p.getCreated())
                    .addParameter("endedat", p.getEndedat())
                    .addParameter("internalid", p.getInternalid())
                    .addParameter("remoteid", p.getRemoteid())
                    .executeUpdate();
            con.commit();

            bRet = true;
        }

        return bRet;
    }
}
