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

package com.eqixiac.equinix.customerportal.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.SupportPlanClient;
import com.eqixiac.equinix.customerportal.model.SupportPlan;
import com.eqixiac.equinix.customerportal.model.json.SupportPlanJson;

import java.util.List;
import java.util.Map;

public class SupportPlanClientImpl extends ResourceClientBase<SupportPlan, SupportPlanJson> implements SupportPlanClient<SupportPlan> {

    public SupportPlanClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "SupportPlans", SupportPlanJson.class);
    }

    @Override
    protected SupportPlan wrap(SupportPlanJson json) {
        return json;
    }

    public Page<SupportPlanJson> list(Map<String, List<String>> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return listPage("ListSupportPlans");
        }
        return listPage("ListSupportPlans", queryParams);
    }
}
