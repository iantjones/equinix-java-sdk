/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.model;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Action Data. &lt;br&gt; notes and attachments are applicable for XC_INFORMATION_UPDATE and XC_DEINSTALL. &lt;br&gt; schedule, services, liveTrafficConsentProvided, notifications and order are applicable for XC_DEINSTALL. &lt;br&gt; Each schedule, service, note and attachment carries the href and the uuid of the sub-resource it refers to. &lt;br&gt; Not applicable to the other action types.
 *
 * <p>Maps spec schema {@code CrossConnectActionResponseData}. Unknown properties are ignored on read.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class CrossConnectActionResponseData {

    /** Applicable for XC_INFORMATION_UPDATE and XC_DEINSTALL. Read-only (server-populated). */
    @JsonProperty("notes")
    private List<CrossConnectActionNoteResponse> notes;

    /** Applicable for XC_INFORMATION_UPDATE and XC_DEINSTALL. Read-only (server-populated). */
    @JsonProperty("attachments")
    private List<CrossConnectActionAttachmentResponse> attachments;

    /** Applicable for XC_DEINSTALL. Read-only (server-populated). */
    @JsonProperty("schedule")
    private CrossConnectScheduleResponse schedule;

    /** Applicable for XC_DEINSTALL. Confirms the customer consents to the removal even though the Cross Connect may still be carrying live traffic. Read-only (server-populated). */
    @JsonProperty("liveTrafficConsentProvided")
    private Boolean liveTrafficConsentProvided;

    /** Applicable for XC_DEINSTALL. Only XC_PATCH_EQUIPMENT is supported for deinstall. Read-only (server-populated). */
    @JsonProperty("services")
    private List<CrossConnectServiceResponse> services;

    /** Applicable for XC_DEINSTALL. The contacts are appended to the ones already held on the Cross Connect. Read-only (server-populated). */
    @JsonProperty("notifications")
    private List<CrossConnectNotification> notifications;

    /** Applicable for XC_DEINSTALL. The purchase order and the customer reference identifier replace the ones already held on the Cross Connect. Read-only (server-populated). */
    @JsonProperty("order")
    private OrderResponse order;
}
