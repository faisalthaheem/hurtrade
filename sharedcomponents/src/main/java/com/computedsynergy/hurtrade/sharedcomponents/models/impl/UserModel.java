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
import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.IUserModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sql2o.Connection;
import org.sql2o.Query;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class UserModel extends ModelBase implements IUserModel{
    
    

    @Override
    public List<User> getAllUsers() {
        
        String query = "select * from users where usertype in ('trader','dealer') and ended is null";
        
        try (Connection conn = sql2o.open()) {
            List<User> users = conn.createQuery(query)
                    .executeAndFetch(User.class);
            
            return users;
        }
    }

    @Override
    public List<User> getAllUsersForOffice(int id) {
        
        String query = "select * from users where usertype in ('trader','dealer') AND id IN (select user_id from offices_users where office_id ="+ id + ") and ended is null";
        
        try (Connection conn = sql2o.open()) {
            List<User> users = conn.createQuery(query)
                    .executeAndFetch(User.class);
            
            return users;
        }
        
    }

    @Override
    public List<User> getNonOfficeUsers() {

        String query = "select * from users where id not in (select distinct(user_id) user_id from offices_users) and ended is null";

        try (Connection conn = sql2o.open()) {
            List<User> users = conn.createQuery(query)
                    .executeAndFetch(User.class);

            return users;
        }
    }

    @Override
    public User authenticate(String username, String password) {
        
        
        String query = String.format(("select * from users where username = '%s' AND password='%s' and ended is null LIMIT 1"), username, password);
        User ret = null;
        
        try (Connection conn = sql2o.open()) {
            List<User> users = conn.createQuery(query)
                    .executeAndFetch(User.class);
            
            if(users.size() > 0){
                ret = users.get(0);
            }
        }
        
        return ret;
        
    }

    /**
     *
     * @param username
     * @return class User if found, null if not found
     */
    @Override
    public User getByUsername(String username) {
        
        
        String query = String.format("select * from users where username = '%s' and ended is null LIMIT 1", username);
        User ret;
        
        try (Connection conn = sql2o.open()) {
            ret = conn.createQuery(query)
                    .executeAndFetchFirst(User.class);
        }
        
        return ret;
        
    }

    @Override
    public void updateUser(User u) {

        String queryEndRecord = "Update users set ended = current_timestamp where id = :id";
        String queryInsert = "INSERT INTO public.users" +
        "(username, pass, locked, usertype, phonenumber, fullname, email, created, ended, useruuid, authtags, liquidate) " +
        "VALUES (:username, :pass, :locked, :usertype, :phonenumber, :fullname, :email, current_timestamp, null, :useruuid, :authtags, :liquidate)";



        try (Connection con = sql2o.beginTransaction()) {

            con.createQuery(queryEndRecord)
                    .addParameter("id",u.getId())
                    .executeUpdate();


            con.createQuery(queryInsert)
                    .addParameter("username",u.getUsername())
                    .addParameter("pass",u.getPass())
                    .addParameter("locked",u.isLocked())
                    .addParameter("usertype",u.getUsertype())
                    .addParameter("phonenumber",u.getPhonenumber())
                    .addParameter("fullname",u.getFullname())
                    .addParameter("email",u.getEmail())
                    .addParameter("useruuid",u.getUseruuid())
                    .addParameter("authtags",u.getAuthtags())
                    .addParameter("liquidate",u.isLiquidate())
                    .executeUpdate();


            con.commit();

        }catch(Exception ex){
            _log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}
