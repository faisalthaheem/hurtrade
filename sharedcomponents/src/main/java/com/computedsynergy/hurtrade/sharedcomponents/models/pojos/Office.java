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

import java.util.UUID;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class Office {
    private int id;
    private String officename;
    private UUID officeuuid;
    
    public Office(int id, String officename, UUID officeuuid){
        this.id = id;
        this.officename = officename;
        this.officeuuid =  officeuuid;
    }

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
     * @return the officename
     */
    public String getOfficename() {
        return officename;
    }

    /**
     * @param officename the officename to set
     */
    public void setOfficename(String officename) {
        this.officename = officename;
    }

    /**
     * @return the officeuuid
     */
    public UUID getOfficeuuid() {
        return officeuuid;
    }

    /**
     * @param officeuuid the officeuuid to set
     */
    public void setOfficeuuid(UUID officeuuid) {
        this.officeuuid = officeuuid;
    }
    
    
}
