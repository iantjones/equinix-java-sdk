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

package api.equinix.javasdk.messaging.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.messaging.client.implementation.MessagingConfigImpl;
import api.equinix.javasdk.messaging.client.internal.SubscriptionClient;
import api.equinix.javasdk.messaging.model.Subscription;
import api.equinix.javasdk.messaging.model.json.SubscriptionJson;
import api.equinix.javasdk.messaging.model.json.creators.SubscriptionCreatorJson;
import api.equinix.javasdk.messaging.model.wrappers.SubscriptionWrapper;

import java.util.Map;

public class SubscriptionClientImpl extends PageableBase implements SubscriptionClient<Subscription> {

    public SubscriptionClientImpl(MessagingConfigImpl configClient) {
        super(configClient, "Messaging", "Subscriptions");
    }

    public Page<Subscription, SubscriptionJson> list() {
        EquinixRequest<Subscription> equinixRequest = this.buildRequest("ListSubscriptions", RequestType.PAGINATED, SubscriptionJson.getPagedTypeRef());
        EquinixResponse<Subscription> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public SubscriptionJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SubscriptionJson> equinixRequest = this.buildRequestWithPathParams("GetSubscription", RequestType.SINGLE, pParams, SubscriptionJson.getSingleTypeRef());
        EquinixResponse<SubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SubscriptionJson create(SubscriptionCreatorJson subscriptionCreatorJson) {
        EquinixRequest<SubscriptionJson> equinixRequest = this.buildRequest("CreateSubscription", RequestType.SINGLE, SubscriptionJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, subscriptionCreatorJson);
        EquinixResponse<SubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SubscriptionJson update(String uuid, SubscriptionCreatorJson subscriptionCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SubscriptionJson> equinixRequest = this.buildRequestWithPathParams("UpdateSubscription", RequestType.SINGLE, pParams, SubscriptionJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, subscriptionCreatorJson);
        EquinixResponse<SubscriptionJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SubscriptionJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<Subscription> equinixRequest = this.buildRequestWithPathParams("DeleteSubscription", RequestType.SINGLE, pParams, SubscriptionJson.getSingleTypeRef());
        EquinixResponse<Subscription> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SubscriptionJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<Subscription> nextPage(PaginatedRequest<Subscription> equinixRequest) {
        EquinixResponse<Subscription> equinixResponse = this.invoke(equinixRequest);
        Page<Subscription, SubscriptionJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Subscription> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, SubscriptionWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
