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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.ServiceProfileOperator;

public interface ServiceProfiles {

    PaginatedList<ServiceProfile> list();

    PaginatedFilteredList<ServiceProfile> search();

    PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter);

    PaginatedFilteredList<ServiceProfile> search(SortPropertyList sort);

    PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter, SortPropertyList sort);

    ServiceProfile getByUuid(String uuid);

    ServiceProfileOperator.ServiceProfileBuilder define(ServiceProfileType serviceProfileType);
}
