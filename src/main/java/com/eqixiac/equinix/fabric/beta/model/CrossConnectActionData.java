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
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Action Data. &lt;br&gt; notes and attachments are applicable for XC_INFORMATION_UPDATE and XC_DEINSTALL. &lt;br&gt; schedule is required for XC_DEINSTALL, where liveTrafficConsentProvided, services, notifications and order are applicable. &lt;br&gt; Not applicable to the other action types.
 *
 * <p>Maps spec schema {@code CrossConnectActionData}. Unknown properties are ignored on read.</p>
 * <p>Request-capable: every property has a chained setter ({@code setX(...)} returns this), so an
 * instance doubles as its own builder.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class CrossConnectActionData {

    /** Applicable for XC_INFORMATION_UPDATE and XC_DEINSTALL. */
    @JsonProperty("notes")
    private List<CrossConnectActionNote> notes;

    /** Applicable for XC_INFORMATION_UPDATE and XC_DEINSTALL. */
    @JsonProperty("attachments")
    private List<CrossConnectAttachment> attachments;

    /** Removal schedule. Required for XC_DEINSTALL - the requestedDateTime is mandatory and only the STANDARD schedule type is supported. */
    @JsonProperty("schedule")
    private DeinstallScheduleRequest schedule;

    /** Applicable for XC_DEINSTALL. Confirms the customer consents to the removal even though the Cross Connect may still be carrying live traffic. */
    @JsonProperty("liveTrafficConsentProvided")
    private Boolean liveTrafficConsentProvided;

    /** Applicable for XC_DEINSTALL. Only XC_PATCH_EQUIPMENT is supported for deinstall. */
    @JsonProperty("services")
    private List<CrossConnectServiceRequest> services;

    /** Applicable for XC_DEINSTALL. The contacts are appended to the ones already held on the Cross Connect. */
    @JsonProperty("notifications")
    private List<CrossConnectNotification> notifications;

    /** Applicable for XC_DEINSTALL. The purchase order and the customer reference identifier replace the ones already held on the Cross Connect. */
    @JsonProperty("order")
    private OrderRequest order;
}
