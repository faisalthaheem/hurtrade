package com.computedsynergy.hurtrade.sharedcomponents.db;

import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;

import java.util.logging.Level;
import java.util.logging.Logger;

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
