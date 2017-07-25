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
package com.computedsynergy.hurtrade.sharedcomponents.models.impl;

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.IPositionModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import org.sql2o.Connection;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class PositionModel extends ModelBase implements IPositionModel {

    @Override
    public void saveUpdatePosition(Position p) {

        String endPreviousSql =
                "UPDATE positions SET endedat = current_timestamp where orderid = :orderid and endedat IS NULL";

        String insertSql =
                "INSERT INTO positions " +
                "(ordertype, commodity, amount, currentpl, orderid, openprice, closeprice, orderstate, createdat, endedat, closedat, approvedopenat, approvedcloseat, friendlyorderid, requoteprice, user_id) " +
                "VALUES(:ordertype, :commodity, :amount, :currentpl, :orderid, :openprice, :closeprice, :orderstate, :createdat, :endedat, :closedat, :approvedopenat, :approvedcloseat, :friendlyorderid, :requoteprice, :user_id) ";

        try (Connection con = sql2o.beginTransaction()) {

            con.createQuery(endPreviousSql)
                    .addParameter("orderid", p.getOrderId())
                    .executeUpdate();

            con.createQuery(insertSql)
                    .addParameter("ordertype", p.getOrderType())
                    .addParameter("commodity", p.getCommodity())
                    .addParameter("amount", p.getAmount())
                    .addParameter("currentpl", p.getCurrentPl())
                    .addParameter("orderid", p.getOrderId())
                    .addParameter("openprice", p.getOpenPrice())
                    .addParameter("closeprice", p.getClosePrice())
                    .addParameter("orderstate", p.getOrderState())
                    .addParameter("createdat", p.getCreatedat())
                    .addParameter("endedat", p.getEndedat())
                    .addParameter("closedat", p.getClosedat())
                    .addParameter("approvedopenat", p.getApprovedopenat())
                    .addParameter("approvedcloseat", p.getApprovedcloseat())
                    .addParameter("friendlyorderid", p.getFriendlyorderid())
                    .addParameter("requoteprice", p.getRequoteprice())
                    .addParameter("user_id", p.getUser_id())
                    .executeUpdate();

            con.commit();
            con.close();
        }catch(Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
