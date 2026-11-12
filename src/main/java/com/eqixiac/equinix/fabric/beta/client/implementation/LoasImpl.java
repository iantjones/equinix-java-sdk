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

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.client.Loas;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaResourceClient;
import com.eqixiac.equinix.fabric.beta.model.CreateLoa;
import com.eqixiac.equinix.fabric.beta.model.CreateLoaNote;
import com.eqixiac.equinix.fabric.beta.model.LoaActionRequest;
import com.eqixiac.equinix.fabric.beta.model.LoaActionResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaConsumer;
import com.eqixiac.equinix.fabric.beta.model.LoaConsumersResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaNoteDetails;
import com.eqixiac.equinix.fabric.beta.model.LoaNotesResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaResponse;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link Loas} over the shared {@code FabricBeta} apiParams area.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public class LoasImpl implements Loas {

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public LoasImpl(BetaClientFactory clients) {
        this.clients = clients;
    }

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return clients.client(requestParent, type);
    }

    @Override
    public LoaResponse create(CreateLoa request) {
        return client("Loas", LoaResponse.class).one("CreateLoa", Map.of(), request);
    }

    @Override
    public LoaResponse getByUuid(String loaId) {
        return client("Loas", LoaResponse.class).one("GetLoaByUuid", Map.of("loaId", loaId), null);
    }

    @Override
    public LoaResponse update(String loaId, List<PatchOperation> operations) {
        return client("Loas", LoaResponse.class).patch("UpdateLoaByUuid", Map.of("loaId", loaId), operations, true);
    }

    @Override
    public LoaActionResponse performAction(String loaId, LoaActionRequest request) {
        return client("Loas", LoaActionResponse.class).one("PerformLoaAction", Map.of("loaId", loaId), request);
    }

    @Override
    public LoaActionResponse getAction(String loaId, String actionId) {
        return client("Loas", LoaActionResponse.class).one("GetLoaActionsByUuid", Map.of("loaId", loaId, "actionId", actionId), null);
    }

    @Override
    public PaginatedFilteredList<LoaResponse> search(FilterPropertyList filter, SortPropertyList sort) {
        return client("Loas", LoaResponse.class).search("SearchLoa", Map.of(), filter, sort);
    }

    @Override
    public PaginatedFilteredList<LoaResponse> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    @Override
    public PaginatedFilteredList<LoaResponse> search() {
        return search(null, null);
    }

    @Override
    public PaginatedFilteredList<LoaActionResponse> searchActions(String loaId, FilterPropertyList filter, SortPropertyList sort) {
        return client("Loas", LoaActionResponse.class).search("SearchLoaAction", Map.of("loaId", loaId), filter, sort);
    }

    @Override
    public PaginatedFilteredList<LoaActionResponse> searchActions(String loaId, FilterPropertyList filter) {
        return searchActions(loaId, filter, null);
    }

    @Override
    public PaginatedFilteredList<LoaActionResponse> searchActions(String loaId) {
        return searchActions(loaId, null, null);
    }

    @Override
    public List<LoaConsumer> getConsumers(String loaId) {
        LoaConsumersResponse envelope = client("Loas", LoaConsumersResponse.class).one("GetLoaConsumersByLoaId", Map.of("loaId", loaId), null);
        return envelope == null || envelope.getData() == null ? List.of() : List.copyOf(envelope.getData());
    }

    @Override
    public List<LoaNoteDetails> getNotes(String loaId) {
        LoaNotesResponse envelope = client("Loas", LoaNotesResponse.class).one("GetLoaNotesByUuid", Map.of("loaId", loaId), null);
        return envelope == null || envelope.getData() == null ? List.of() : List.copyOf(envelope.getData());
    }

    @Override
    public LoaNoteDetails createNote(String loaId, CreateLoaNote request) {
        return client("Loas", LoaNoteDetails.class).one("CreateLoaNoteByLoaId", Map.of("loaId", loaId), request);
    }
}
