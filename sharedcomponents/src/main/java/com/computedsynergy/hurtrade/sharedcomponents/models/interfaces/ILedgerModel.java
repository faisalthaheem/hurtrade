package com.computedsynergy.hurtrade.sharedcomponents.models.interfaces;

import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.LedgerRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Created by faisal.t on 7/3/2017.
 */
public interface ILedgerModel {

    public BigDecimal GetAvailableCashForUser(int user_id);
    public Boolean SaveRealizedPositionPL(int user_id, UUID orderId, BigDecimal realizedPL);
    public List<LedgerRow> GetLedgerForUser(int user_id);
    public boolean saveFeeForPosition(int user_id, UUID orderId, BigDecimal amt);
    public boolean saveCommissionForPosition(int user_id, UUID orderId, BigDecimal amt);
}
