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
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.client.CrossConnects;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaResourceClient;
import com.eqixiac.equinix.fabric.beta.model.CreateCrossConnect;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectActionRequest;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectActionResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectAttachmentResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectAttachmentsResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectDiverse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectNoteResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectScheduleAvailability;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectScheduleResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectServiceResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectServicesResponse;
import com.eqixiac.equinix.fabric.beta.model.DiverseCrossConnectRequest;
import com.eqixiac.equinix.fabric.beta.model.SchedulesResponse;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link CrossConnects} over the shared {@code FabricBeta} apiParams area.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public class CrossConnectsImpl implements CrossConnects {

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public CrossConnectsImpl(BetaClientFactory clients) {
        this.clients = clients;
    }

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return clients.client(requestParent, type);
    }

    @Override
    public CrossConnectResponse create(CreateCrossConnect request) {
        return client("CrossConnects", CrossConnectResponse.class).one("CreateCrossConnect", Map.of(), request);
    }

    @Override
    public List<CrossConnectResponse> createDiverse(DiverseCrossConnectRequest request) {
        CrossConnectDiverse envelope = client("CrossConnects", CrossConnectDiverse.class).one("CreateDiverseCrossConnect", Map.of(), request);
        return envelope == null || envelope.getData() == null ? List.of() : List.copyOf(envelope.getData());
    }

    @Override
    public PaginatedFilteredList<CrossConnectResponse> search(FilterPropertyList filter, SortPropertyList sort) {
        return client("CrossConnects", CrossConnectResponse.class).search("SearchCrossConnects", Map.of(), filter, sort);
    }

    @Override
    public PaginatedFilteredList<CrossConnectResponse> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    @Override
    public PaginatedFilteredList<CrossConnectResponse> search() {
        return search(null, null);
    }

    @Override
    public CrossConnectResponse getByUuid(String crossConnectId) {
        return client("CrossConnects", CrossConnectResponse.class).one("GetCrossConnectByUuid", Map.of("crossConnectId", crossConnectId), null);
    }

    @Override
    public List<CrossConnectScheduleResponse> getSchedules(String crossConnectId) {
        SchedulesResponse envelope = client("CrossConnects", SchedulesResponse.class).one("GetCrossConnectSchedulesByUuid", Map.of("crossConnectId", crossConnectId), null);
        return envelope == null || envelope.getData() == null ? List.of() : List.copyOf(envelope.getData());
    }

    @Override
    public List<CrossConnectServiceResponse> getServices(String crossConnectId) {
        CrossConnectServicesResponse envelope = client("CrossConnects", CrossConnectServicesResponse.class).one("GetCrossConnectServicesByUuid", Map.of("crossConnectId", crossConnectId), null);
        return envelope == null || envelope.getData() == null ? List.of() : List.copyOf(envelope.getData());
    }

    @Override
    public List<CrossConnectAttachmentResponse> getAttachments(String crossConnectId) {
        CrossConnectAttachmentsResponse envelope = client("CrossConnects", CrossConnectAttachmentsResponse.class).one("GetCrossConnectAttachmentsByUuid", Map.of("crossConnectId", crossConnectId), null);
        return envelope == null || envelope.getData() == null ? List.of() : List.copyOf(envelope.getData());
    }

    @Override
    public PaginatedList<CrossConnectNoteResponse> listNotes(String crossConnectId) {
        return client("CrossConnects", CrossConnectNoteResponse.class).list("GetCrossConnectNotesByUuid", Map.of("crossConnectId", crossConnectId), null);
    }

    @Override
    public CrossConnectActionResponse performAction(String crossConnectId, CrossConnectActionRequest request) {
        return client("CrossConnects", CrossConnectActionResponse.class).one("PerformCrossConnectAction", Map.of("crossConnectId", crossConnectId), request);
    }

    @Override
    public CrossConnectActionResponse getAction(String crossConnectId, String actionId) {
        return client("CrossConnects", CrossConnectActionResponse.class).one("GetCrossConnectActionByUuid", Map.of("crossConnectId", crossConnectId, "actionId", actionId), null);
    }

    @Override
    public PaginatedFilteredList<CrossConnectActionResponse> searchActions(String crossConnectId, FilterPropertyList filter, SortPropertyList sort) {
        return client("CrossConnects", CrossConnectActionResponse.class).search("SearchCrossConnectAction", Map.of("crossConnectId", crossConnectId), filter, sort);
    }

    @Override
    public PaginatedFilteredList<CrossConnectActionResponse> searchActions(String crossConnectId, FilterPropertyList filter) {
        return searchActions(crossConnectId, filter, null);
    }

    @Override
    public PaginatedFilteredList<CrossConnectActionResponse> searchActions(String crossConnectId) {
        return searchActions(crossConnectId, null, null);
    }

    @Override
    public PaginatedFilteredList<CrossConnectScheduleAvailability> searchSchedules(FilterPropertyList filter, SortPropertyList sort) {
        return client("CrossConnectSchedules", CrossConnectScheduleAvailability.class).search("SearchCrossConnectSchedules", Map.of(), filter, sort);
    }

    @Override
    public PaginatedFilteredList<CrossConnectScheduleAvailability> searchSchedules(FilterPropertyList filter) {
        return searchSchedules(filter, null);
    }

    @Override
    public PaginatedFilteredList<CrossConnectScheduleAvailability> searchSchedules() {
        return searchSchedules(null, null);
    }
}
