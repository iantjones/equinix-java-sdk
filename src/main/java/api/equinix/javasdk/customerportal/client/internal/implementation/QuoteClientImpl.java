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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.QuoteClient;
import api.equinix.javasdk.customerportal.model.Quote;
import api.equinix.javasdk.customerportal.model.json.QuoteJson;
import api.equinix.javasdk.customerportal.model.json.creators.QuoteCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.QuoteWrapper;

import java.util.Map;

public class QuoteClientImpl extends PageableBase implements QuoteClient<Quote> {

    public QuoteClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Quotes");
    }

    public Page<Quote, QuoteJson> list() {
        EquinixRequest<Quote> equinixRequest = this.buildRequest("ListQuotes", RequestType.PAGINATED, QuoteJson.getPagedTypeRef());
        EquinixResponse<Quote> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public QuoteJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<QuoteJson> equinixRequest = this.buildRequestWithPathParams("GetQuote", RequestType.SINGLE, pParams, QuoteJson.getSingleTypeRef());
        EquinixResponse<QuoteJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public QuoteJson create(QuoteCreatorJson quoteCreatorJson) {
        EquinixRequest<QuoteJson> equinixRequest = this.buildRequest("CreateQuote", RequestType.SINGLE, QuoteJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, quoteCreatorJson);
        EquinixResponse<QuoteJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public QuoteJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<Quote> nextPage(PaginatedRequest<Quote> equinixRequest) {
        EquinixResponse<Quote> equinixResponse = this.invoke(equinixRequest);
        Page<Quote, QuoteJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Quote> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, QuoteWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
