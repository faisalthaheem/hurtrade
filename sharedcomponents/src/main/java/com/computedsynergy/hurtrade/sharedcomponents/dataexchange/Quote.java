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
package com.computedsynergy.hurtrade.sharedcomponents.dataexchange;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com> 
 */
public class Quote {
    
    public final BigDecimal bid;
    public final BigDecimal ask;
    public final Date quoteTime;
    public final BigDecimal rate;
    public final String name;
    public final BigDecimal lotSize;
    
    
    public Quote(BigDecimal bid, BigDecimal ask, Date quoteTime, BigDecimal rate, String name, BigDecimal lotSize){
        this.bid = bid;
        this.ask = ask;
        this.quoteTime = quoteTime;
        this.rate = rate;
        this.name = name;
        this.lotSize = lotSize;
    }
    
    public Quote(String bid, String ask, String date, String time, String rate, String name, BigDecimal lotSize) throws ParseException{

        this.bid = new BigDecimal(bid);
        this.ask = new BigDecimal(ask);
        this.rate= new BigDecimal(rate);

        DateFormat format = new SimpleDateFormat("M/d/yyyy h:mma", Locale.ENGLISH);
        this.quoteTime = format.parse(date + " " + time);

        this.name = name;

        this.lotSize = lotSize;
    }
    
    public boolean equals(Object o){
        return o instanceof Quote && ((Quote)o).name.equals(this.name);
    }
}
