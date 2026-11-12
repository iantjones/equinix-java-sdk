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
import com.eqixiac.equinix.fabric.model.implementation.filter.CloudEventQuery;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.CloudEventJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * Default {@link CloudEvents} implementation.
 */
@RequiredArgsConstructor
public class CloudEventsImpl implements CloudEvents {

    private final CloudEventClient<CloudEvent> serviceClient;

    @Override
    public PaginatedFilteredList<CloudEvent> search(CloudEventQuery query) {
        return search(query, null);
    }

    @Override
    public PaginatedFilteredList<CloudEvent> search(CloudEventQuery query, SortPropertyList sort) {
        Objects.requireNonNull(query, "query");
        return search(query.toFilter(), sort);
    }

    @Override
    public PaginatedFilteredList<CloudEvent> search(FilterPropertyList filter, SortPropertyList sort) {
        CloudEventQuery.validate(filter);
        Page<CloudEventJson> responsePage = this.serviceClient.search(filter, sort);
        return ResponseHandler.toPaginatedFilteredList(responsePage, this.serviceClient, (json, client) -> json);
    }

    @Override
    public CloudEvent getByUuid(String uuid) {
        return this.serviceClient.getByUuid(uuid);
    }

    @Override
    public PaginatedFilteredList<CloudEvent> searchLiveEvents(String... subjects) {
        Objects.requireNonNull(subjects, "subjects");
        List<Object> values = new ArrayList<>();
        for (String s : subjects) {
            if (s == null || s.isBlank()) {
                throw new IllegalArgumentException("subject is required");
            }
            if (s.contains("*")) {
                throw new IllegalArgumentException("live events take exact subjects, not wildcards: " + s);
            }
            values.add(s);
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("at least one subject is required");
        }
        Page<CloudEventJson> responsePage = this.serviceClient.searchLiveEvents(Filter.filter().and().in(CloudEventQuery.SUBJECT, values));
        return ResponseHandler.toPaginatedFilteredList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
