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
import com.eqixiac.equinix.fabric.model.RouteAggregationRule;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleCreatorJson;

import java.util.List;

public interface RouteAggregationRuleClient<T> extends PageablePost<T> {

    Page<RouteAggregationRuleJson> list(String routeAggregationId);

    RouteAggregationRuleJson getByUuid(String routeAggregationId, String uuid);

    RouteAggregationRuleJson create(String routeAggregationId, RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson);

    RouteAggregationRuleJson update(String routeAggregationId, String uuid, List<PatchOperation> operations);

    RouteAggregationRuleJson delete(String routeAggregationId, String uuid);

    RouteAggregationRuleJson refresh(String routeAggregationId, String uuid);

    RouteAggregationRuleJson replace(String routeAggregationId, String uuid, RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson);

    List<RouteAggregationRuleJson> createBulk(String routeAggregationId, List<RouteAggregationRuleCreatorJson> routeAggregationRuleCreatorJsonList);

    Page<RouteAggregationRuleJson> search(String routeAggregationId, FilterPropertyList filter, SortPropertyList sort);

    List<Change> getChanges(String routeAggregationId, String uuid);

    Change getChange(String routeAggregationId, String uuid, String changeId);
}
