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

import java.util.UUID;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class MqNamingUtil {
    
    private static final String E_OFFICE_NAME_SUFFIX = "_off";
    private static final String Q_OFFICE_REQUEST_NAME_SUFFIX = "_off_requests";
    private static final String Q_OFFICE_DEALER_OUT_NAME_SUFFIX = "_off_dealer_out";
    private static final String Q_OFFICE_DEALER_IN_NAME_SUFFIX = "_off_dealer_in";
    private static final String E_CLIENT_NAME_SUFFIX = "_client";
    private static final String Q_CLIENT_OUTGOING_NAME_SUFFIX = "_client_outgoing";
    private static final String Q_CLIENT_INCOMING_NAME_SUFFIX = "_client_incoming";

    public static final String MQ_GEN_Q_NAME_PREFIX = "amq.gen";
    public static final String Q_NAME_MQ_STATS_COMMAND = "mqstats.commands";

    
    public static String getOfficeExchangeName(UUID uuid){
        return uuid.toString() + E_OFFICE_NAME_SUFFIX;
    }
    
    public static String getOfficeClientRequestQueueName(UUID uuid){
        return uuid.toString() + Q_OFFICE_REQUEST_NAME_SUFFIX;
    }
    
    public static String getOfficeDealerOutQueueName(UUID uuid){
        return uuid.toString() + Q_OFFICE_DEALER_OUT_NAME_SUFFIX;
    }
    
    public static String getOfficeDealerINQueueName(UUID uuid){
        return uuid.toString() + Q_OFFICE_DEALER_IN_NAME_SUFFIX;
    }
    
    public static String getClientExchangeName(UUID uuid){
        return uuid.toString() + E_CLIENT_NAME_SUFFIX;
    }
    
    public static String getClientOutgoingQueueName(UUID uuid){
        return uuid.toString() + Q_CLIENT_OUTGOING_NAME_SUFFIX;
    }

    public static String getClientIncomingQueueName(UUID uuid){
        return uuid.toString() + Q_CLIENT_INCOMING_NAME_SUFFIX;
    }
}
