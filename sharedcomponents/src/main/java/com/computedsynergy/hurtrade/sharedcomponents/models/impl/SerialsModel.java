package com.computedsynergy.hurtrade.sharedcomponents.models.impl;

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.ISerialsModel;
import org.sql2o.Connection;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by faisal.t on 7/18/2017.
 */
public class SerialsModel extends ModelBase implements ISerialsModel {
    @Override
    public long getNextSerial(String sname) {

        long ret = 0;

        try (Connection conn = sql2o.beginTransaction()) {

            //update and then select

            String query = "update serials set svalue = svalue +1 where sname = :serialname";

            conn.createQuery(query)
                    .addParameter("serialname", sname)
                    .executeUpdate();

            query = "select svalue from serials where sname = :serialname";

            ret = (long)conn.createQuery(query)
                    .addParameter("serialname", sname)
                    .executeScalar();

            conn.commit();
            conn.close();

        }catch(Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

        return ret;
    }
}
