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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.TroubleTicketClientImpl;
import api.equinix.javasdk.customerportal.enums.TicketCategory;
import api.equinix.javasdk.customerportal.enums.TicketPriority;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketJson;
import api.equinix.javasdk.customerportal.model.wrappers.TroubleTicketWrapper;
import lombok.Getter;

public class TroubleTicketOperator extends ResourceImpl<TroubleTicket> {

    @Getter
    private final Pageable<TroubleTicket> serviceClient;

    public TroubleTicketOperator(Pageable<TroubleTicket> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public TroubleTicketBuilder create() {
        return new TroubleTicketBuilder();
    }

    @Getter
    public class TroubleTicketBuilder {
        private TicketCategory category;
        private TicketPriority priority;
        private String subject;
        private String description;
        private String ibxCode;
        private String accountNumber;
        private String requestorName;
        private String requestorEmail;

        public TroubleTicketBuilder category(TicketCategory category) {
            this.category = category;
            return this;
        }

        public TroubleTicketBuilder priority(TicketPriority priority) {
            this.priority = priority;
            return this;
        }

        public TroubleTicketBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public TroubleTicketBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TroubleTicketBuilder ibxCode(String ibxCode) {
            this.ibxCode = ibxCode;
            return this;
        }

        public TroubleTicketBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public TroubleTicketBuilder requestorName(String requestorName) {
            this.requestorName = requestorName;
            return this;
        }

        public TroubleTicketBuilder requestorEmail(String requestorEmail) {
            this.requestorEmail = requestorEmail;
            return this;
        }

        public TroubleTicket create() {
            TroubleTicketCreatorJson creatorJson = new TroubleTicketCreatorJson(this);
            TroubleTicketJson troubleTicketJson = ((TroubleTicketClientImpl) TroubleTicketOperator.this.getServiceClient()).create(creatorJson);
            return new TroubleTicketWrapper(troubleTicketJson, TroubleTicketOperator.this.getServiceClient());
        }
    }
}
