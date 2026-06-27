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

import api.equinix.javasdk.customerportal.enums.TicketCategory;
import api.equinix.javasdk.customerportal.enums.TicketPriority;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TroubleTicketCreatorJson {

    @JsonProperty("category") private TicketCategory category;
    @JsonProperty("priority") private TicketPriority priority;
    @JsonProperty("subject") private String subject;
    @JsonProperty("description") private String description;
    @JsonProperty("ibxCode") private String ibxCode;
    @JsonProperty("accountNumber") private String accountNumber;
    @JsonProperty("requestorName") private String requestorName;
    @JsonProperty("requestorEmail") private String requestorEmail;

    public TroubleTicketCreatorJson(TroubleTicketOperator.TroubleTicketBuilder builder) {
        this.category = builder.getCategory();
        this.priority = builder.getPriority();
        this.subject = builder.getSubject();
        this.description = builder.getDescription();
        this.ibxCode = builder.getIbxCode();
        this.accountNumber = builder.getAccountNumber();
        this.requestorName = builder.getRequestorName();
        this.requestorEmail = builder.getRequestorEmail();
    }
}
