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
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectResponseBmmrType;
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectResponseConnectionDestinationType;
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectResponsePathType;
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectResponseType;
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectState;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Optical Metro Connect connection with its current state and the resolved physical termination points at both ends.
 *
 * <p>Maps spec schema {@code OpticalConnectResponse}. Unknown properties are ignored on read.</p>
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
public class OpticalConnectResponse {

    /** URI of this Optical Connect resource. Read-only (server-populated). */
    @JsonProperty("href")
    private String href;

    /** Unique identifier of this Optical Connect. Read-only (server-populated). */
    @JsonProperty("uuid")
    private String uuid;

    /** Connection Type. &lt;br&gt; OC - Optical Connect. */
    @JsonProperty("type")
    private OpticalConnectResponseType type;

    /** Equinix-assigned name, derived from the account number and the two IBX locations. Read-only (server-populated). */
    @JsonProperty("name")
    private String name;

    /** Optical Connect lifecycle states. */
    @JsonProperty("state")
    private OpticalConnectState state;

    /** Provisioned connection bandwidth in Mbps. */
    @JsonProperty("bandwidth")
    private Integer bandwidth;

    /** Where the Z-side terminates. &lt;br&gt; COLO - your own colocation space at another Equinix IBX. &lt;br&gt; BMMR - a Building Meet-Me Room. &lt;br&gt; REMOTE - another customer's colocation space at another Equinix IBX. &lt;br&gt; */
    @JsonProperty("connectionDestinationType")
    private OpticalConnectResponseConnectionDestinationType connectionDestinationType;

    /** Level of resilience for the connection. &lt;br&gt; UNPROTECTED - a single fiber path with no node redundancy. &lt;br&gt; PROTECTED - two diverse fiber paths between single nodes at each IBX. &lt;br&gt; DUAL_DIVERSE - two diverse fiber paths terminating on redundant nodes at both IBXs. */
    @JsonProperty("pathType")
    private OpticalConnectResponsePathType pathType;

    /** BMMR infrastructure ownership type. &lt;br&gt; EQUINIX - Equinix-operated. &lt;br&gt; SELF - customer-operated. &lt;br&gt; Required only when connectionDestinationType is BMMR. */
    @JsonProperty("bmmrType")
    private OpticalConnectResponseBmmrType bmmrType;

    /** Optical Connect redundancy configuration. &lt;br&gt; Required only for DUAL_DIVERSE connection. */
    @JsonProperty("redundancy")
    private OpticalConnectRedundancy redundancy;

    /** Originating end as provisioned, with the demarcation point and IBX location. */
    @JsonProperty("aSide")
    private OpticalConnectASideResponse aSide;

    /** Terminating end as provisioned, with the demarcation point and IBX location. */
    @JsonProperty("zSide")
    private OpticalConnectZSideResponse zSide;

    /** Ordering and billing reference details for this connection. */
    @JsonProperty("order")
    private OpticalConnectOrder order;

    /** The {@code account} property. */
    @JsonProperty("account")
    private SimplifiedAccount account;

    /** Preferences for notifications on connection configuration or status changes */
    @JsonProperty("notifications")
    private List<OpticalConnectNotification> notifications;

    /** Change log */
    @JsonProperty("changeLog")
    private Changelog changeLog;
}
