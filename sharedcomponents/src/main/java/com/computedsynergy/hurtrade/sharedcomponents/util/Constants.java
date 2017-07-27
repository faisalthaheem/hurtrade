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
package com.computedsynergy.hurtrade.sharedcomponents.util;

import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverPosition;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class Constants {
    public static final String QUEUE_NAME_RATES = "rates";
    public static final String EXCHANGE_NAME_RATES = "rates";
    public static final String QUEUE_NAME_AUTH = "authReqQ";
    public static final String EXCHANGE_NAME_AUTH = "authExchange";

    public static Type TYPE_DICTIONARY = new TypeToken<Map<String, String>>(){}.getType();
    public static Type TYPE_POSITIONS_MAP = new TypeToken<Map<UUID, Position>>(){}.getType();
    public static Type TYPE_COV_POSITIONS_MAP = new TypeToken<Map<UUID, CoverPosition>>(){}.getType();
    public static Type TYPE_TRADING_SCHEDULE = new TypeToken<List<int[]>>(){}.getType();
    public static Type TYPE_STRING_ARRAY = new TypeToken<String[]>(){}.getType();

    public static final String USERTYPE_SERVICE = "service";
    public static final String USERTYPE_ADMIN = "admin";
    public static final String USERTYPE_DEALER = "dealer";
    public static final String USERTYPE_TRADER = "trader";

    public static final String ORDER_TYPE_BUY = "buy";
    public static final String ORDER_TYPE_SELL = "sell";

    public static final String ORDER_STATE_PENDING_OPEN = "pending_dealer_open";
    public static final String ORDER_STATE_OPEN = "open";
    public static final String ORDER_STATE_PENDING_CLOSE = "pending_dealer_close";
    public static final String ORDER_STATE_CLOSED = "closed";
    public static final String ORDER_STATE_REJECTED_OPEN = "rejected_open";
    public static final String ORDER_STATE_CANCELED = "canceled";
    public static final String ORDER_STATE_REQUOTED = "requoted";


    public static final String SERIALS_POSITIONS_FRIENDLY_ORDER_ID = "positions";

}
