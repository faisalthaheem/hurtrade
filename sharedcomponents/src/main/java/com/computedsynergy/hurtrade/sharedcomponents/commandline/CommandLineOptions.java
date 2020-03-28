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
package com.computedsynergy.hurtrade.sharedcomponents.commandline;

import com.beust.jcommander.Parameter;

public class CommandLineOptions {
    
    private static CommandLineOptions thisInstance=null;
    
    private CommandLineOptions(){
        
    }
    
    public static CommandLineOptions getInstance(){
        
        if(null == thisInstance){
            
            thisInstance = new CommandLineOptions();
        }
        
        return thisInstance;
    }

    @Parameter(names = "--debug")
    public boolean debug = false;
    
    @Parameter(names = {"--database"})
    public String database = "postgres";
    
    @Parameter(names = {"--db-host"})
    public String dbHost = "localhost";

    @Parameter(names = {"--db-username"})
    public String dbUsername = "postgres";

    @Parameter(names = {"--db-password"})
    public String dbPassword = "faisal123";

    @Parameter(names = {"--db-port"})
    public Integer dbPort = 5432;

    @Parameter(names = {"--mq-host"})
    public String mqHost = "localhost";
    
    @Parameter(names = {"--mq-username"})
    public String mqUsername = "svc";
    
    @Parameter(names = {"--mq-password"})
    public String mqPassword = "svc";

    @Parameter(names = {"--mq-stats-interval"})
    public Integer mqStatsInterval = 5000;

    @Parameter(names = {"--mq-exchange-name-stats"})
    public String mqExchangeNameStats = "stats";

    @Parameter(names = {"--mq-rabbitmq-management-base-url"})
    public String mqRabbitMqManagementBaseUrl = "http://localhost:15672";
    
    @Parameter(names = {"--redis-server"})
    public String redisServer = "localhost";

    @Parameter(names = {"--yahoo-fx-frequency"})
    public Integer yahooFxFrequency = 3000;

    @Parameter(names = {"--gecko-query-frequency"})
    public Integer geckoQueryFrequency = 1000;

    @Parameter(names = {"--requote-network-delay"})
    public Integer requoteNetworkDelay = 1; //how much grace period on top of the allowed time within which the client has to respond (in secs)

    @Parameter(names = {"--requote-timeout"})
    public Integer requoteTimeout = 10; //(in secs

    @Parameter(names = {"--max-queue-ttl"})
    public Integer maxQueueTtl = 5000; //(in secs// )

    @Parameter(names = {"--max-queued-messages"})
    public Integer maxQueuedMessages = 50; // max messages stored in the queue
}
