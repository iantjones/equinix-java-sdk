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
 * Request body for adding a note to a trouble ticket
 * ({@code POST /v2/tickets/{id}/notes}, {@code NoteRequest}). {@code text} is required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketNoteRequest {

    @JsonProperty("text")
    private final String text;

    @JsonProperty("referenceId")
    private final String referenceId;

    @JsonProperty("attachments")
    private final List<OrderAttachment> attachments;

    public TicketNoteRequest(String text) {
        this(text, null, null);
    }

    public TicketNoteRequest(String text, String referenceId, List<OrderAttachment> attachments) {
        this.text = text;
        this.referenceId = referenceId;
        this.attachments = attachments;
    }
}
