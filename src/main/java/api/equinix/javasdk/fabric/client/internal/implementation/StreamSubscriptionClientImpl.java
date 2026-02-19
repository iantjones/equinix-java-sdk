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
import api.equinix.javasdk.fabric.client.internal.StreamSubscriptionClient;
import api.equinix.javasdk.fabric.model.StreamSubscription;
import api.equinix.javasdk.fabric.model.json.StreamSubscriptionJson;
import api.equinix.javasdk.fabric.model.json.creators.StreamSubscriptionCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.StreamSubscriptionWrapper;

import java.util.Map;

public class StreamSubscriptionClientImpl extends PageableBase implements StreamSubscriptionClient<StreamSubscription> {

    public StreamSubscriptionClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "StreamSubscriptions");
    }

    public Page<StreamSubscription, StreamSubscriptionJson> list(String streamId) {
        Map<String, String> pParams = Map.of("streamId", streamId);
        EquinixRequest<StreamSubscription> equinixRequest = this.buildRequestWithPathParams("ListStreamSubscriptions", RequestType.PAGINATED, pParams, StreamSubscriptionJson.getPagedTypeRef());
        EquinixResponse<StreamSubscription> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public StreamSubscriptionJson getByUuid(String streamId, String uuid) {
        Map<String, String> pParams = Map.of("streamId", streamId, "uuid", uuid);
        EquinixRequest<StreamSubscriptionJson> equinixRequest = this.buildRequestWithPathParams("GetStreamSubscription", RequestType.SINGLE, pParams, StreamSubscriptionJson.getSingleTypeRef());
        EquinixResponse<StreamSubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamSubscriptionJson create(String streamId, StreamSubscriptionCreatorJson streamSubscriptionCreatorJson) {
        Map<String, String> pParams = Map.of("streamId", streamId);
        EquinixRequest<StreamSubscriptionJson> equinixRequest = this.buildRequestWithPathParams("PostStreamSubscription", RequestType.SINGLE, pParams, StreamSubscriptionJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, streamSubscriptionCreatorJson);
        EquinixResponse<StreamSubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamSubscriptionJson update(String streamId, String uuid, StreamSubscriptionCreatorJson streamSubscriptionCreatorJson) {
        Map<String, String> pParams = Map.of("streamId", streamId, "uuid", uuid);
        EquinixRequest<StreamSubscriptionJson> equinixRequest = this.buildRequestWithPathParams("PutStreamSubscription", RequestType.SINGLE, pParams, StreamSubscriptionJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, streamSubscriptionCreatorJson);
        EquinixResponse<StreamSubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamSubscriptionJson delete(String streamId, String uuid) {
        Map<String, String> pParams = Map.of("streamId", streamId, "uuid", uuid);
        EquinixRequest<StreamSubscription> equinixRequest = this.buildRequestWithPathParams("DeleteStreamSubscription", RequestType.SINGLE, pParams, StreamSubscriptionJson.getSingleTypeRef());
        EquinixResponse<StreamSubscription> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamSubscriptionJson refresh(String streamId, String uuid) {
        return this.getByUuid(streamId, uuid);
    }

    public PaginatedList<StreamSubscription> nextPage(PaginatedRequest<StreamSubscription> equinixRequest) {
        EquinixResponse<StreamSubscription> equinixResponse = this.invoke(equinixRequest);
        Page<StreamSubscription, StreamSubscriptionJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<StreamSubscription> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, StreamSubscriptionWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
