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
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.*;
import com.github.jedis.lock.JedisLock;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static com.computedsynergy.hurtrade.sharedcomponents.util.Constants.TYPE_COV_POSITIONS_MAP;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 * 
 * Contains constants, functions and everything related to redis cache
 * 
 */
public class RedisUtil {

    public static final String USER_SPREAD_MAP_PREFIX = "spreadmap_";
    public static final String LOCK_USER_SPREAD_MAP_PREFIX = "spreadlock_";
    public static final int TIMEOUT_LOCK_SPREAD_MAP = 200;
    public static final int EXPIRY_LOCK_SPREAD_MAP = 300;

    public static final String USER_POSITIONS_KEY_PREFIX = "positions_";
    public static final String LOCK_USER_POSITIONS_PREFIX = "poslock_";
    public static final int TIMEOUT_LOCK_USER_POSITIONS = 200;
    public static final int EXPIRY_LOCK_USER_POSITIONS = 300;

    //this one is just a lock
    public static final String LOCK_USER_PROCESSING_PREFIX = "lock_userprocessing_";
    public static final int TIMEOUT_LOCK_USER_PROCESSING = 5000;
    public static final int EXPIRY_LOCK_USER_PROCESSING = 10000;

    public static final String USER_QUOTES_KEY_PREFIX = "userquotes";
    public static final String LOCK_USER_QUOTES_PREFIX = "lock_userquotes_";
    public static final int TIMEOUT_LOCK_USER_QUOTES = 5000;
    public static final int EXPIRY_LOCK_USER_QUOTES = 10000;

    public static final String USER_INFO_PREFIX = "user_";
    public static final String LOCK_USER_INFO = "lock_userinfo_";
    public static final int TIMEOUT_LOCK_USER_INFO = 5000;
    public static final int EXPIRY_LOCK_USER_INFO = 10000;

    public static final String OFFICE_KEY_PREFIX = "office_";
    public static final String LOCK_OFFICE_INFO = "lock_officeinfo_";
    public static final int TIMEOUT_LOCK_OFFICE_INFO = 5000;
    public static final int EXPIRY_LOCK_OFFICE_INFO = 10000;

    public static final String OFFICE_COVER_POSITIONS_KEY_PREFIX = "off_cov_pos_";
    public static final String LOCK_COVER_POSITIONS = "lock_cov_pos_";
    public static final int TIMEOUT_LOCK_COVER_POSITIONS = 5000;
    public static final int EXPIRY_LOCK_COVER_POSITIONS = 10000;

    //active mq users

    //requotes
    public static final String REQUOTE_PREFIX = "requote_";
    
    private static RedisUtil _self = null;

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
    
