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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.PowerClient;
import api.equinix.javasdk.ibxsmartview.model.PowerReading;
import api.equinix.javasdk.ibxsmartview.model.json.PowerReadingJson;

import java.util.Map;

public class PowerClientImpl extends PageableBase implements PowerClient<PowerReading> {

    public PowerClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "Power");
    }

    public Page<PowerReading, PowerReadingJson> list(String ibx) {
        Map<String, String> pParams = Map.of("ibx", ibx);
        EquinixRequest<PowerReading> equinixRequest = this.buildRequestWithPathParams("ListPowerReadings", RequestType.PAGINATED, pParams, PowerReadingJson.getPagedTypeRef());
        EquinixResponse<PowerReading> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public PowerReadingJson getPowerReading(String ibx, String cabinetId) {
        Map<String, String> pParams = Map.of("ibx", ibx, "cabinetId", cabinetId);
        EquinixRequest<PowerReading> equinixRequest = this.buildRequestWithPathParams("GetPowerReading", RequestType.SINGLE, pParams, PowerReadingJson.getSingleTypeRef());
        EquinixResponse<PowerReading> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<PowerReading> nextPage(PaginatedRequest<PowerReading> equinixRequest) {
        EquinixResponse<PowerReading> equinixResponse = this.invoke(equinixRequest);
        Page<PowerReading, PowerReadingJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<PowerReading> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
