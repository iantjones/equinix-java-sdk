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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.customerportal.client.Quotes;
import com.eqixiac.equinix.customerportal.client.internal.QuoteClient;
import com.eqixiac.equinix.customerportal.model.Quote;
import com.eqixiac.equinix.customerportal.model.json.QuoteJson;
import com.eqixiac.equinix.customerportal.model.wrappers.QuoteWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuotesImpl implements Quotes {

    private final QuoteClient<Quote> serviceClient;

    private final CustomerPortal serviceManager;

    public Quote getByUuid(String quoteId) {
        QuoteJson quoteJson = this.serviceClient.getByUuid(quoteId);
        return new QuoteWrapper(quoteJson, this.serviceClient);
    }

    public Quote getByUuid(String quoteId, List<String> ibxs) {
        QuoteJson quoteJson = this.serviceClient.getByUuid(quoteId, ibxs);
        return new QuoteWrapper(quoteJson, this.serviceClient);
    }
}
