package com.computedsynergy.hurtrade.sharedcomponents.dataexchange.charting;

/**
 * Created by faisal.t on 7/3/2017.
 */
public class CandleStick {
    private double highest;
    private double open;
    private double close;
    private double lowest;

    //where does this fall on selected period
    private int segmentNumber;


    public CandleStick(){

    }

    public CandleStick(double h, double o, double c, double l){
        this.highest = h;
        this.open = o;
        this.close = c;
        this.lowest = l;
    }

    public double getHighest() {
        return highest;
    }

    public void setHighest(double highest) {
        this.highest = highest;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getLowest() {
        return lowest;
    }

    public void setLowest(double lowest) {
        this.lowest = lowest;
    }

    public int getSegmentNumber() {
        return segmentNumber;
    }

    public void setSegmentNumber(int segmentNumber) {
        this.segmentNumber = segmentNumber;
    }
}
