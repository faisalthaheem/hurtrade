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
    public String database = "hurtrade";
    
    @Parameter(names = {"--db-host"})
    public String dbHost = "localhost";

    @Parameter(names = {"--db-username"})
    public String dbUsername = "postgres";

    @Parameter(names = {"--db-password"})
    public String dbPassword = "";

    @Parameter(names = {"--db-port"})
    public Integer dbPort = 5432;

    @Parameter(names = {"--mq-host"})
    public String mqHost = "localhost";
    
    @Parameter(names = {"--mq-username"})
    public String mqUsername = "guest";
    
    @Parameter(names = {"--mq-password"})
    public String mqPassword = "guest";
    
    @Parameter(names = {"--redis-server"})
    public String redisServer = "192.168.56.101";

    @Parameter(names = {"--yahoo-fx-frequency"})
    public Integer yahooFxFrequency = 3000;
}
