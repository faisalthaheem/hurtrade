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
package com.computedsynergy.hurtrade.webauth;

import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.transformers.JsonTransformer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static spark.Spark.post;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class Webauth {
    
    public static void main(String[] args){
        
        post("/authenticate/", (req, res) -> {
            
            Map<String,String> response = new HashMap<String,String>();
            
            try {
                
                String username = req.params("username");
                String password = req.params("password");
                
                //todo get sha checksum for password before querying db
                
                UserModel userModel = new UserModel();
                User user = userModel.authenticate(username, password);
                if(user != null){
                    response.put("identifier", user.getUseruuid().toString());
                }else{
                    response.put("error", "invalid username/password");
                }
            
            } catch (Exception ex) {
                Logger.getLogger(Webauth.class.getName()).log(Level.SEVERE, null, ex);
            }

            return response;
        }, new JsonTransformer());
    }
    
}
