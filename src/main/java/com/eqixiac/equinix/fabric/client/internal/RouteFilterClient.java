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
import com.eqixiac.equinix.fabric.model.RouteFilter;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteFilterJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteFilterCreatorJson;

import java.util.List;

public interface RouteFilterClient<T> extends PageablePost<T> {

    Page<RouteFilterJson> search(FilterPropertyList filter, SortPropertyList sort);

    RouteFilterJson getByUuid(String uuid);

    RouteFilterJson create(RouteFilterCreatorJson routeFilterCreatorJson);

    RouteFilterJson update(String uuid, List<PatchOperation> operations);

    RouteFilterJson delete(String uuid);

    RouteFilterJson refresh(String uuid);

    List<Change> getChanges(String uuid);

    Change getChange(String uuid, String changeId);

    List<Connection> getConnections(String uuid);
}
