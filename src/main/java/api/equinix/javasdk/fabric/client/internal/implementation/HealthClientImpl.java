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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.HealthClient;
import api.equinix.javasdk.fabric.model.HealthStatus;
import api.equinix.javasdk.fabric.model.json.HealthStatusJson;

public class HealthClientImpl extends PageableBase implements HealthClient<HealthStatus> {

    public HealthClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Health");
    }

    public HealthStatusJson getHealth() {
        EquinixRequest<HealthStatus> equinixRequest = this.buildRequest("GetHealth", RequestType.SINGLE, HealthStatusJson.getSingleTypeRef());
        EquinixResponse<HealthStatus> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<HealthStatus> nextPage(PaginatedRequest<HealthStatus> equinixRequest) {
        throw new UnsupportedOperationException("Health endpoint does not support pagination.");
    }
}
