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

package api.equinix.javasdk.ibxsmartview.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.SystemAlertClient;
import api.equinix.javasdk.ibxsmartview.model.SystemAlert;
import api.equinix.javasdk.ibxsmartview.model.json.SystemAlertJson;

import java.util.List;
import java.util.Map;

public class SystemAlertClientImpl extends PageableBase implements SystemAlertClient<SystemAlert> {

    public SystemAlertClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "SystemAlerts");
    }

    public Page<SystemAlert, SystemAlertJson> search(String status, String assetClassification, String edgeCollectedOn, int offset, int limit) {
        Map<String, List<String>> qParams = Map.of(
                "status", List.of(status),
                "assetClassification", List.of(assetClassification),
                "edgeCollectedOn", List.of(edgeCollectedOn),
                "offset", List.of(String.valueOf(offset)),
                "limit", List.of(String.valueOf(limit))
        );
        EquinixRequest<SystemAlert> equinixRequest = this.buildRequestWithQueryParams("SearchAlertsGet", RequestType.PAGINATED, qParams, SystemAlertJson.getPagedTypeRef());
        EquinixResponse<SystemAlert> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public Page<SystemAlert, SystemAlertJson> searchPost(Object filterBody) {
        EquinixRequest<SystemAlert> equinixRequest = this.buildRequest("SearchAlertsPost", RequestType.PAGINATED_POST, SystemAlertJson.getPagedTypeRef());
        Utils.serializeJson(equinixRequest, filterBody);
        EquinixResponse<SystemAlert> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<SystemAlert> nextPage(PaginatedRequest<SystemAlert> equinixRequest) {
        EquinixResponse<SystemAlert> equinixResponse = this.invoke(equinixRequest);
        Page<SystemAlert, SystemAlertJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<SystemAlert> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
