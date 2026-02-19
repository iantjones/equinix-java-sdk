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
import api.equinix.javasdk.customerportal.model.Report;

/**
 * Client interface for accessing reports in the Equinix Customer Portal.
 * Provides read-only operations to list and retrieve generated reports
 * covering usage, billing, and operational metrics.
 */
public interface Reports {

    /**
     * Lists all reports for the current account.
     *
     * @return a paginated list of reports
     */
    PaginatedList<Report> list();

    /**
     * Retrieves a specific report by its unique identifier.
     *
     * @param uuid the unique identifier of the report
     * @return the matching report
     */
    Report getByUuid(String uuid);
}
