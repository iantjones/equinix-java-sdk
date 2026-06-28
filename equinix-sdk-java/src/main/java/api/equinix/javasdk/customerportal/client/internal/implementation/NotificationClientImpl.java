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

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.NotificationClient;
import api.equinix.javasdk.customerportal.model.Notification;
import api.equinix.javasdk.customerportal.model.json.NotificationJson;
import api.equinix.javasdk.customerportal.model.json.NotificationSearchResponseJson;
import api.equinix.javasdk.customerportal.model.json.creators.NotificationSearchRequest;

import java.util.List;
import java.util.Map;

public class NotificationClientImpl extends ClientBase implements NotificationClient {

    public NotificationClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Notifications");
    }

    public List<? extends Notification> searchIbx(NotificationSearchRequest request) {
        NotificationSearchResponseJson response = postAs("SearchIbxNotifications", request, NotificationSearchResponseJson.class);
        return response.getData();
    }

    public List<? extends Notification> searchNetwork(NotificationSearchRequest request) {
        NotificationSearchResponseJson response = postAs("SearchNetworkNotifications", request, NotificationSearchResponseJson.class);
        return response.getData();
    }

    public NotificationJson getIbxById(String id) {
        return getAs("GetIbxNotification", Map.of("id", id), null, NotificationJson.class);
    }

    public NotificationJson getNetworkById(String id) {
        return getAs("GetNetworkNotification", Map.of("id", id), null, NotificationJson.class);
    }
}
