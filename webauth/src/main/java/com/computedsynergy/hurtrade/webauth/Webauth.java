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

import com.beust.jcommander.JCommander;
import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.db.DbConnectivityChecker;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.OfficeModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.UserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Office;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.computedsynergy.hurtrade.sharedcomponents.util.Constants;
import com.computedsynergy.hurtrade.sharedcomponents.util.MqNamingUtil;
import com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.port;
import static spark.Spark.post;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 *
 *     https://rawcdn.githack.com/rabbitmq/rabbitmq-management/rabbitmq_v3_6_10/priv/www/api/index.html
 */
public class Webauth {

    /**
     * https://github.com/rabbitmq/rabbitmq-auth-backend-http
     * @param
     */

    private static final Logger _log = Logger.getLogger(Webauth.class.getName());

    private static Map<String, String> toMap(List<NameValuePair> pairs){
        Map<String, String> map = new HashMap<>();
        for(int i=0; i<pairs.size(); i++){
            NameValuePair pair = pairs.get(i);
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }

    private static void bootstrap(){

        try{

            OfficeModel officeModel = new OfficeModel();
            UserModel userModel = new UserModel();

            List<Office> offices = officeModel.getAllOffices();

            for(Office office : offices){

                List<User> usersList = userModel.getAllUsersForOffice(office.getId());

                for(User user : usersList){
                    user.setUserOffice(office);
                    cacheUser(user);
                }
            }

            //save all others such as services and administrative
            List<User> usersList = userModel.getNonOfficeUsers();
            for(User user : usersList){
                cacheUser(user);
            }

        }catch(Exception ex){
            _log.log(Level.SEVERE, ex.getMessage(), ex);
        }

    }

    private static void cacheUser(User user){

        String authtags = user.getAuthtags();
        user.getTagsList().addAll(
                Arrays.asList(authtags.split(" "))
        );

        //set resources
        List<String> resources = user.getResourcesList();
        resources.add(Constants.EXCHANGE_NAME_AUTH);
        resources.add(MqNamingUtil.MQ_GEN_Q_NAME_PREFIX);

        resources.add(
                MqNamingUtil.getClientExchangeName(user.getUseruuid())
        );

        resources.add(
                MqNamingUtil.getClientOutgoingQueueName(user.getUseruuid())
        );

        resources.add(
                MqNamingUtil.getClientIncomingQueueName(user.getUseruuid())
        );

        if(user.getUsertype().equalsIgnoreCase(Constants.USERTYPE_DEALER)){

            resources.add(
                    MqNamingUtil.getOfficeExchangeName(user.getUserOffice().getOfficeuuid())
            );

            resources.add(
                    MqNamingUtil.getOfficeDealerINQueueName(user.getUserOffice().getOfficeuuid())
            );

            resources.add(
                    MqNamingUtil.getOfficeDealerOutQueueName(user.getUserOffice().getOfficeuuid())
            );
        }

        RedisUtil.getInstance().SetUserInfo(user);

    }

    public static void main(String[] args){

        new JCommander(CommandLineOptions.getInstance(), args);

        if( !new DbConnectivityChecker().IsDbReady()){
            _log.info("Unable to connect to db. Exiting.");
            return;
        }


        bootstrap();

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

                        OfficeModel officeModel = new OfficeModel();
                        dbUser.setUserOffice(officeModel.getOfficeForUser(dbUser.getId()));

                        cacheUser(dbUser);

                        if(!user.isLocked() && dbUser.getPass().equals(password)) {
                            userValid = true;
                        }
                    }

                }else{
                    if(!user.isLocked() && user.getPass().equals(password)){
                        authtags = user.getAuthtags();
                        userValid=true;
                    }
                }

                if(false == userValid){
                    _log.log(Level.INFO, "Authentication failed for " + username );
                }

            } catch (Exception ex) {
                _log.log(Level.SEVERE, null, ex);
            }

            res.status(200);

            if(userValid){
                return "allow " + authtags;
            }

            return "deny";
        });

        post("/vhost", (req, res) -> {

            String returnString = "allow";
            try {
                List<NameValuePair> pairs = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
                Map<String, String> params = toMap(pairs);

                if(params.containsKey("vhost")) {
                    String vhost = params.get("vhost");
                    if(!vhost.equalsIgnoreCase("/")){
                        returnString = "deny";
                    }
                }

            }catch(Exception ex){

                _log.log(Level.SEVERE, ex.getMessage(), ex);
            }

            res.status(200);
            return returnString;

        });

        post("/resource", (req, res) -> {

            String ret = "deny";

            try {
                List<NameValuePair> pairs = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
                Map<String, String> params = toMap(pairs);

                String username = params.get("username");
                String vhost = params.get("vhost");
                String resource = params.get("resource");
                String permission = params.get("permission");
                String resourceName = params.get("name");

                User user = RedisUtil.getInstance().GetUserInfo(username);
                if(null != user && !user.isLocked()){

                    if(
                            user.getUsertype().equalsIgnoreCase(Constants.USERTYPE_TRADER) ||
                                    user.getUsertype().equalsIgnoreCase(Constants.USERTYPE_DEALER)
                        ){

                        //traders and dealers are not allowed to declare/delete auth exchange
                        if(resource.equalsIgnoreCase("exchange")
                                &&
                                resourceName.equalsIgnoreCase(Constants.EXCHANGE_NAME_AUTH)
                                &&
                                permission.equalsIgnoreCase("configure")
                            ){
                            ret = "deny"; //
                        }else{
                            for(String allowedResource : user.getResourcesList()){
                                if(resourceName.startsWith(allowedResource)){
                                    ret = "allow";
                                }
                            }
                        }

                    }else if(
                            user.getUsertype().equalsIgnoreCase(Constants.USERTYPE_ADMIN) ||
                                    user.getUsertype().equalsIgnoreCase(Constants.USERTYPE_SERVICE)
                            ){
                            ret = "allow";
                    }

                    String logLine = String.format("resource u:[%s] v:[%s] r:[%s] p:[%s] :n[%s] -> %s.",
                            username,
                            vhost,
                            resource,
                            permission,
                            resourceName,
                            ret
                            );
                    _log.info(logLine);
                }

            }catch(Exception ex){

                _log.log(Level.SEVERE, ex.getMessage(), ex);
            }


            res.status(200);
            return ret;

        });

        post("/topic", (req, res) -> {

            res.status(200);
            return "allow";

        });

    }
}
