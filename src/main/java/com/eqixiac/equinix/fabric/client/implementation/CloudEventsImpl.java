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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.client.CloudEvents;
import com.eqixiac.equinix.fabric.client.internal.CloudEventClient;
import com.eqixiac.equinix.fabric.model.CloudEvent;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.CloudEventJson;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CloudEventsImpl implements CloudEvents {

    private final CloudEventClient<CloudEvent> serviceClient;

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
        Page<CloudEventJson> responsePage = this.serviceClient.search(filter, sort);
        PaginatedFilteredList<CloudEvent> cloudEventList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedFilteredList<>(cloudEventList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public CloudEvent getByUuid(String uuid) {
        return this.serviceClient.getByUuid(uuid);
    }

    public List<CloudEvent> getByAssetId(String asset, String assetId) {
        return this.serviceClient.getByAssetId(asset, assetId);
    }
}
