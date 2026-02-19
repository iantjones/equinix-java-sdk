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
import api.equinix.javasdk.customerportal.client.internal.TroubleTicketClient;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketJson;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.TroubleTicketWrapper;

import java.util.Map;

public class TroubleTicketClientImpl extends PageableBase implements TroubleTicketClient<TroubleTicket> {

    public TroubleTicketClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "TroubleTickets");
    }

    public Page<TroubleTicket, TroubleTicketJson> list() {
        EquinixRequest<TroubleTicket> equinixRequest = this.buildRequest("ListTroubleTickets", RequestType.PAGINATED, TroubleTicketJson.getPagedTypeRef());
        EquinixResponse<TroubleTicket> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public TroubleTicketJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<TroubleTicketJson> equinixRequest = this.buildRequestWithPathParams("GetTroubleTicket", RequestType.SINGLE, pParams, TroubleTicketJson.getSingleTypeRef());
        EquinixResponse<TroubleTicketJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public TroubleTicketJson create(TroubleTicketCreatorJson troubleTicketCreatorJson) {
        EquinixRequest<TroubleTicketJson> equinixRequest = this.buildRequest("PostTroubleTicket", RequestType.SINGLE, TroubleTicketJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, troubleTicketCreatorJson);
        EquinixResponse<TroubleTicketJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public TroubleTicketJson update(String uuid, TroubleTicketCreatorJson troubleTicketCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<TroubleTicketJson> equinixRequest = this.buildRequestWithPathParams("UpdateTroubleTicket", RequestType.SINGLE, pParams, TroubleTicketJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, troubleTicketCreatorJson);
        EquinixResponse<TroubleTicketJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public TroubleTicketJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<TroubleTicket> nextPage(PaginatedRequest<TroubleTicket> equinixRequest) {
        EquinixResponse<TroubleTicket> equinixResponse = this.invoke(equinixRequest);
        Page<TroubleTicket, TroubleTicketJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<TroubleTicket> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, TroubleTicketWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
