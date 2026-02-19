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
import api.equinix.javasdk.ibxsmartview.client.internal.StreamingSubscriptionClient;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.json.StreamingSubscriptionJson;
import api.equinix.javasdk.ibxsmartview.model.json.SubscriptionCertificateJson;
import api.equinix.javasdk.ibxsmartview.model.json.SubscriptionDataJson;
import api.equinix.javasdk.ibxsmartview.model.json.creators.StreamingSubscriptionCreatorJson;
import api.equinix.javasdk.ibxsmartview.model.wrappers.StreamingSubscriptionWrapper;

import java.util.List;
import java.util.Map;

public class StreamingSubscriptionClientImpl extends PageableBase implements StreamingSubscriptionClient<StreamingSubscription> {

    public StreamingSubscriptionClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "StreamingSubscriptions");
    }

    public List<StreamingSubscriptionJson> list() {
        EquinixRequest<StreamingSubscription> equinixRequest = this.buildRequest("ListSubscriptions", RequestType.LIST, StreamingSubscriptionJson.getListTypeRef());
        EquinixResponse<StreamingSubscription> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleListResponse(equinixResponse, equinixRequest);
    }

    public StreamingSubscriptionJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<StreamingSubscriptionJson> equinixRequest = this.buildRequestWithPathParams("GetSubscription", RequestType.SINGLE, pParams, StreamingSubscriptionJson.getSingleTypeRef());
        EquinixResponse<StreamingSubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamingSubscriptionJson create(StreamingSubscriptionCreatorJson creatorJson) {
        EquinixRequest<StreamingSubscriptionJson> equinixRequest = this.buildRequest("CreateSubscription", RequestType.SINGLE, StreamingSubscriptionJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, creatorJson);
        EquinixResponse<StreamingSubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamingSubscriptionJson update(String uuid, StreamingSubscriptionCreatorJson creatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<StreamingSubscriptionJson> equinixRequest = this.buildRequestWithPathParams("UpdateSubscription", RequestType.SINGLE, pParams, StreamingSubscriptionJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, creatorJson);
        EquinixResponse<StreamingSubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamingSubscriptionJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<StreamingSubscription> equinixRequest = this.buildRequestWithPathParams("DeleteSubscription", RequestType.SINGLE, pParams, StreamingSubscriptionJson.getSingleTypeRef());
        EquinixResponse<StreamingSubscription> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public StreamingSubscriptionJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public SubscriptionDataJson getSubscriptionData(String subscriptionId) {
        Map<String, String> pParams = Map.of("subscriptionId", subscriptionId);
        EquinixRequest<SubscriptionDataJson> equinixRequest = this.buildRequestWithPathParams("GetSubscriptionData", RequestType.SINGLE, pParams, SubscriptionDataJson.getSingleTypeRef());
        EquinixResponse<SubscriptionDataJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SubscriptionCertificateJson getCertificate(String channelType) {
        Map<String, List<String>> qParams = Map.of("channelType", List.of(channelType));
        EquinixRequest<SubscriptionCertificateJson> equinixRequest = this.buildRequestWithQueryParams("GetCertificate", RequestType.SINGLE, qParams, SubscriptionCertificateJson.getSingleTypeRef());
        EquinixResponse<SubscriptionCertificateJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<StreamingSubscription> nextPage(PaginatedRequest<StreamingSubscription> equinixRequest) {
        EquinixResponse<StreamingSubscription> equinixResponse = this.invoke(equinixRequest);
        Page<StreamingSubscription, StreamingSubscriptionJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<StreamingSubscription> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, StreamingSubscriptionWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
