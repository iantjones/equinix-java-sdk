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
import api.equinix.javasdk.customerportal.client.internal.NotificationClient;
import api.equinix.javasdk.customerportal.model.Notification;
import api.equinix.javasdk.customerportal.model.json.NotificationJson;

import java.util.Map;

public class NotificationClientImpl extends PageableBase implements NotificationClient<Notification> {

    public NotificationClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Notifications");
    }

    public Page<Notification, NotificationJson> list() {
        EquinixRequest<Notification> equinixRequest = this.buildRequest("ListNotifications", RequestType.PAGINATED, NotificationJson.getPagedTypeRef());
        EquinixResponse<Notification> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public NotificationJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<NotificationJson> equinixRequest = this.buildRequestWithPathParams("GetNotification", RequestType.SINGLE, pParams, NotificationJson.getSingleTypeRef());
        EquinixResponse<NotificationJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<Notification> nextPage(PaginatedRequest<Notification> equinixRequest) {
        EquinixResponse<Notification> equinixResponse = this.invoke(equinixRequest);
        Page<Notification, NotificationJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Notification> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
