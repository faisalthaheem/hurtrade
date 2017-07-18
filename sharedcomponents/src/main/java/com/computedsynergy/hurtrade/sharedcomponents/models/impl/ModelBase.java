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

import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.db.DbConnection;
import org.sql2o.Sql2o;

import java.util.logging.Logger;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class ModelBase {
    
    protected final Sql2o sql2o;
    protected final DbConnection db;

    //logging
    protected Logger _log = Logger.getLogger(this.getClass().getName());
    
    public ModelBase(){
        
        CommandLineOptions options = CommandLineOptions.getInstance();
        
        db = DbConnection.getInstance();
        db.init("jdbc:postgresql://" + options.dbHost
                + ":" + options.dbPort + "/" + options.database,
                options.dbUsername, options.dbPassword);
        
        this.sql2o = db.getSql2o();
    }
}
