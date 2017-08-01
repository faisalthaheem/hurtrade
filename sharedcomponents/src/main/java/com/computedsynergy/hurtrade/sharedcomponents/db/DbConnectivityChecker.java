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
package com.computedsynergy.hurtrade.sharedcomponents.db;

import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */

public class DbConnectivityChecker {

    protected Logger _log = Logger.getLogger(this.getClass().getName());

    public boolean IsDbReady(){

        int triesLeft = 10;
        boolean ready = false;

        do{

            try{
                CommandLineOptions options = CommandLineOptions.getInstance();

                DbConnection db = DbConnection.getInstance();
                db.init("jdbc:postgresql://" + options.dbHost
                                + ":" + options.dbPort + "/" + options.database,
                        options.dbUsername, options.dbPassword);

                db.getSql2o();
                ready = true;

                _log.info("DB Connectivty Good to go.");
                break;

            }catch (Exception ex){
                String message = String.format("Unable to connect to db, will wait for 30 seconds before trying again [%d] times.", triesLeft);
                _log.log(Level.SEVERE, message, ex);
            }

            try {

                if(!ready) {
                    Thread.sleep(30 * 1000);
                }

            }catch(Exception ex){

                _log.severe(ex.getMessage());
            }


        }while(triesLeft-- > 0);


        return ready;

    }

}
