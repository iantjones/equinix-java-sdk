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
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.TroubleTicketClient;
import com.eqixiac.equinix.customerportal.model.TroubleTicket;
import com.eqixiac.equinix.customerportal.model.json.TroubleTicketJson;
import com.eqixiac.equinix.customerportal.model.json.creators.TicketCancelRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.TicketNoteRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.TicketUpdateRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.TroubleTicketCreateRequest;
import com.eqixiac.equinix.customerportal.model.wrappers.TroubleTicketWrapper;

import java.util.Map;

public class TroubleTicketClientImpl extends ResourceClientBase<TroubleTicket, TroubleTicketJson> implements TroubleTicketClient<TroubleTicket> {

    public TroubleTicketClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "TroubleTickets", TroubleTicketJson.class);
    }

    @Override
    protected TroubleTicket wrap(TroubleTicketJson json) {
        return new TroubleTicketWrapper(json, this);
    }

    public String create(TroubleTicketCreateRequest request) {
        EquinixRequest<Object> equinixRequest = buildRequest("CreateTroubleTicket", RequestType.SINGLE, Object.class);
        SerializationHelper.serializeJson(equinixRequest, request);
        return ResponseHandler.extractFromHeader(invoke(equinixRequest), "Location", OrderLocation.LAST_SEGMENT_PATTERN);
    }

    public TroubleTicketJson getByUuid(String id) {
        return getOne("GetTroubleTicket", Map.of("id", id));
    }

    public Boolean update(String id, TicketUpdateRequest request) {
        return booleanOp("UpdateTroubleTicket", RequestType.SINGLE, Map.of("id", id), null, request);
    }

    public Boolean addNote(String id, TicketNoteRequest request) {
        return booleanOp("AddTroubleTicketNote", RequestType.SINGLE, Map.of("id", id), null, request);
    }

    public Boolean cancel(String id, TicketCancelRequest request) {
        return booleanOp("CancelTroubleTicket", RequestType.SINGLE, Map.of("id", id), null, request);
    }

    public TroubleTicketJson refresh(String id) {
        return this.getByUuid(id);
    }
}
