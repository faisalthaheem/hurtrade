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
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CommodityUser;
import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.ICommodityUser;
import java.util.List;
import org.sql2o.Connection;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class CommodityUserModel extends ModelBase implements ICommodityUser{
    
    /**
     *
     * @param user_id
     * @return
     */
    @Override
    public List<CommodityUser> getCommoditiesForUser(int user_id){

    String query = "select c.commodityname commodityname, cu.spread spread from commodities c, commodities_users cu where c.id = cu.commodity_id and cu.user_id = " + user_id;
        
        try (Connection conn = sql2o.open()) {
            List<CommodityUser> commodities = conn.createQuery(query)
                    .executeAndFetch(CommodityUser.class);
            
            return commodities;
        }
    }

}
