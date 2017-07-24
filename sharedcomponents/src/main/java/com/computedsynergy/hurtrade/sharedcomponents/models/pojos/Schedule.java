package com.computedsynergy.hurtrade.sharedcomponents.models.pojos;

import java.util.Date;
import java.util.List;

public class Schedule {

    private int id;
    private int dayofweek;
    private String schedule;
    private Date ended;
    private Date created;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDayofweek() {
        return dayofweek;
    }

    public void setDayofweek(int dayofweek) {
        this.dayofweek = dayofweek;
    }


    public Date getEnded() {
        return ended;
    }

    public void setEnded(Date ended) {
        this.ended = ended;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
}
