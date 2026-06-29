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

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.CloudEvents;
import api.equinix.javasdk.fabric.client.internal.CloudEventClient;
import api.equinix.javasdk.fabric.model.CloudEvent;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CloudEventJson;

import java.util.List;

public class CloudEventsImpl implements CloudEvents {

    private final CloudEventClient<CloudEvent> serviceClient;

    public CloudEventsImpl(CloudEventClient<CloudEvent> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public PaginatedFilteredList<CloudEvent> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<CloudEvent> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<CloudEvent> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<CloudEvent> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<CloudEvent, CloudEventJson> responsePage = this.serviceClient.search(filter, sort);
        PaginatedFilteredList<CloudEvent> cloudEventList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedFilteredList<>(cloudEventList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public CloudEvent getByUuid(String uuid) {
        return this.serviceClient.getByUuid(uuid);
    }

    public List<CloudEvent> getByAssetId(String asset, String assetId) {
        return this.serviceClient.getByAssetId(asset, assetId);
    }
}
