/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.customerportal.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.customerportal.client.internal.implementation.QuoteClientImpl;
import com.eqixiac.equinix.customerportal.model.Quote;
import com.eqixiac.equinix.customerportal.model.json.QuoteJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class QuoteWrapper extends ResourceImpl<Quote> implements Quote {

    @Delegate(excludes = QuoteMutability.class)
    private QuoteJson jsonObject;
    @Getter
    private final Pageable<Quote> serviceClient;

    public QuoteWrapper(QuoteJson quoteJson, Pageable<Quote> serviceClient) {
        this.jsonObject = quoteJson;
        this.serviceClient = serviceClient;
    }

    public void refresh() {
        this.jsonObject = ((QuoteClientImpl)this.serviceClient).refresh(this.getQuoteId());
    }

    private interface QuoteMutability {
        void refresh();
    }
}
