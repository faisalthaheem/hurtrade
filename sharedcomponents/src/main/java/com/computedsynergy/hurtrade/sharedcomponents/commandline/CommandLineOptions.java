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
    
    @Parameter(names = {"--redis-server"})
    public String redisServer = "localhost";

    @Parameter(names = {"--yahoo-fx-frequency"})
    public Integer yahooFxFrequency = 3000;

    @Parameter(names = {"--requote-network-delay"})
    public Integer requoteNetworkDelay = 1; //how much grace period on top of the allowed time within which the client has to respond (in secs)

    @Parameter(names = {"--requote-timeout"})
    public Integer requoteTimeout = 10; //(in secs)
}
