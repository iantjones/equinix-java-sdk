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
import api.equinix.javasdk.fabric.client.internal.StreamClient;
import api.equinix.javasdk.fabric.model.Stream;
import api.equinix.javasdk.fabric.model.json.StreamJson;
import api.equinix.javasdk.fabric.model.json.creators.StreamCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.StreamWrapper;

import java.util.Map;

public class StreamClientImpl extends PageableBase implements StreamClient<Stream> {

    public StreamClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Streams");
    }

    public Page<Stream, StreamJson> list() {
        EquinixRequest<Stream> equinixRequest = this.buildRequest("ListStreams", RequestType.PAGINATED, StreamJson.getPagedTypeRef());
        EquinixResponse<Stream> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public StreamJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<StreamJson> equinixRequest = this.buildRequestWithPathParams("GetStream", RequestType.SINGLE, pParams, StreamJson.getSingleTypeRef());
        EquinixResponse<StreamJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamJson create(StreamCreatorJson streamCreatorJson) {
        EquinixRequest<StreamJson> equinixRequest = this.buildRequest("PostStream", RequestType.SINGLE, StreamJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, streamCreatorJson);
        EquinixResponse<StreamJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamJson update(String uuid, StreamCreatorJson streamCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<StreamJson> equinixRequest = this.buildRequestWithPathParams("PutStream", RequestType.SINGLE, pParams, StreamJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, streamCreatorJson);
        EquinixResponse<StreamJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<Stream> equinixRequest = this.buildRequestWithPathParams("DeleteStream", RequestType.SINGLE, pParams, StreamJson.getSingleTypeRef());
        EquinixResponse<Stream> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<Stream> nextPage(PaginatedRequest<Stream> equinixRequest) {
        EquinixResponse<Stream> equinixResponse = this.invoke(equinixRequest);
        Page<Stream, StreamJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Stream> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, StreamWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
