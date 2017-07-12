package com.computedsynergy.hurtrade.sharedcomponents.models.pojos;

import java.util.Date;
import java.util.List;

/**
 * Created by faisal.t on 7/12/2017.
 */
public class CoverAccount {

    private int id;
    private String title;
    private boolean active;
    private Date created;
    private int office_id;
    private List<CoverPosition> coverPositions;

    public CoverAccount Clone(){

        CoverAccount clone = new CoverAccount();
        clone.setActive(active);
        clone.setCreated(created);
        clone.setId(id);
        clone.setOffice_id(office_id);
        clone.setTitle(title);

        return clone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getOffice_id() {
        return office_id;
    }

    public void setOffice_id(int office_id) {
        this.office_id = office_id;
    }

    public List<CoverPosition> getCoverPositions() {
        return coverPositions;
    }

    public void setCoverPositions(List<CoverPosition> coverPositions) {
        this.coverPositions = coverPositions;
    }
}
