package com.computedsynergy.hurtrade.sharedcomponents.models.interfaces;

import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverPosition;

import java.util.List;

/**
 * Created by faisal.t on 7/12/2017.
 */
public interface ICoverPositionModel {

    public List<CoverPosition> listCoverPositionsForAccount(int coverAccountId);
    public boolean saveUpdateCoverPosition(CoverPosition p);
}
