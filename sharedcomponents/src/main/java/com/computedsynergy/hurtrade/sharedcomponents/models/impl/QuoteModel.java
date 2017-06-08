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

import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.Quote;
import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.IQuoteModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import org.sql2o.Connection;
import org.sql2o.Query;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class QuoteModel extends ModelBase implements IQuoteModel{

    @Override
    public void saveQuote(int userId, Collection<Quote> quotes)
    {
        String insertSql =
                "INSERT INTO quotes " +
                        "(user_id, commodityname, bid, ask, created) " +
                        "VALUES(:user_id, :commodityname, :bid, :ask, current_timestamp) ";

        try (Connection con = sql2o.beginTransaction()) {

            Query query = con.createQuery(insertSql);

            for(Quote q : quotes) {
                query
                    .addParameter("user_id", userId)
                    .addParameter("commodityname", q.name)
                    .addParameter("bid", q.bid)
                    .addParameter("ask", q.ask)
                    .addToBatch();
            }

            query.executeBatch();

            con.commit();

        }catch(Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void saveQuote(int userId, Quote q) {

        String insertSql =
                "INSERT INTO quotes " +
                        "(user_id, commodityname, bid, ask, created) " +
                        "VALUES(:user_id, :commodityname, :bid, :ask, current_timestamp) ";

        try (Connection con = sql2o.open()) {

            con.createQuery(insertSql)
                    .addParameter("user_id", userId)
                    .addParameter("commodityname", q.name)
                    .addParameter("bid", q.bid)
                    .addParameter("ask", q.ask)
                    .executeUpdate();

        }catch(Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
