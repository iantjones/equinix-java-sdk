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

package api.equinix.javasdk.core.model;

import api.equinix.javasdk.core.http.request.Pagination;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilteredSortedPaginatedPost {

    private FilterPropertyList filter;

    private SortPropertyList sort;

    private Pagination pagination = new Pagination(Constants.PAGE_OFFSET, Constants.PAGE_LIMIT);

    public FilteredSortedPaginatedPost(FilterPropertyList filter, SortPropertyList sort) {
        this.filter = filter;
        this.sort = sort;
    }
}
