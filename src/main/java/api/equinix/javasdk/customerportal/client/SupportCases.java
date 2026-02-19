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
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseOperator;

/**
 * Client interface for managing support cases in the Equinix Customer Portal.
 * Provides operations to list, retrieve, and create support cases for requesting
 * assistance from Equinix support teams.
 */
public interface SupportCases {

    /**
     * Lists all support cases for the current account.
     *
     * @return a paginated list of support cases
     */
    PaginatedList<SupportCase> list();

    /**
     * Retrieves a specific support case by its unique identifier.
     *
     * @param uuid the unique identifier of the support case
     * @return the matching support case
     */
    SupportCase getByUuid(String uuid);

    /**
     * Returns a builder for defining a new support case.
     *
     * @return a new SupportCaseBuilder instance
     */
    SupportCaseOperator.SupportCaseBuilder define();
}
