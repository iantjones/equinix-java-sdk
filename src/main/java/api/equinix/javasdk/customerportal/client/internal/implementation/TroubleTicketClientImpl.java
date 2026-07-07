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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.SerializationHelper;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.TroubleTicketClient;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketJson;
import api.equinix.javasdk.customerportal.model.json.creators.TicketCancelRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TicketNoteRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TicketUpdateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketCreateRequest;
import api.equinix.javasdk.customerportal.model.wrappers.TroubleTicketWrapper;

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
