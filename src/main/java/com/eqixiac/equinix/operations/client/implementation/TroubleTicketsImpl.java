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
import com.eqixiac.equinix.operations.client.TroubleTickets;
import com.eqixiac.equinix.operations.client.internal.TroubleTicketClient;
import com.eqixiac.equinix.operations.model.TroubleTicket;
import com.eqixiac.equinix.operations.model.json.TroubleTicketJson;
import com.eqixiac.equinix.operations.model.json.creators.TicketCancelRequest;
import com.eqixiac.equinix.operations.model.json.creators.TicketNoteRequest;
import com.eqixiac.equinix.operations.model.json.creators.TicketUpdateRequest;
import com.eqixiac.equinix.operations.model.json.creators.TroubleTicketCreateRequest;
import com.eqixiac.equinix.operations.model.wrappers.TroubleTicketWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TroubleTicketsImpl implements TroubleTickets {

    private final TroubleTicketClient<TroubleTicket> serviceClient;

    private final Operations serviceManager;

    public String create(TroubleTicketCreateRequest request) {
        return this.serviceClient.create(request);
    }

    public TroubleTicket getByUuid(String id) {
        TroubleTicketJson troubleTicketJson = this.serviceClient.getByUuid(id);
        return new TroubleTicketWrapper(troubleTicketJson, this.serviceClient);
    }

    public Boolean update(String id, TicketUpdateRequest request) {
        return this.serviceClient.update(id, request);
    }

    public Boolean addNote(String id, TicketNoteRequest request) {
        return this.serviceClient.addNote(id, request);
    }

    public Boolean cancel(String id, TicketCancelRequest request) {
        return this.serviceClient.cancel(id, request);
    }
}
