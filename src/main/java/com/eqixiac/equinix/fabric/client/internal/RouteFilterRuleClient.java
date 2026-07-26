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
import com.eqixiac.equinix.fabric.model.RouteFilterRule;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteFilterRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteFilterRuleCreatorJson;

import java.util.List;

public interface RouteFilterRuleClient<T> extends PageablePost<T> {

    Page<RouteFilterRuleJson> list(String routeFilterId);

    RouteFilterRuleJson getByUuid(String routeFilterId, String uuid);

    RouteFilterRuleJson create(String routeFilterId, RouteFilterRuleCreatorJson routeFilterRuleCreatorJson);

    RouteFilterRuleJson update(String routeFilterId, String uuid, List<PatchOperation> operations);

    RouteFilterRuleJson delete(String routeFilterId, String uuid);

    RouteFilterRuleJson refresh(String routeFilterId, String uuid);

    RouteFilterRuleJson replace(String routeFilterId, String uuid, RouteFilterRuleCreatorJson routeFilterRuleCreatorJson);

    List<RouteFilterRuleJson> createBulk(String routeFilterId, List<RouteFilterRuleCreatorJson> routeFilterRuleCreatorJsonList);

    Page<RouteFilterRuleJson> search(String routeFilterId, FilterPropertyList filter, SortPropertyList sort);

    List<Change> getChanges(String routeFilterId, String uuid);

    Change getChange(String routeFilterId, String uuid, String changeId);
}
