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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class User {

    private int id;
    private String username;
    private String pass;
    private boolean locked;
    private String usertype;
    private String phonenumber;
    private String fullname;
    private String email;
    private Date created;
    private Date ended;
    private UUID useruuid;
    private String authtags;
    private boolean liquidate;
    private Office userOffice;

    private List<String> tagsList;
    private List<String> resourcesList;

    public User(){
        tagsList = new ArrayList();
        resourcesList = new ArrayList();
    }
    
    public User(int id, String username, String pass, boolean locked, String usertype,
        String phonenumber, String fullname, String email, Date created, Date ended, UUID useruuid,
                String authtags, boolean liquidate, Office userOffice)
    {
        this.id = id;
        this.username = username;
        this.pass = pass;
        this.locked = locked;
        this.usertype = usertype;
        this.phonenumber = phonenumber;
        this.fullname = fullname;
        this.email = email;
        this.created = created;
        this.ended = ended;
        this.useruuid = useruuid;
        this.setAuthtags(authtags);
        this.liquidate = liquidate;
        this.setUserOffice(userOffice);
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
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the pass
     */
    public String getPass() {
        return pass;
    }

    /**
     * @param pass the pass to set
     */
    public void setPass(String pass) {
        this.pass = pass;
    }

    /**
     * @return the locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked the locked to set
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * @return the usertype
     */
    public String getUsertype() {
        return usertype;
    }

    /**
     * @param usertype the usertype to set
     */
    public void setUsertype(String usertype) {
        this.usertype = usertype;
    }

    /**
     * @return the phonenumber
     */
    public String getPhonenumber() {
        return phonenumber;
    }

    /**
     * @param phonenumber the phonenumber to set
     */
    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    /**
     * @return the fullname
     */
    public String getFullname() {
        return fullname;
    }

    /**
     * @param fullname the fullname to set
     */
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
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
     * @return the ended
     */
    public Date getEnded() {
        return ended;
    }

    /**
     * @param ended the ended to set
     */
    public void setEnded(Date ended) {
        this.ended = ended;
    }

    /**
     * @return the useruuid
     */
    public UUID getUseruuid() {
        return useruuid;
    }

    /**
     * @param useruuid the useruuid to set
     */
    public void setUseruuid(UUID useruuid) {
        this.useruuid = useruuid;
    }


    public String getAuthtags() {
        return authtags;
    }

    public void setAuthtags(String authtags) {
        this.authtags = authtags;
    }

    public Office getUserOffice() {
        return userOffice;
    }

    public void setUserOffice(Office userOffice) {
        this.userOffice = userOffice;
    }

    public boolean isLiquidate() {
        return liquidate;
    }

    public void setLiquidate(boolean liquidate) {
        this.liquidate = liquidate;
    }

    public List<String> getTagsList() {
        if(tagsList == null)
        {
            System.out.println("Encountered null tagsList for user " + this.username);
            this.tagsList = new ArrayList();
        }
        return tagsList;
    }

    public void setTagsList(List<String> tagsList) {
        this.tagsList = tagsList;
    }

    public List<String> getResourcesList() {
        if(resourcesList == null)
        {
            System.out.println("Encountered null resourcesList for user " + this.username);
            this.resourcesList = new ArrayList();
        }
        return resourcesList;
    }

    public void setResourcesList(List<String> resourcesList) {
        this.resourcesList = resourcesList;
    }
}
