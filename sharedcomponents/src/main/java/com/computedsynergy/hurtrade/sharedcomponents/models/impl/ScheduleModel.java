package com.computedsynergy.hurtrade.sharedcomponents.models.impl;

import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.IScheduleModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.interfaces.ISerialsModel;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Schedule;
import org.sql2o.Connection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by faisal.t on 7/18/2017.
 */
public class ScheduleModel extends ModelBase implements IScheduleModel {
    @Override
    public List<Schedule> getSchedules() {

        List<Schedule> ret = new ArrayList<>();

        try (Connection conn = sql2o.open()) {

            //update and then select

            String query = "select * from schedules where ended is null";

            ret = conn.createQuery(query)
                    .executeAndFetch(Schedule.class);

        }catch(Exception ex){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

        return ret;
    }

}
