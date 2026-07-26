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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.client.SupportPlans;
import com.eqixiac.equinix.customerportal.client.internal.SupportPlanClient;
import com.eqixiac.equinix.customerportal.model.SupportPlan;
import com.eqixiac.equinix.customerportal.model.json.SupportPlanJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SupportPlansImpl implements SupportPlans {

    private final SupportPlanClient<SupportPlan> serviceClient;

    private final CustomerPortal serviceManager;

    public PaginatedList<SupportPlan> list() {
        return list(null, null, null, null);
    }

    public PaginatedList<SupportPlan> list(List<String> accountNumbers, List<String> ibxs, List<String> planIds) {
        return list(accountNumbers, ibxs, planIds, null);
    }

    public PaginatedList<SupportPlan> list(List<String> accountNumbers, List<String> ibxs, List<String> planIds,
                                           List<String> sorts) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (accountNumbers != null && !accountNumbers.isEmpty()) {
            queryParams.put("accountNumbers", accountNumbers);
        }
        if (ibxs != null && !ibxs.isEmpty()) {
            queryParams.put("ibxs", ibxs);
        }
        if (planIds != null && !planIds.isEmpty()) {
            queryParams.put("planIds", planIds);
        }
        if (sorts != null && !sorts.isEmpty()) {
            queryParams.put("sorts", sorts);
        }
        Page<SupportPlanJson> responsePage = this.serviceClient.list(queryParams);
        PaginatedList<SupportPlan> supportPlanList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(supportPlanList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
