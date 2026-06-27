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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.TroubleTickets;
import api.equinix.javasdk.customerportal.client.internal.TroubleTicketClient;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketJson;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketOperator;
import api.equinix.javasdk.customerportal.model.wrappers.TroubleTicketWrapper;

public class TroubleTicketsImpl implements TroubleTickets {

    private final CustomerPortal serviceManager;

    private final TroubleTicketClient<TroubleTicket> serviceClient;

    public TroubleTicketsImpl(TroubleTicketClient<TroubleTicket> serviceClient, CustomerPortal serviceManager) {
        this.serviceClient = serviceClient;
        this.serviceManager = serviceManager;
    }

    public PaginatedList<TroubleTicket> list() {
        Page<TroubleTicket, TroubleTicketJson> responsePage = this.serviceClient.list();
        PaginatedList<TroubleTicket> troubleTicketList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, TroubleTicketWrapper::new);
        return new PaginatedList<>(troubleTicketList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public TroubleTicket getByUuid(String uuid) {
        TroubleTicketJson troubleTicketJson = this.serviceClient.getByUuid(uuid);
        return new TroubleTicketWrapper(troubleTicketJson, this.serviceClient);
    }

    public TroubleTicketOperator.TroubleTicketBuilder define() {
        return new TroubleTicketOperator(this.serviceClient).create();
    }
}
