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

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.IOfficeModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Office;
import java.util.List;
import org.sql2o.Connection;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class OfficeModel extends ModelBase implements IOfficeModel{
    
    

    @Override
    public List<Office> getAllOffices() {
        
        String query = "select * from offices";
        
        try (Connection conn = sql2o.open()) {
            List<Office> offices = conn.createQuery(query)
                    .executeAndFetch(Office.class);
            
            return offices;
        }
    }

    @Override
    public Office getOffice(int id) {

        Office ret;

        String query = "Select * from offices where id = :officeid";

        try (Connection conn = sql2o.open()) {
            ret = conn.createQuery(query)
                    .addParameter("officeid", id)
                    .executeAndFetchFirst(Office.class);
        }

        return ret;
    }

    @Override
    public Office getOfficeForUser(int userid)
    {
        Office ret;

        String query = "Select * from offices where id = (select id from offices_users where user_id = :userid)";

        try (Connection conn = sql2o.open()) {
            ret = conn.createQuery(query)
                    .addParameter("userid", userid)
                    .executeAndFetchFirst(Office.class);
        }

        return ret;
    }
}
