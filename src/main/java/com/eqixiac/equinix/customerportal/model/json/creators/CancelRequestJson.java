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
 * Request body for cancelling an order. A {@code reason} is required; {@code attachments}
 * and {@code lineIds} (to cancel specific order lines) are optional.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelRequestJson {

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("attachments")
    private final List<AttachmentReference> attachments;

    @JsonProperty("lineIds")
    private final List<String> lineIds;

    public CancelRequestJson(String reason, List<AttachmentReference> attachments, List<String> lineIds) {
        this.reason = reason;
        this.attachments = attachments;
        this.lineIds = lineIds;
    }
}
