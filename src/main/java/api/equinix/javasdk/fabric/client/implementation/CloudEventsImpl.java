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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.CloudEvents;
import api.equinix.javasdk.fabric.client.internal.CloudEventClient;
import api.equinix.javasdk.fabric.model.CloudEvent;
import api.equinix.javasdk.fabric.model.json.CloudEventJson;

public class CloudEventsImpl implements CloudEvents {

    private final Fabric serviceManager;

    private final CloudEventClient<CloudEvent> serviceClient;

    public CloudEventsImpl(CloudEventClient<CloudEvent> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<CloudEvent> list() {
        Page<CloudEvent, CloudEventJson> responsePage = this.serviceClient.list();
        PaginatedList<CloudEvent> cloudEventList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(cloudEventList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
