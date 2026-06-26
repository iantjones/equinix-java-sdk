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
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.TroubleTicketClient;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketJson;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.TroubleTicketWrapper;

public class TroubleTicketClientImpl extends ResourceClientBase<TroubleTicket, TroubleTicketJson> implements TroubleTicketClient<TroubleTicket> {

    public TroubleTicketClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "TroubleTickets", TroubleTicketJson.class);
    }

    @Override
    protected TroubleTicket wrap(TroubleTicketJson json) {
        return new TroubleTicketWrapper(json, this);
    }

    public Page<TroubleTicket, TroubleTicketJson> list() {
        return listPage("ListTroubleTickets");
    }

    public TroubleTicketJson getByUuid(String uuid) {
        return getOne("GetTroubleTicket", uuid);
    }

    public TroubleTicketJson create(TroubleTicketCreatorJson troubleTicketCreatorJson) {
        return postOne("CreateTroubleTicket", troubleTicketCreatorJson);
    }

    public TroubleTicketJson update(String uuid, TroubleTicketCreatorJson troubleTicketCreatorJson) {
        return updateOne("UpdateTroubleTicket", uuid, troubleTicketCreatorJson);
    }

    public TroubleTicketJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
