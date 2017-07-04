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

import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
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
    public static Type POSITIONS_MAP_TYPE = new TypeToken<Map<UUID, Position>>(){}.getType();
}
