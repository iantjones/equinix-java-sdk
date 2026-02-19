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

package api.equinix.javasdk.internetaccess.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.InternetAccessPortClient;
import api.equinix.javasdk.internetaccess.model.InternetAccessPort;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessPortJson;

import java.util.Map;

public class InternetAccessPortClientImpl extends PageableBase implements InternetAccessPortClient<InternetAccessPort> {

    public InternetAccessPortClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "Ports");
    }

    public Page<InternetAccessPort, InternetAccessPortJson> list() {
        EquinixRequest<InternetAccessPort> equinixRequest = this.buildRequest("ListPorts", RequestType.PAGINATED, InternetAccessPortJson.getPagedTypeRef());
        EquinixResponse<InternetAccessPort> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public InternetAccessPortJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<InternetAccessPortJson> equinixRequest = this.buildRequestWithPathParams("GetPort", RequestType.SINGLE, pParams, InternetAccessPortJson.getSingleTypeRef());
        EquinixResponse<InternetAccessPortJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<InternetAccessPort> nextPage(PaginatedRequest<InternetAccessPort> equinixRequest) {
        EquinixResponse<InternetAccessPort> equinixResponse = this.invoke(equinixRequest);
        Page<InternetAccessPort, InternetAccessPortJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<InternetAccessPort> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
