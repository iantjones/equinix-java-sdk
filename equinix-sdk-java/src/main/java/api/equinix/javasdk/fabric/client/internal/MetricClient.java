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

package api.equinix.javasdk.fabric.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.MetricJson;

/**
 * <p>MetricClient interface. Internal client for the Fabric Metrics search API
 * ({@code POST /fabric/v4/metrics/search}).</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface MetricClient<T> extends PageablePost<T> {

    /**
     * <p>Searches metrics matching the supplied filter and sort criteria.</p>
     *
     * @param filter a {@link api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList} object.
     * @param sort a {@link api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList} object.
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<Metric, MetricJson> search(FilterPropertyList filter, SortPropertyList sort);
}
