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

import com.computedsynergy.hurtrade.sharedcomponents.models.impl.SavedPositionModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.ScheduleModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.SerialsModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.SavedPosition;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Schedule;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;
import com.google.gson.Gson;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.EXPIRY_LOCK_USER_POSITIONS;
import static com.computedsynergy.hurtrade.sharedcomponents.util.RedisUtil.TIMEOUT_LOCK_USER_POSITIONS;

/**
 * Created by faisal.t on 7/18/2017.
 */
public class GeneralUtil {

    private static final SerialsModel serials = new SerialsModel();
    private static final ConcurrentHashMap<Integer, List<int[]>> _schedules = new ConcurrentHashMap<>();
    private static final SavedPositionModel savedPositionModel = new SavedPositionModel();
    private static final ExecutorService _singleExecutorService = Executors.newFixedThreadPool(10);;

    private static Logger _log = Logger.getLogger(GeneralUtil.class.getName());

    public static long GetNextFriendlyOrderId(){

        long ret = 0;

        ret = serials.getNextSerial(Constants.SERIALS_POSITIONS_FRIENDLY_ORDER_ID);

        return ret;
    }

    /**
     * populates schdules private member
     * should be called during bootstrap
     */
    public static void refreshTradingSchedule(){

        _log.info("Begin loading schedules.");

        ScheduleModel schedule = new ScheduleModel();

        List<Schedule> rows = schedule.getSchedules();

        Gson gson = new Gson();

        for(Schedule row : rows){

            List<int[]> sch = gson.fromJson(row.getSchedule(), Constants.TYPE_TRADING_SCHEDULE);

            if(_schedules.containsKey(row.getDayofweek())){
                _schedules.replace(row.getDayofweek(), sch);
                _log.info("Replaced schedule for day: " + row.getDayofweek() + " " + row.getSchedule());
            }else {
                _schedules.put(row.getDayofweek(), sch);
                _log.info("Loaded schedule for day: " + row.getDayofweek() + " " + row.getSchedule());
            }
        }

        _log.info("End loading schedules.");

    }

    /**
     * It is expected that database rows contain the periods of day as milliseconds of day
     * @return
     */
    public static boolean isTradingOpen(){

        boolean ret = false;

        int dow = new LocalDate().dayOfWeek().get();
        int now = new LocalTime().getMillisOfDay();

        if(_schedules.containsKey(dow)) {
            List<int[]> schedule = _schedules.get(dow);
            if(schedule.size() > 0){
                for(int[] period : schedule){

                    if(now >= period[0] && now <= period[1]){

                        ret = true;
                        break;
                    }
                }
            }
        }
        return ret;
    }

    public static void loadClientPositions(User user)
    {
        try {
            _singleExecutorService.submit(() -> {
                 try {

                    SavedPosition position = savedPositionModel.getPositions(user.getId());

                    if(position != null) {

                        String keyName = RedisUtil.getUserPositionsKeyName(user.getUseruuid());
                        String lockName = RedisUtil.getLockNameForUserPositions(keyName);

                        RedisUtil.getInstance().SetString(
                                keyName,
                                lockName,
                                TIMEOUT_LOCK_USER_POSITIONS,
                                EXPIRY_LOCK_USER_POSITIONS,
                                position.getPositiondata()
                        );

                        _log.info("Loaded positions for: " + user.getUsername());
                    }else{
                        _log.warning("Saved positions found null for: " + user.getUsername());
                    }

                }catch (Exception ex){
                    _log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });

        }catch (Exception ex){
            _log.log(Level.SEVERE, ex.getMessage(), ex);
        }

    }

    public static void saveClientPositions(User user, String positions)
    {
        try {

            _singleExecutorService.submit(() -> {

                try {
                    SavedPosition p = new SavedPosition(user.getId(), positions);
                    savedPositionModel.savePositions(p);
                }catch (Exception ex){
                    _log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });

        }catch (Exception ex){
            _log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
