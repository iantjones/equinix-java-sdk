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
import api.equinix.javasdk.ibxsmartview.client.internal.EnvironmentalClient;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import api.equinix.javasdk.ibxsmartview.model.json.SensorReadingJson;
import api.equinix.javasdk.ibxsmartview.model.wrappers.SensorReadingWrapper;

import java.util.Map;

public class EnvironmentalClientImpl extends PageableBase implements EnvironmentalClient<SensorReading> {

    public EnvironmentalClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "Environmental");
    }

    public Page<SensorReading, SensorReadingJson> list(String ibx) {
        Map<String, String> pParams = Map.of("ibx", ibx);
        EquinixRequest<SensorReading> equinixRequest = this.buildRequestWithPathParams("ListSensorReadings", RequestType.PAGINATED, pParams, SensorReadingJson.getPagedTypeRef());
        EquinixResponse<SensorReading> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public SensorReadingJson getSensorReading(String ibx, String sensorId) {
        Map<String, String> pParams = Map.of("ibx", ibx, "sensorId", sensorId);
        EquinixRequest<SensorReading> equinixRequest = this.buildRequestWithPathParams("GetSensorReading", RequestType.SINGLE, pParams, SensorReadingJson.getSingleTypeRef());
        EquinixResponse<SensorReading> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<SensorReading> nextPage(PaginatedRequest<SensorReading> equinixRequest) {
        EquinixResponse<SensorReading> equinixResponse = this.invoke(equinixRequest);
        Page<SensorReading, SensorReadingJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<SensorReading> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, SensorReadingWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
