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
package com.computedsynergy.hurtrade.sharedcomponents.dataexchange.charting;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by faisal.t on 7/3/2017.
 */
public class CandleStick {
    private BigDecimal highest;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal lowest;
    private Date sampleFor;

    //where does this fall on selected period
    private int segmentNumber;


    public CandleStick(){

    }

    public CandleStick(BigDecimal h, BigDecimal o, BigDecimal c, BigDecimal l){
        this.highest = h;
        this.open = o;
        this.close = c;
        this.lowest = l;
    }

    public BigDecimal getHighest() {
        return highest;
    }

    public void setHighest(BigDecimal highest) {
        this.highest = highest;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public BigDecimal getLowest() {
        return lowest;
    }

    public void setLowest(BigDecimal lowest) {
        this.lowest = lowest;
    }

    public int getSegmentNumber() {
        return segmentNumber;
    }

    public void setSegmentNumber(int segmentNumber) {
        this.segmentNumber = segmentNumber;
    }

    public Date getSampleFor() {
        return sampleFor;
    }

    public void setSampleFor(Date sampleFor) {
        this.sampleFor = sampleFor;
    }
}
