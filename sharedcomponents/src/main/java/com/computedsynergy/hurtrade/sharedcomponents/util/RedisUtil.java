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
package com.computedsynergy.hurtrade.sharedcomponents.util;

import com.computedsynergy.hurtrade.sharedcomponents.commandline.CommandLineOptions;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.positions.Position;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CommodityUser;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.github.jedis.lock.JedisLock;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 * 
 * Contains constants, functions and everything related to redis cache
 * 
 */
public class RedisUtil {
    //public static final String CLIENT_LIST_NAME = 
    private static String USER_SPREAD_MAP_PREFIX = "spreadmap_";
    private static String USER_POSITIONS_KEY_PREFIX = "positions_";
    
    private static String LOCK_USER_SPREAD_MAP_PREFIX = "lock_";
    private static int TIMEOUT_LOCK_SPREAD_MAP = 5000;
    private static int EXPIRY_LOCK_SPREAD_MAP = 10000;
    
    private static String LOCK_USER_POSITIONS_PREFIX = "lock_";
    private static int TIMEOUT_LOCK_USER_POSITIONS = 5000;
    private static int EXPIRY_LOCK_USER_POSITIONS = 10000;
    
    
    private static RedisUtil _self = null;

    /**
     * @return the USER_SPREAD_MAP_PREFIX
     */
    public static String getUSER_SPREAD_MAP_PREFIX() {
        return USER_SPREAD_MAP_PREFIX;
    }

    /**
     * @param aUSER_SPREAD_MAP_PREFIX the USER_SPREAD_MAP_PREFIX to set
     */
    public static void setUSER_SPREAD_MAP_PREFIX(String aUSER_SPREAD_MAP_PREFIX) {
        USER_SPREAD_MAP_PREFIX = aUSER_SPREAD_MAP_PREFIX;
    }

    /**
     * @return the USER_POSITIONS_KEY_PREFIX
     */
    public static String getUSER_POSITIONS_KEY_PREFIX() {
        return USER_POSITIONS_KEY_PREFIX;
    }

    /**
     * @param aUSER_POSITIONS_KEY_PREFIX the USER_POSITIONS_KEY_PREFIX to set
     */
    public static void setUSER_POSITIONS_KEY_PREFIX(String aUSER_POSITIONS_KEY_PREFIX) {
        USER_POSITIONS_KEY_PREFIX = aUSER_POSITIONS_KEY_PREFIX;
    }

    /**
     * @return the LOCK_USER_SPREAD_MAP_PREFIX
     */
    public static String getLOCK_USER_SPREAD_MAP_PREFIX() {
        return LOCK_USER_SPREAD_MAP_PREFIX;
    }

    /**
     * @param aLOCK_USER_SPREAD_MAP_PREFIX the LOCK_USER_SPREAD_MAP_PREFIX to set
     */
    public static void setLOCK_USER_SPREAD_MAP_PREFIX(String aLOCK_USER_SPREAD_MAP_PREFIX) {
        LOCK_USER_SPREAD_MAP_PREFIX = aLOCK_USER_SPREAD_MAP_PREFIX;
    }

    /**
     * @return the TIMEOUT_LOCK_SPREAD_MAP
     */
    public static int getTIMEOUT_LOCK_SPREAD_MAP() {
        return TIMEOUT_LOCK_SPREAD_MAP;
    }

    /**
     * @param aTIMEOUT_LOCK_SPREAD_MAP the TIMEOUT_LOCK_SPREAD_MAP to set
     */
    public static void setTIMEOUT_LOCK_SPREAD_MAP(int aTIMEOUT_LOCK_SPREAD_MAP) {
        TIMEOUT_LOCK_SPREAD_MAP = aTIMEOUT_LOCK_SPREAD_MAP;
    }

    /**
     * @return the EXPIRY_LOCK_SPREAD_MAP
     */
    public static int getEXPIRY_LOCK_SPREAD_MAP() {
        return EXPIRY_LOCK_SPREAD_MAP;
    }

    /**
     * @param aEXPIRY_LOCK_SPREAD_MAP the EXPIRY_LOCK_SPREAD_MAP to set
     */
    public static void setEXPIRY_LOCK_SPREAD_MAP(int aEXPIRY_LOCK_SPREAD_MAP) {
        EXPIRY_LOCK_SPREAD_MAP = aEXPIRY_LOCK_SPREAD_MAP;
    }

    /**
     * @return the LOCK_USER_POSITIONS_PREFIX
     */
    public static String getLOCK_USER_POSITIONS_PREFIX() {
        return LOCK_USER_POSITIONS_PREFIX;
    }

    /**
     * @param aLOCK_USER_POSITIONS_PREFIX the LOCK_USER_POSITIONS_PREFIX to set
     */
    public static void setLOCK_USER_POSITIONS_PREFIX(String aLOCK_USER_POSITIONS_PREFIX) {
        LOCK_USER_POSITIONS_PREFIX = aLOCK_USER_POSITIONS_PREFIX;
    }

    /**
     * @return the TIMEOUT_LOCK_USER_POSITIONS
     */
    public static int getTIMEOUT_LOCK_USER_POSITIONS() {
        return TIMEOUT_LOCK_USER_POSITIONS;
    }

    /**
     * @param aTIMEOUT_LOCK_USER_POSITIONS the TIMEOUT_LOCK_USER_POSITIONS to set
     */
    public static void setTIMEOUT_LOCK_USER_POSITIONS(int aTIMEOUT_LOCK_USER_POSITIONS) {
        TIMEOUT_LOCK_USER_POSITIONS = aTIMEOUT_LOCK_USER_POSITIONS;
    }

