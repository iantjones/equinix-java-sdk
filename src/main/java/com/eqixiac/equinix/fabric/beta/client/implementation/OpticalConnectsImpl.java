/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.client.implementation;

import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.client.OpticalConnects;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaResourceClient;
import com.eqixiac.equinix.fabric.beta.model.BulkOpticalConnectRequest;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectBulk;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectPostRequest;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectResponse;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link OpticalConnects} over the shared {@code FabricBeta} apiParams area.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public class OpticalConnectsImpl implements OpticalConnects {

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public OpticalConnectsImpl(BetaClientFactory clients) {
        this.clients = clients;
    }

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return clients.client(requestParent, type);
    }

    @Override
    public OpticalConnectResponse create(OpticalConnectPostRequest request) {
        return client("OpticalConnects", OpticalConnectResponse.class).one("CreateOpticalConnect", Map.of(), request);
    }

    @Override
    public List<OpticalConnectResponse> createDiverse(BulkOpticalConnectRequest request) {
        OpticalConnectBulk envelope = client("OpticalConnects", OpticalConnectBulk.class).one("CreateBulkOpticalConnect", Map.of(), request);
        return envelope == null || envelope.getData() == null ? List.of() : List.copyOf(envelope.getData());
    }

    @Override
    public OpticalConnectResponse getByUuid(String opticalConnectId) {
        return client("OpticalConnects", OpticalConnectResponse.class).one("GetOpticalConnectByUuid", Map.of("opticalConnectId", opticalConnectId), null);
    }

    @Override
    public PaginatedFilteredList<OpticalConnectResponse> search(FilterPropertyList filter, SortPropertyList sort) {
        return client("OpticalConnects", OpticalConnectResponse.class).search("SearchOpticalConnect", Map.of(), filter, sort);
    }

    @Override
    public PaginatedFilteredList<OpticalConnectResponse> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    @Override
    public PaginatedFilteredList<OpticalConnectResponse> search() {
        return search(null, null);
    }
}
