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
 * Request body for adding a note to an order. The {@code text} of the note is required;
 * {@code referenceId} (for two-way notes) and {@code attachments} are optional.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoteRequestJson {

    @JsonProperty("text")
    private final String text;

    @JsonProperty("referenceId")
    private final String referenceId;

    @JsonProperty("attachments")
    private final List<AttachmentReference> attachments;

    public NoteRequestJson(String text, String referenceId, List<AttachmentReference> attachments) {
        this.text = text;
        this.referenceId = referenceId;
        this.attachments = attachments;
    }
}
