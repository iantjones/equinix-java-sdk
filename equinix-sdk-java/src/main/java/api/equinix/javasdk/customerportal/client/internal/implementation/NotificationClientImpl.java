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
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationClientImpl extends ClientBase implements NotificationClient {

    public NotificationClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Notifications");
    }

    public List<? extends Notification> searchIbx(NotificationSearchRequest request, Integer offset, Integer limit) {
        return search("SearchIbxNotifications", request, offset, limit);
    }

    public List<? extends Notification> searchNetwork(NotificationSearchRequest request, Integer offset, Integer limit) {
        return search("SearchNetworkNotifications", request, offset, limit);
    }

    public NotificationJson getIbxById(String id) {
        return getAs("GetIbxNotification", Map.of("id", id), null, NotificationJson.class);
    }

    public NotificationJson getNetworkById(String id) {
        return getAs("GetNetworkNotification", Map.of("id", id), null, NotificationJson.class);
    }

    /**
     * Posts a notification search, forwarding {@code sorts} (from the request), {@code offset} and
     * {@code limit} as query parameters and reading the notifications from the response {@code data}
     * array.
     */
    private List<? extends Notification> search(String serviceEndpoint, NotificationSearchRequest request,
                                               Integer offset, Integer limit) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (request != null && request.getSorts() != null && !request.getSorts().isEmpty()) {
            queryParams.put("sorts", request.getSorts());
        }
        if (offset != null) {
            queryParams.put("offset", List.of(String.valueOf(offset)));
        }
        if (limit != null) {
            queryParams.put("limit", List.of(String.valueOf(limit)));
        }
        NotificationSearchResponseJson response = postForType(serviceEndpoint, null,
                queryParams.isEmpty() ? null : queryParams, request,
                new TypeReference<NotificationSearchResponseJson>() {});
        return response.getData();
    }
}
