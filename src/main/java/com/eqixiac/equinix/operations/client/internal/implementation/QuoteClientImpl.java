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

package com.eqixiac.equinix.operations.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.operations.client.implementation.OperationsConfigImpl;
import com.eqixiac.equinix.operations.client.internal.QuoteClient;
import com.eqixiac.equinix.operations.model.Quote;
import com.eqixiac.equinix.operations.model.json.QuoteJson;
import com.eqixiac.equinix.operations.model.wrappers.QuoteWrapper;

import java.util.List;
import java.util.Map;

public class QuoteClientImpl extends ResourceClientBase<Quote, QuoteJson> implements QuoteClient<Quote> {

    public QuoteClientImpl(OperationsConfigImpl configClient) {
        super(configClient, "Operations", "Quotes", QuoteJson.class);
    }

    @Override
    protected Quote wrap(QuoteJson json) {
        return new QuoteWrapper(json, this);
    }

    public QuoteJson getByUuid(String quoteId) {
        return getOne("GetQuote", Map.of("quoteId", quoteId));
    }

    public QuoteJson getByUuid(String quoteId, List<String> ibxs) {
        if (ibxs == null || ibxs.isEmpty()) {
            return getByUuid(quoteId);
        }
        return getAs("GetQuote", Map.of("quoteId", quoteId), Map.of("ibxs", ibxs), QuoteJson.class);
    }

    public QuoteJson refresh(String quoteId) {
        return this.getByUuid(quoteId);
    }
}
