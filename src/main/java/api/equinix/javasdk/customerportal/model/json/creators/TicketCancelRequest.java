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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Request body for cancelling a trouble ticket
 * ({@code POST /v2/tickets/{id}/cancel}, {@code Tickets_Cancel}). {@code reason} is required;
 * up to five previously uploaded {@code attachments} may be referenced.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketCancelRequest {

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("attachments")
    private final List<OrderAttachment> attachments;

    public TicketCancelRequest(String reason) {
        this(reason, null);
    }

    /**
     * Builds a cancel request with supporting attachments.
     *
     * @param reason      the cancellation reason (required)
     * @param attachments references to previously uploaded attachments (maximum 5)
     */
    public TicketCancelRequest(String reason, List<OrderAttachment> attachments) {
        this.reason = reason;
        this.attachments = attachments;
    }
}
