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
package com.computedsynergy.hurtrade.sharedcomponents.dataexchange;

import com.computedsynergy.hurtrade.sharedcomponents.models.pojos.User;

/**
 *
 * @author Faisal Thaheem <faisal.ajmal@gmail.com>
 * 
 * Instead of solving the problem of who processes the quote for which user
 * we address the problem by duplicating the quote for each user and posting
 * to queue, the queue is then distributively processed by the cpus
 */
public class SourceQuote {
    private QuoteList quoteList;
    private User user;
    
    public SourceQuote(QuoteList q, User u){
        this.quoteList = q;
        this.user = u;
    }

    /**
     * @return the quoteList
     */
    public QuoteList getQuoteList() {
        return quoteList;
    }

    /**
     * @param quoteList the quoteList to set
     */
    public void setQuoteList(QuoteList quoteList) {
        this.quoteList = quoteList;
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    
    
}
