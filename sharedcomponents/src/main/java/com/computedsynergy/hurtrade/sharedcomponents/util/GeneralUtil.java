package com.computedsynergy.hurtrade.sharedcomponents.util;

import com.computedsynergy.hurtrade.sharedcomponents.models.impl.SerialsModel;

/**
 * Created by faisal.t on 7/18/2017.
 */
public class GeneralUtil {

    private static final SerialsModel serials = new SerialsModel();

    public static long GetNextFriendlyOrderId(){

        long ret = 0;

        ret = serials.getNextSerial(Constants.SERIALS_POSITIONS_FRIENDLY_ORDER_ID);

        return ret;
    }
}
