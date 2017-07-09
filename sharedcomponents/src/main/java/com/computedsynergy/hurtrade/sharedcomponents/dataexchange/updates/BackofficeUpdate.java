package com.computedsynergy.hurtrade.sharedcomponents.dataexchange.updates;

import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.Quote;
import com.computedsynergy.hurtrade.sharedcomponents.dataexchange.QuoteList;
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
}