    /**
     * @return the EXPIRY_LOCK_USER_POSITIONS
     */
    public static int getEXPIRY_LOCK_USER_POSITIONS() {
        return EXPIRY_LOCK_USER_POSITIONS;
    }

    /**
     * @param aEXPIRY_LOCK_USER_POSITIONS the EXPIRY_LOCK_USER_POSITIONS to set
     */
    public static void setEXPIRY_LOCK_USER_POSITIONS(int aEXPIRY_LOCK_USER_POSITIONS) {
        EXPIRY_LOCK_USER_POSITIONS = aEXPIRY_LOCK_USER_POSITIONS;
    }

    /**
     * @return the _self
     */
    public static RedisUtil getSelf() {
        return _self;
    }

    /**
     * @param aSelf the _self to set
     */
    public static void setSelf(RedisUtil aSelf) {
        _self = aSelf;
    }
    
    private Jedis jedis;
    
    private Gson gson = new Gson();
    
    
        
    
    private RedisUtil(){
        
    }
    
    public static RedisUtil getInstance(){
        if(getSelf() == null){
            setSelf(new RedisUtil());
            getSelf().initialize();
        }
        
        return getSelf();
    }
    
    private void initialize(){
        
      //todo add clustering support by seeing if there is a comma separated list of addresses
      //https://github.com/xetorthio/jedis
        setJedis(new Jedis(CommandLineOptions.getInstance().redisServer));
    }
    
    public static String getUserSpreadMapName(UUID userUuid)
    {
        return getUSER_SPREAD_MAP_PREFIX() + userUuid.toString();
    }
    
    public static String getLockNameForSpreadMap(String spreadMapName)
    {
        return getLOCK_USER_SPREAD_MAP_PREFIX() + spreadMapName;
    }
    
    /**
     * Returns a lock name which is used to acquire mutex lock over a use's positions
     * @param userUuid
     * @return 
     */
    public static String getLockNameForUserPositions(String userPositionKey)
    {
        return getLOCK_USER_POSITIONS_PREFIX() + userPositionKey;
    }
    
    /**
     * Returns the key under which a particular user's positions are stored
     * @param userUuid
     * @return 
     */
    public static String getUserPositionsKeyName(UUID userUuid)
    {
        return getUSER_POSITIONS_KEY_PREFIX() + userUuid.toString();
    }
    
    public void setUserSpreadMap(String userMapName, List<CommodityUser> userCommodities) 
    {
        try {
            Map<String, BigDecimal> userSpreadMap = new HashMap<String, BigDecimal>();
            for(CommodityUser cu: userCommodities){
                userSpreadMap.put(cu.getCommodityname(), cu.getSpread());
            }
            
            String serializedMap = getGson().toJson(userSpreadMap);
            
            JedisLock lock = new JedisLock(getJedis(), getLockNameForSpreadMap(userMapName), getTIMEOUT_LOCK_SPREAD_MAP(), getEXPIRY_LOCK_SPREAD_MAP());
            if(lock.acquire()){
                
                getJedis().set(userMapName, serializedMap);
                
            }else{
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, "Could not set user spread map for " + userMapName);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public Map<String, BigDecimal> getUserSpreadMap(String userMapName)
    {
        Type mapType = new TypeToken<Map<String, BigDecimal>>(){}.getType();
        Map<String, BigDecimal> userSpreadMap = new HashMap<String,BigDecimal>();
            
        try {
            
            JedisLock lock = new JedisLock(getJedis(), getLockNameForSpreadMap(userMapName), getTIMEOUT_LOCK_SPREAD_MAP(), getEXPIRY_LOCK_SPREAD_MAP());
            if(lock.acquire()){
                
                userSpreadMap = getGson().fromJson(getJedis().get(userMapName),
                        mapType
                );
                
                lock.release();
                
            }else{
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, "Could not get user spread map for " + userMapName);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return userSpreadMap;
    }
    
    /**
     * 
     * @param user
     * @param userPositions
     * @return serialized map
     */
    public String setUserPositions(User user, Map<UUID, Position> userPositions) 
    {
        String serializedPositions = "";
        
        try {
           
            serializedPositions = getGson().toJson(userPositions);
            String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());
            
            JedisLock lock = new JedisLock(getJedis(), getLockNameForUserPositions(userPositionsKeyName), getTIMEOUT_LOCK_USER_POSITIONS(), getEXPIRY_LOCK_USER_POSITIONS());
            if(lock.acquire()){
                
                getJedis().set(userPositionsKeyName, serializedPositions);
                
            }else{
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, "Could not set user positions " + user.getUsername());
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return serializedPositions;
    }
    
    public Map<UUID, Position> getUserPositions(User user)
    {
        Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();
        Map<UUID, Position> userPositions = new HashMap<UUID,Position>();
        String userPositionsKeyName = getUserPositionsKeyName(user.getUseruuid());
            
        try {
            
            JedisLock lock = new JedisLock(getJedis(), getLockNameForUserPositions(userPositionsKeyName), getTIMEOUT_LOCK_USER_POSITIONS(), getEXPIRY_LOCK_USER_POSITIONS());
            if(lock.acquire()){
                
                userPositions = getGson().fromJson(getJedis().get(userPositionsKeyName),
                        mapType
                );
                
                lock.release();
                
            }else{
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, "Could not get user positions " + user.getUsername());
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return userPositions;
    }

    /**
     * @return the jedis
     */
    public Jedis getJedis() {
        return jedis;
    }

    /**
     * @param jedis the jedis to set
     */
    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }

    /**
     * @return the gson
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * @param gson the gson to set
     */
    public void setGson(Gson gson) {
        this.gson = gson;
    }
    
    
    
    
}
