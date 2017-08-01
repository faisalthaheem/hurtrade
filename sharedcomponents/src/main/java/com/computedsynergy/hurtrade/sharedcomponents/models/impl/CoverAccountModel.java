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

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.ICoverAccountModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverAccount;
import org.sql2o.Connection;
import java.util.List;

/**
 * Created by faisal.t on 7/12/2017.
 */
public class CoverAccountModel extends ModelBase implements ICoverAccountModel {


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
