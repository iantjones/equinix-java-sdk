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

package com.eqixiac.equinix.customerportal.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for an Attachments v1 {@code attachment} ({@code GET /v1/attachments/{attachmentId}},
 * {@code GET /v1/attachments}, {@code POST /v1/attachments/file}).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentJson {

    @Getter static TypeReference<List<AttachmentJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("attachmentId")
    private String attachmentId;

    @JsonProperty("attachmentName")
    private String attachmentName;

    @JsonProperty("attachmentType")
    private String attachmentType;

    @JsonProperty("attachmentSize")
    private Long attachmentSize;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("lastUpdatedDate")
    private String lastUpdatedDate;
}
