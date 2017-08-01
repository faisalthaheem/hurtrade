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
package com.computedsynergy.hurtrade.sharedcomponents.models.interfaces;

import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.LedgerRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Created by faisal.t on 7/3/2017.
 */
public interface ILedgerModel {

    BigDecimal GetAvailableCashForUser(int user_id);
    Boolean SaveRealizedPositionPL(int user_id, UUID orderId, BigDecimal realizedPL);
    List<LedgerRow> GetLedgerForUser(int user_id);
    boolean saveFeeForPosition(int user_id, UUID orderId, BigDecimal amt);
    boolean saveCommissionForPosition(int user_id, UUID orderId, BigDecimal amt);
}
