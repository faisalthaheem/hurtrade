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
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.charting.CandleStick;
import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.IQuoteModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.converters.BigDecimalConverter;

import java.util.Collection;
import java.util.Date;
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

    @Override
    public CandleStick GetCandleStickForPeriod(String commodity, int user_id, Date start, Date end) {

        CandleStick cstick = new CandleStick();

        try{

            //find the max price in the period
            String queryHighest = String.format(
                    "select max(bid) maxprice from quotes where commodityname='%1$s' and user_id=%2$d and created > '%3$s' and created <= '%4$s'",
                    commodity, user_id, start.toString(), end.toString()
            );
            //find the min price in the period
            String queryLowest = String.format(
                    "select min(bid) minprice from quotes where commodityname='%1$s' and user_id=%2$d and created > '%3$s' and created <= '%4$s'",
                    commodity, user_id, start.toString(), end.toString()
            );
            //find the nearest to 'start' open price
            String queryOpen = String.format(
                    "select bid from quotes where commodityname='%1$s' and user_id=%2$d and created > '%3$s' and created <= '%4$s' order by id desc limit 1",
                    commodity, user_id, start.toString(), end.toString()
            );
            //find the nearest to 'end' close price
            String queryClose = String.format(
                    "select bid from quotes where commodityname='%1$s' and user_id=%2$d and created > '%3$s' and created <= '%4$s' order by id asc limit 1",
                    commodity, user_id, start.toString(), end.toString()
            );

            BigDecimalConverter converter = new BigDecimalConverter();

            try (Connection con = sql2o.open()) {
                cstick.setHighest(converter.convert(con.createQuery(queryHighest).executeScalar()));
                cstick.setLowest(converter.convert(con.createQuery(queryLowest).executeScalar()));
                cstick.setOpen(converter.convert(con.createQuery(queryOpen).executeScalar()));
                cstick.setClose(converter.convert(con.createQuery(queryClose).executeScalar()));
                cstick.setSampleFor(start);
            }

        }catch (Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

        return cstick;
    }

}
