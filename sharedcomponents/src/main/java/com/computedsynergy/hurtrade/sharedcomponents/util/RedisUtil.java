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
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CommodityUser;
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
    public static final String USER_SPREAD_MAP_PREFIX = "spreadmap_";
    public static final String LOCK_USER_SPREAD_MAP_PREFIX = "lock_";
    public static final int TIMEOUT_LOCK_SPREAD_MAP = 5000;
    public static final int EXPIRY_LOCK_SPREAD_MAP = 10000;
    
    
    private static RedisUtil _self = null;
    
    Jedis jedis;
    
    Gson gson = new Gson();
        
    
    private RedisUtil(){
        
    }
    
    public static RedisUtil getInstance(){
        if(_self == null){
            _self = new RedisUtil();
            _self.initialize();
        }
        
        return _self;
    }
    
    private void initialize(){
        
      //todo add clustering support by seeing if there is a comma separated list of addresses
      //https://github.com/xetorthio/jedis
      jedis = new Jedis(CommandLineOptions.getInstance().redisServer);
    }
    
    public static String getUserSpreadMapName(UUID userUuid)
    {
        return USER_SPREAD_MAP_PREFIX + userUuid.toString();
    }
    
    public static String getLockNameForSpreadMap(String spreadMapName)
    {
        return LOCK_USER_SPREAD_MAP_PREFIX + spreadMapName;
    }
    
    public void setUserSpreadMap(String userMapName, List<CommodityUser> userCommodities) 
    {
        try {
            Map<String, BigDecimal> userSpreadMap = new HashMap<String, BigDecimal>();
            for(CommodityUser cu: userCommodities){
                userSpreadMap.put(cu.getCommodity(), cu.getSpread());
            }
            
            String serializedMap = gson.toJson(userSpreadMap);
            
            JedisLock lock = new JedisLock(jedis, getLockNameForSpreadMap(userMapName), TIMEOUT_LOCK_SPREAD_MAP, EXPIRY_LOCK_SPREAD_MAP);
            if(lock.acquire()){
                
                jedis.set(userMapName, serializedMap);
                
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
            
            JedisLock lock = new JedisLock(jedis, getLockNameForSpreadMap(userMapName), TIMEOUT_LOCK_SPREAD_MAP, EXPIRY_LOCK_SPREAD_MAP);
            if(lock.acquire()){
                
                userSpreadMap = gson.fromJson(
                        jedis.get(userMapName),
                        mapType
                );
                
            }else{
                Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, "Could not get user spread map for " + userMapName);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(RedisUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return userSpreadMap;
    }
    
}
