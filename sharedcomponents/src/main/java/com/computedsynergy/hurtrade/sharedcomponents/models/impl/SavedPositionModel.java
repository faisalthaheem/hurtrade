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

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.ISavedPositionModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.SavedPosition;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sql2o.Connection;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class SavedPositionModel extends ModelBase implements ISavedPositionModel{

    @Override
    public SavedPosition getPositions(int userid) {
        String query = "select * from savedpositions where user_id = " + userid;
        
        try (Connection conn = sql2o.open()) {
            SavedPosition positions = conn.createQuery(query)
                    .executeAndFetchFirst(SavedPosition.class);
            
            return positions;
        }
    }

    @Override
    public void savePositions(SavedPosition p) {
        
        String insertSql = 
	"INSERT INTO savedpositions(user_id, positiondata, created) " +
            "values (:user_id, to_json(:positiondata::json), :created) " +
            "ON CONFLICT (user_id) DO UPDATE SET positiondata = EXCLUDED.positiondata, created = EXCLUDED.created";

        try (Connection con = sql2o.open()) {
            con.createQuery(insertSql)
                    .addParameter("user_id", p.getUser_id())
                    .addParameter("positiondata", p.getPositiondata())
                    .addParameter("created", p.getCreated())
                    .executeUpdate();
        }catch(Exception ex){
            Logger.getLogger(SavedPositionModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
