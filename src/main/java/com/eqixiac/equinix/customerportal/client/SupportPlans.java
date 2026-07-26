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

package com.eqixiac.equinix.customerportal.client;

import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.model.SupportPlan;

import java.util.List;

/**
 * Client interface for accessing Smart Hands support plans in the Equinix Customer Portal.
 *
 * <p>Backed by the Support Plans v2 API at {@code /colocations/v2/supportPlans}. Support plans are
 * listed (optionally filtered by account numbers, IBXs and plan ids); there is no per-id retrieval
 * endpoint.</p>
 */
public interface SupportPlans {

    /**
     * Lists all support plans for the current account.
     *
     * <p>Maps to {@code GET /colocations/v2/supportPlans} ({@code Retrieve Smart Hands support plans}).</p>
     *
     * @return a paginated list of support plans
     */
    PaginatedList<SupportPlan> list();

    /**
     * Lists support plans filtered by account numbers, IBXs and/or plan ids.
     *
     * <p>Maps to {@code GET /colocations/v2/supportPlans} ({@code Retrieve Smart Hands support plans}).</p>
     *
     * @param accountNumbers the account numbers to filter by, or {@code null}
     * @param ibxs           the IBX codes to filter by, or {@code null}
     * @param planIds        the plan ids to filter by, or {@code null}
     * @return a paginated list of matching support plans
     */
    PaginatedList<SupportPlan> list(List<String> accountNumbers, List<String> ibxs, List<String> planIds);

    /**
     * Lists support plans filtered by account numbers, IBXs and/or plan ids, sorted by the supplied
     * sort specifiers.
     *
     * <p>Maps to {@code GET /colocations/v2/supportPlans} ({@code Retrieve Smart Hands support plans})
     * with the {@code sorts} query parameter.</p>
     *
     * @param accountNumbers the account numbers to filter by, or {@code null}
     * @param ibxs           the IBX codes to filter by, or {@code null}
     * @param planIds        the plan ids to filter by, or {@code null}
     * @param sorts          the sort specifiers, or {@code null}
     * @return a paginated list of matching support plans
     */
    PaginatedList<SupportPlan> list(List<String> accountNumbers, List<String> ibxs, List<String> planIds,
                                    List<String> sorts);
}
