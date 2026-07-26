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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Request body for adding a note to an existing trouble ticket / case. The {@code text} of the
 * note is required; supporting {@code attachments} reference previously uploaded files by id.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportCaseNoteRequest {

    @JsonProperty("text")
    private final String text;

    @JsonProperty("attachments")
    private final List<SupportCaseAttachment> attachments;

    public SupportCaseNoteRequest(String text) {
        this(text, null);
    }

    public SupportCaseNoteRequest(String text, List<SupportCaseAttachment> attachments) {
        this.text = text;
        this.attachments = attachments;
    }
}
