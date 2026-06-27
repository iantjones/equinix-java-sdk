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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitOperator;

/**
 * Client interface for managing work visit requests in the Equinix Customer Portal.
 * Provides operations to list, retrieve, and create work visit scheduling requests
 * for on-site access to IBX data centers.
 */
public interface WorkVisits {

    /**
     * Lists all work visits for the current account.
     *
     * @return a paginated list of work visits
     */
    PaginatedList<WorkVisit> list();

    /**
     * Retrieves a specific work visit by its unique identifier.
     *
     * @param uuid the unique identifier of the work visit
     * @return the matching work visit
     */
    WorkVisit getByUuid(String uuid);

    /**
     * Returns a builder for defining a new work visit request.
     *
     * @return a new WorkVisitBuilder instance
     */
    WorkVisitOperator.WorkVisitBuilder define();
}
