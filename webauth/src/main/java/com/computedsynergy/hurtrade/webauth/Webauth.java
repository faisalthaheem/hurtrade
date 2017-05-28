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
import com.computedsynergy.hurtrade.sharedcomponents.util.ObjectUtils;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.port;
import static spark.Spark.post;
import java.nio.charset.Charset;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class Webauth {

    /**
     * https://github.com/rabbitmq/rabbitmq-auth-backend-http
     * @param args
     */

    private static Map<String, String> toMap(List<NameValuePair> pairs){
        Map<String, String> map = new HashMap<>();
        for(int i=0; i<pairs.size(); i++){
            NameValuePair pair = pairs.get(i);
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }

    public static void main(String[] args){

        port(80);

        post("/user", (req, res) -> {

            boolean userValid = false;
            String authtags = "";

            try {

                List<NameValuePair> pairs = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());

                Map<String, String> params = toMap(pairs);

                String username = params.get("username");
                String password = params.get("password");

                //todo get sha checksum for password before querying db

                User user = RedisUtil.getInstance().GetUserInfo(username);
                if(user == null){

                    UserModel userModel = new UserModel();
                    User dbUser = userModel.getByUsername(username);

                    if(dbUser != null){

                        RedisUtil.getInstance().SetUserInfo(dbUser);

                        if(dbUser.getPass().equals(password)) {
                            authtags = dbUser.getAuthtags();
                            userValid = true;
                        }
                    }

                }else{
                    if(user.getPass().equals(password)){
                        authtags = user.getAuthtags();
                        userValid=true;
                    }
                }

            } catch (Exception ex) {
                Logger.getLogger(Webauth.class.getName()).log(Level.SEVERE, null, ex);
            }

            res.status(200);

            if(userValid){
                return "allow " + authtags;
            }

            return "deny";
        });

        post("/vhost", (req, res) -> {

            res.status(200);
            return "allow";

        });

        post("/resource", (req, res) -> {

            res.status(200);
            return "allow";

        });

        post("/topic", (req, res) -> {

            res.status(200);
            return "allow";

        });

    }
}
