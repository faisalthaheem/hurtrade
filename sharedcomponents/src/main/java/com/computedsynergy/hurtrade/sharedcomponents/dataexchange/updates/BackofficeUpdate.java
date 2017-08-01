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
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverAccount;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.CoverPosition;
import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by faisal.t on 7/9/2017.
 */
public class BackofficeUpdate {

    private Map<String, List<Position>> userPositions = new HashMap<>();
    private QuoteList quotes = new QuoteList();
    private List<CoverAccount> coverAccounts = new ArrayList<>();
    private List<CoverPosition> coverPositions = new ArrayList<>();

    public Map<String, List<Position>> getUserPositions() {
        return userPositions;
    }

    public void setUserPositions(Map<String, List<Position>> userPositions) {
        this.userPositions = userPositions;
    }

    public QuoteList getQuotes() {
        return quotes;
    }

    public void setQuotes(QuoteList quotes) {
        this.quotes = quotes;
    }

    public List<CoverAccount> getCoverAccounts() {
        return coverAccounts;
    }

    public void setCoverAccounts(List<CoverAccount> coverAccounts) {
        this.coverAccounts = coverAccounts;
    }

    public List<CoverPosition> getCoverPositions() {
        return coverPositions;
    }

    public void setCoverPositions(List<CoverPosition> coverPositions) {
        this.coverPositions = coverPositions;
    }
}
