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

package com.eqixiac.equinix.fabric.client.internal;

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RouteAggregation;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationCreatorJson;

import java.util.List;

public interface RouteAggregationClient<T> extends PageablePost<T> {

    Page<RouteAggregationJson> search(FilterPropertyList filter, SortPropertyList sort);

    RouteAggregationJson getByUuid(String uuid);

    RouteAggregationJson create(RouteAggregationCreatorJson routeAggregationCreatorJson);

    RouteAggregationJson update(String uuid, List<PatchOperation> operations);

    RouteAggregationJson delete(String uuid);

    RouteAggregationJson refresh(String uuid);

    List<Change> getChanges(String uuid);

    Change getChange(String uuid, String changeId);

    List<Connection> getConnections(String uuid);
}
