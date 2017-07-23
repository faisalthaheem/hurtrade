package com.computedsynergy.hurtrade.sharedcomponents.models.interfaces;

import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverAccount;

import java.util.List;

/**
 * Created by faisal.t on 7/12/2017.
 */
public interface INotificationModel {

    void saveNotification (int user_id, int office_id, String notification);
}
