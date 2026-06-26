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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.QuoteClient;
import api.equinix.javasdk.customerportal.model.Quote;
import api.equinix.javasdk.customerportal.model.json.QuoteJson;
import api.equinix.javasdk.customerportal.model.json.creators.QuoteCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.QuoteWrapper;

public class QuoteClientImpl extends ResourceClientBase<Quote, QuoteJson> implements QuoteClient<Quote> {

    public QuoteClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Quotes", QuoteJson.class);
    }

    @Override
    protected Quote wrap(QuoteJson json) {
        return new QuoteWrapper(json, this);
    }

    public Page<Quote, QuoteJson> list() {
        return listPage("ListQuotes");
    }

    public QuoteJson getByUuid(String uuid) {
        return getOne("GetQuote", uuid);
    }

    public QuoteJson create(QuoteCreatorJson quoteCreatorJson) {
        return postOne("CreateQuote", quoteCreatorJson);
    }

    public QuoteJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
