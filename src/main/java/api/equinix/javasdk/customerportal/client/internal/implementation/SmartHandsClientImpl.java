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
import api.equinix.javasdk.customerportal.client.internal.SmartHandsClient;
import api.equinix.javasdk.customerportal.model.SmartHands;
import api.equinix.javasdk.customerportal.model.json.SmartHandsJson;
import api.equinix.javasdk.customerportal.model.json.creators.SmartHandsCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.SmartHandsWrapper;

import java.util.Map;

public class SmartHandsClientImpl extends PageableBase implements SmartHandsClient<SmartHands> {

    public SmartHandsClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "SmartHands");
    }

    public Page<SmartHands, SmartHandsJson> list() {
        EquinixRequest<SmartHands> equinixRequest = this.buildRequest("ListSmartHands", RequestType.PAGINATED, SmartHandsJson.getPagedTypeRef());
        EquinixResponse<SmartHands> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public SmartHandsJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SmartHandsJson> equinixRequest = this.buildRequestWithPathParams("GetSmartHands", RequestType.SINGLE, pParams, SmartHandsJson.getSingleTypeRef());
        EquinixResponse<SmartHandsJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SmartHandsJson create(SmartHandsCreatorJson smartHandsCreatorJson) {
        EquinixRequest<SmartHandsJson> equinixRequest = this.buildRequest("CreateSmartHands", RequestType.SINGLE, SmartHandsJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, smartHandsCreatorJson);
        EquinixResponse<SmartHandsJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SmartHandsJson update(String uuid, SmartHandsCreatorJson smartHandsCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SmartHandsJson> equinixRequest = this.buildRequestWithPathParams("UpdateSmartHands", RequestType.SINGLE, pParams, SmartHandsJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, smartHandsCreatorJson);
        EquinixResponse<SmartHandsJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SmartHandsJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<SmartHands> nextPage(PaginatedRequest<SmartHands> equinixRequest) {
        EquinixResponse<SmartHands> equinixResponse = this.invoke(equinixRequest);
        Page<SmartHands, SmartHandsJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<SmartHands> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, SmartHandsWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
