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

import com.zaxxer.hikari.*;
import org.sql2o.Sql2o;
import org.sql2o.quirks.PostgresQuirks;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class DbConnection {

    private static DbConnection _singleton = null;

    private DbConnection() {

    }

    public static DbConnection getInstance() {

        if (null == _singleton) {
            _singleton = new DbConnection();
        }

        return _singleton;
    }

    private HikariDataSource ds = null;

    public void init(String jdbcURl, String username, String password) {

        if (ds == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcURl);
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            ds = new HikariDataSource(config);
        }
    }

    public Sql2o getSql2o() {
        final Sql2o sql2o = new Sql2o(ds, new PostgresQuirks());
        return sql2o;
    }
}
