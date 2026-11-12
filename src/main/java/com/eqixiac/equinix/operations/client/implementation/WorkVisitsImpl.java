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

package com.eqixiac.equinix.operations.client.implementation;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.operations.client.WorkVisits;
import com.eqixiac.equinix.operations.client.internal.WorkVisitClient;
import com.eqixiac.equinix.operations.model.OrderResponse;
import com.eqixiac.equinix.operations.model.LookupLocation;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitOrderRequest;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitUpdateRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkVisitsImpl implements WorkVisits {

    private final WorkVisitClient serviceClient;

    private final Operations serviceManager;

    public OrderResponse order(WorkVisitOrderRequest request) {
        return this.serviceClient.order(request);
    }

    public OrderResponse update(String orderId, WorkVisitUpdateRequest request) {
        return this.serviceClient.update(orderId, request);
    }

    /** Lookup v2 permission code for work visits. */
    static final String WORK_VISIT_PERMISSION = "WORK_VISIT";

    public List<? extends LookupLocation> listLocations() {
        return this.serviceManager.lookups().listLocations(WORK_VISIT_PERMISSION);
    }

    public List<? extends LookupLocation> listLocations(List<String> ibxs, Boolean details) {
        return this.serviceManager.lookups().listLocations(WORK_VISIT_PERMISSION, ibxs, null, null, null, details);
    }
}
