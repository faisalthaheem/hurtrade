package com.computedsynergy.hurtrade.sharedcomponents.models.pojos;

import java.util.Date;

public class ConnectionInfo {

    private String username;
    private String ipaddress;
    private Date connectedat;
    //this name identifies the connection to rabbitmq, and is required to disconnect the user
    private String mqName;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public Date getConnectedat() {
        return connectedat;
    }

    public void setConnectedat(Date connectedat) {
        this.connectedat = connectedat;
    }

    public String getMqName() {
        return mqName;
    }

    public void setMqName(String mqName) {
        this.mqName = mqName;
    }
}
