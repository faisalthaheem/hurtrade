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
