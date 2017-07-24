package com.computedsynergy.hurtrade.sharedcomponents.util;

import com.computedsynergy.hurtrade.sharedcomponents.models.impl.ScheduleModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.impl.SerialsModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Schedule;
import com.google.gson.Gson;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by faisal.t on 7/18/2017.
 */
public class GeneralUtil {

    private static final SerialsModel serials = new SerialsModel();
    private static final ConcurrentHashMap<Integer, List<int[]>> _schedules = new ConcurrentHashMap<>();

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
}
