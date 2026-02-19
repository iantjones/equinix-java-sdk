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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.PrecisionTimeClient;
import api.equinix.javasdk.fabric.model.PrecisionTime;
import api.equinix.javasdk.fabric.model.json.PrecisionTimeJson;
import api.equinix.javasdk.fabric.model.json.creators.PrecisionTimeCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.PrecisionTimeWrapper;

import java.util.Map;

public class PrecisionTimeClientImpl extends PageableBase implements PrecisionTimeClient<PrecisionTime> {

    public PrecisionTimeClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "PrecisionTime");
    }

    public Page<PrecisionTime, PrecisionTimeJson> list() {
        EquinixRequest<PrecisionTime> equinixRequest = this.buildRequest("ListPrecisionTime", RequestType.PAGINATED, PrecisionTimeJson.getPagedTypeRef());
        EquinixResponse<PrecisionTime> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public PrecisionTimeJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<PrecisionTimeJson> equinixRequest = this.buildRequestWithPathParams("GetPrecisionTime", RequestType.SINGLE, pParams, PrecisionTimeJson.getSingleTypeRef());
        EquinixResponse<PrecisionTimeJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PrecisionTimeJson create(PrecisionTimeCreatorJson precisionTimeCreatorJson) {
        EquinixRequest<PrecisionTimeJson> equinixRequest = this.buildRequest("PostPrecisionTime", RequestType.SINGLE, PrecisionTimeJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, precisionTimeCreatorJson);
        EquinixResponse<PrecisionTimeJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PrecisionTimeJson update(String uuid, PrecisionTimeCreatorJson precisionTimeCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<PrecisionTimeJson> equinixRequest = this.buildRequestWithPathParams("PutPrecisionTime", RequestType.SINGLE, pParams, PrecisionTimeJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, precisionTimeCreatorJson);
        EquinixResponse<PrecisionTimeJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PrecisionTimeJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<PrecisionTime> equinixRequest = this.buildRequestWithPathParams("DeletePrecisionTime", RequestType.SINGLE, pParams, PrecisionTimeJson.getSingleTypeRef());
        EquinixResponse<PrecisionTime> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PrecisionTimeJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<PrecisionTime> nextPage(PaginatedRequest<PrecisionTime> equinixRequest) {
        EquinixResponse<PrecisionTime> equinixResponse = this.invoke(equinixRequest);
        Page<PrecisionTime, PrecisionTimeJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<PrecisionTime> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, PrecisionTimeWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
