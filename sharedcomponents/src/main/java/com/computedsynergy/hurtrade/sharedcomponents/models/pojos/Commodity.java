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
package com.computedsynergy.hurtrade.sharedcomponents.models.pojos;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class Commodity {
    
    private int id;
    private String commodityname;
    private String commoditytype;
    private Date created;
    private Date modified;
    private BigDecimal lotsize;

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the commodityname
     */
    public String getCommodityname() {
        return commodityname;
    }

    /**
     * @param commodityname the commodityname to set
     */
    public void setCommodityname(String commodityname) {
        this.commodityname = commodityname;
    }


    /**
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * @return the modified
     */
    public Date getModified() {
        return modified;
    }

    /**
     * @param modified the modified to set
     */
    public void setModified(Date modified) {
        this.modified = modified;
    }

    /**
     * @return the commoditytype
     */
    public String getCommoditytype() {
        return commoditytype;
    }

    /**
     * @param commoditytype the commoditytype to set
     */
    public void setCommoditytype(String commoditytype) {
        this.commoditytype = commoditytype;
    }

    /**
     * @return the lotsize
     */
    public BigDecimal getLotsize() {
        return lotsize;
    }

    /**
     * @param lotsize the lotsize to set
     */
    public void setLotsize(BigDecimal lotsize) {
        this.lotsize = lotsize;
    }
    
    
}
