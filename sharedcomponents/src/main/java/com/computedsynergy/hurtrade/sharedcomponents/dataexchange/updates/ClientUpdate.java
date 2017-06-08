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
package com.computedsynergy.hurtrade.sharedcomponents.dataexchange.updates;

import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.trade.TradeResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 */
public class ClientUpdate {

    private final Map<UUID, Position> positions;
    private final QuoteList clientQuotes;
    private final TradeResponse tradeResponse;
    
    public ClientUpdate(Map<UUID, Position> positions, QuoteList clientQuotes){
        this.positions = positions;
        this.clientQuotes = clientQuotes;
        this.tradeResponse = null;
    }

    public ClientUpdate(TradeResponse tradeResponse){
        this.positions = new HashMap<>();
        this.clientQuotes = new QuoteList();
        this.tradeResponse = tradeResponse;
    }

    /**
     * @return the positions
     */
    public Map<UUID, Position> getPositions() {
        return positions;
    }

    /**
     * @return the clientQuotes
     */
    public QuoteList getClientQuotes() {
        return clientQuotes;
    }


    public TradeResponse getTradeResponse() {
        return tradeResponse;
    }
}