    private JedisPool jedisPool;
    
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
        setJedis(new JedisPool(new JedisPoolConfig(), CommandLineOptions.getInstance().redisServer));
    }

    public static String getKeyNameforUserInfo(String username)
    {
        return USER_INFO_PREFIX + username;
    }

    public static String getUserSpreadMapName(UUID userUuid)
    {
        return USER_SPREAD_MAP_PREFIX + userUuid.toString();
    }
    
    public static String getLockNameForSpreadMap(String spreadMapName)
    {
        return LOCK_USER_SPREAD_MAP_PREFIX+ spreadMapName;
    }
    
    public static String getLockNameForUserProcessing(UUID userUuid)
    {
        return LOCK_USER_PROCESSING_PREFIX + userUuid.toString();
    }

    public static String getLockNameForUserInfo(String username)
    {
        return LOCK_USER_INFO + username;
    }
    
    public static String getLockNameForUserQuotes(String keyName)
    {
        return LOCK_USER_QUOTES_PREFIX + keyName;
    }
    
    public static String getKeyNameForUserQuotes(UUID userUuid)
    {
        return USER_QUOTES_KEY_PREFIX + userUuid.toString();
    }
    
    /**
     * Returns a lock name which is used to acquire mutex lock over a use's positions
     * @param
     * @return 
     */
    public static String getLockNameForUserPositions(String userPositionKey)
    {
        return LOCK_USER_POSITIONS_PREFIX + userPositionKey;
    }
    
    /**
     * Returns the key under which a particular user's positions are stored
     * @param userUuid
     * @return 
     */
    public static String getUserPositionsKeyName(UUID userUuid)
    {
        return USER_POSITIONS_KEY_PREFIX + userUuid.toString();
    }

    public static String getKeyNameForOffCovPos(UUID officeid){
        return OFFICE_COVER_POSITIONS_KEY_PREFIX + officeid.toString();
    }

    public static String getLockNameForOffCovPos(UUID officeid){
        return LOCK_COVER_POSITIONS + officeid.toString();
    }
    
    public void cacheUserCommodities(UUID userUUID, List<CommodityUser> userCommodities)
    {
        try {

            String userMapName=RedisUtil.getUserSpreadMapName(userUUID);

            Map<String, CommodityUser> commodityMap = new HashMap<>();
            for(CommodityUser cu: userCommodities){
                commodityMap.put(cu.getCommodityname(), cu);
            }
            
            String serializedMap = getGson().toJson(commodityMap);
            
            try(Jedis jedis = jedisPool.getResource()){
                JedisLock lock = new JedisLock(jedis, getLockNameForSpreadMap(userMapName), TIMEOUT_LOCK_SPREAD_MAP, EXPIRY_LOCK_SPREAD_MAP);
                if(lock.acquire()){

                    jedis.set(userMapName, serializedMap);

                }else{
                    Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, "Could not set user spread map for {0}", userMapName);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public Map<String, CommodityUser> getCachedUserCommodities(UUID userUUID)
    {
        String userMapName = RedisUtil.getUserSpreadMapName(userUUID);
        Type mapType = new TypeToken<Map<String, CommodityUser>>(){}.getType();
        Map<String, CommodityUser> userCommodityMap = new HashMap<>();
            
        try {
            try(Jedis jedis = jedisPool.getResource()){
                JedisLock lock = new JedisLock(jedis, getLockNameForSpreadMap(userMapName), TIMEOUT_LOCK_SPREAD_MAP, EXPIRY_LOCK_SPREAD_MAP);
                if(lock.acquire()){

                    userCommodityMap = getGson().fromJson(jedis.get(userMapName),
                            mapType
                    );

                    lock.release();

                }else{
                    Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, "Could not get user spread map for {0}",  userMapName);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return userCommodityMap;
    }
    
    public void setSeriaizedQuotesForClient(String quotes, UUID clientId)
    {
        try(Jedis jedis = jedisPool.getResource())
        {
            String keyName = getKeyNameForUserQuotes(clientId);
            
            JedisLock lock = new JedisLock(jedis, getLockNameForUserQuotes(keyName), TIMEOUT_LOCK_USER_QUOTES, EXPIRY_LOCK_USER_QUOTES);
            try {
                if(lock.acquire()){
                    
                    jedis.set(keyName, quotes);
                    
                    lock.release();
                    
                }else{
                    Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, "Could not set user quotes for {0}", keyName);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public String getSeriaizedQuotesForClient(UUID clientId)
    {
        String response = "";
        
        try(Jedis jedis = jedisPool.getResource())
        {
            String keyName = getKeyNameForUserQuotes(clientId);
            
            JedisLock lock = new JedisLock(jedis, getLockNameForUserQuotes(keyName), TIMEOUT_LOCK_USER_QUOTES, EXPIRY_LOCK_USER_QUOTES);
            try {
                if(lock.acquire()){
                    
                    response = jedis.get(keyName);
                    
                    lock.release();
                    
                }else{
                    Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, "Could not set user quotes for {0}",  keyName);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return response;
    }

    public String GetString(String keyName, String lockName, int lockTimeout, int lockExpiry){
        String response = "";

        try(Jedis jedis = jedisPool.getResource())
        {
            JedisLock lock = new JedisLock(jedis, lockName,
                    lockTimeout, lockExpiry);
            try {
                if(lock.acquire()){

                    response = jedis.get(keyName);

                    lock.release();

                }else{
                    Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, "Could not get value for key {0}",  keyName);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return response;
    }

    public boolean SetString(String keyName, String lockName, int lockTimeout, int lockExpiry, String valueToSet){
        boolean ret = false;

        try(Jedis jedis = jedisPool.getResource())
        {
            JedisLock lock = new JedisLock(jedis, lockName,
                    lockTimeout, lockExpiry);
            try {
                if(lock.acquire()){

                    jedis.set(keyName, valueToSet);

                    lock.release();

                    ret = true;

                }else{
                    Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, "Could not set value for key {0}",  keyName);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return ret;
    }

    public boolean SetString(String keyName, String value, int expiry){

        boolean ret = false;

        try(Jedis jedis = jedisPool.getResource())
        {
            try {
                jedis.set(keyName, value);
                ret = jedis.expire(keyName, expiry) == 1;

            } catch (Exception ex) {
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return ret;
    }

    public boolean IsKeySet(String keyName){

        boolean ret = false;

        try(Jedis jedis = jedisPool.getResource())
        {
            try {

                ret = jedis.exists(keyName);

            } catch (Exception ex) {
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        return ret;
    }

    public User GetUserInfo(String username){

        User u = null;

        String keyName = getKeyNameforUserInfo(username);
        String lockName = getLockNameForUserInfo(keyName);
        String json = GetString(keyName, lockName, TIMEOUT_LOCK_USER_INFO, EXPIRY_LOCK_USER_INFO);
        try {
            u = ObjectUtils.convertJsonToPOJO(json, User.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return u;
    }

    public boolean SetUserInfo(User u){

        String keyName = getKeyNameforUserInfo(u.getUsername());
        String lockName = getLockNameForUserInfo(keyName);
        String json = ObjectUtils.ObjectToJson(u);

        return SetString(keyName, lockName, TIMEOUT_LOCK_USER_INFO, EXPIRY_LOCK_USER_INFO, json);

    }

    public boolean SetOrderRequoted(UUID orderid, int expiry)
    {
        String keyName = REQUOTE_PREFIX + orderid.toString();
        return SetString(keyName, "", expiry);
    }

    public boolean GetOrderRequoteValid(UUID orderid){
        String keyName = REQUOTE_PREFIX + orderid.toString();

        return IsKeySet(keyName);
    }

    public List<Position> GetUserPositions(String username){

        List<Position> retPositions = new ArrayList<>();

        User u = GetUserInfo(username);

        String lockName = getLockNameForUserPositions(username);
        String keyName = getUserPositionsKeyName(u.getUseruuid());
        Type mapType = new TypeToken<Map<UUID, Position>>(){}.getType();

        String json="";


        if(u != null){
            try(Jedis jedis = jedisPool.getResource())
            {
                JedisLock lock = new JedisLock(jedis, lockName,
                        TIMEOUT_LOCK_USER_POSITIONS, EXPIRY_LOCK_USER_POSITIONS);
                try {
                    if(lock.acquire()){

                        json = jedis.get(keyName);

                        lock.release();


                        if(json != null && json.length() > 0){
                            Map<UUID, Position> positions = gson.fromJson(json,mapType);
                            if(positions!=null){
                                for(Position p:positions.values()){
                                    retPositions.add(p);
                                }
                            }
                        }

                    }else{
                        Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, "Could not set value for key {0}",  keyName);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return retPositions;
    }

    public List<CoverPosition> GetOfficeCoverPositions(UUID officeUUID){

        List<CoverPosition> ret = new ArrayList<>();

        String officeCoverPositionsKeyName = getKeyNameForOffCovPos(officeUUID);
        String officeCoverPositionsLockName = getLockNameForOffCovPos(officeUUID);

        try(Jedis jedis = RedisUtil.getInstance().getJedisPool().getResource()){
            JedisLock lock = new JedisLock(
                    jedis,
                    officeCoverPositionsLockName,
                    TIMEOUT_LOCK_COVER_POSITIONS,
                    EXPIRY_LOCK_COVER_POSITIONS);
            try{
                if(lock.acquire()){

                    Map<UUID, CoverPosition> positions = null;
                    if(!jedis.exists(officeCoverPositionsKeyName)){
                        positions = new HashMap<>();
                    }else {
                        positions = gson.fromJson(
                                jedis.get(officeCoverPositionsKeyName),
                                TYPE_COV_POSITIONS_MAP);
                    }
                    lock.release();

                    ret.addAll(positions.values());
                }else{
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Office cover position", "Unable to lock " + officeCoverPositionsLockName);
                }
            }catch(Exception ex){
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }

            return ret;
        }
    }

//    public Office GetOfficeInfo(String officeName){
//
//
//        String keyName = getKeyNameforUserInfo(username);
//        String lockName = getLockNameForUserInfo(keyName);
//        String val = GetString(keyName, lockName, TIMEOUT_LOCK_USER_INFO, EXPIRY_LOCK_USER_INFO);
//        Office off = new Gson().fromJson(new String(val), Office.class);
//
//        return response;
//    }
//
//    public boolean SetOfficeInfo(Office office){
//
//
//        String keyName = getKeyNameforUserInfo(username);
//        String lockName = getLockNameForUserInfo(keyName);
//        return SetString(keyName, lockName, TIMEOUT_LOCK_USER_INFO, EXPIRY_LOCK_USER_INFO, val);
//
//    }

    /**
     * @return the jedis
     */
    public JedisPool getJedisPool() {
        
        return jedisPool;
    }

    /**
     * @param jedis the jedis to set
     */
    public void setJedis(JedisPool jedis) {
        this.jedisPool = jedis;
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
