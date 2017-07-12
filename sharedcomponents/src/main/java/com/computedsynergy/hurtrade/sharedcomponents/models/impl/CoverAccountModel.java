package com.computedsynergy.hurtrade.sharedcomponents.models.impl;

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.ICoverAccount;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverAccount;
import org.sql2o.Connection;
import java.util.List;

/**
 * Created by faisal.t on 7/12/2017.
 */
public class CoverAccountModel extends ModelBase implements ICoverAccount{


    @Override
    public List<CoverAccount> listCoverAccountsForOffice(int officeid) {

        List<CoverAccount> ret;

        String query = "Select * from coveraccounts where office_id = :officeid";

        try (Connection conn = sql2o.open()) {
            ret = conn.createQuery(query)
                    .addParameter("officeid", officeid)
                    .executeAndFetch(CoverAccount.class);
        }

        return ret;
    }
}
