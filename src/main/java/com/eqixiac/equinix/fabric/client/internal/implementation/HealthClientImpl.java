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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.http.request.PaginatedRequest;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.HealthClient;
import com.eqixiac.equinix.fabric.model.HealthStatus;
import com.eqixiac.equinix.fabric.model.json.HealthStatusJson;

public class HealthClientImpl extends ClientBase implements HealthClient<HealthStatus> {

    public HealthClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Health");
    }

    public HealthStatusJson getHealth() {
        return getAs("GetHealth", HealthStatusJson.class);
    }

    public PaginatedList<HealthStatus> nextPage(PaginatedRequest<HealthStatus> equinixRequest) {
        throw new UnsupportedOperationException("Health endpoint does not support pagination.");
    }
}
