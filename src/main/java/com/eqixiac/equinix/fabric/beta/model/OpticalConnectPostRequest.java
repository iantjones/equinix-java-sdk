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
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectPostRequestBmmrType;
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectPostRequestConnectionDestinationType;
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectPostRequestPathType;
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectPostRequestType;
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
 * Request to order a single connection. The A-side is always a patch panel port in your own cage; the Z-side shape depends on connectionDestinationType.
 *
 * <p>Maps spec schema {@code OpticalConnectPostRequest}. Unknown properties are ignored on read.</p>
 * <p>Spec-required properties: {@code aSide}, {@code bandwidth}, {@code connectionDestinationType}, {@code pathType}, {@code type}, {@code zSide}.</p>
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
public class OpticalConnectPostRequest {

    /** OC - Optical Connect Type Required by the spec. */
    @JsonProperty("type")
    private OpticalConnectPostRequestType type;

    /** Connection bandwidth Mbps. &lt;br&gt; Available bandwidths depend on the IBX pair. &lt;br&gt; 1000 - 1 Gbps. &lt;br&gt; 10000 - 10 Gbps. &lt;br&gt; 100000 - 100 Gbps. &lt;br&gt; Required by the spec. */
    @JsonProperty("bandwidth")
    private Integer bandwidth;

    /** Where the Z-side terminates. &lt;br&gt; COLO - your own colocation space at another Equinix IBX. &lt;br&gt; BMMR - a Building Meet-Me Room. &lt;br&gt; REMOTE - another customer's colocation space at another Equinix IBX. &lt;br&gt; BMMR and REMOTE require a Letter of Authorization on the Z-side. Required by the spec. */
    @JsonProperty("connectionDestinationType")
    private OpticalConnectPostRequestConnectionDestinationType connectionDestinationType;

    /** Level of resilience for the connection. &lt;br&gt; UNPROTECTED - a single fiber path with no node redundancy. &lt;br&gt; PROTECTED - two diverse fiber paths between single nodes at each IBX. &lt;br&gt; DUAL_DIVERSE - two diverse fiber paths terminating on redundant nodes at both IBXs. Required by the spec. */
    @JsonProperty("pathType")
    private OpticalConnectPostRequestPathType pathType;

    /** Optical Connect redundancy configuration. &lt;br&gt; Required only for DUAL_DIVERSE connection. */
    @JsonProperty("redundancy")
    private OpticalConnectRedundancy redundancy;

    /** A-Side configuration. Always a patch panel port from your cage. Required by the spec. */
    @JsonProperty("aSide")
    private OpticalConnectASideRequest aSide;

    /** Z-Side configuration. &lt;br&gt; For COLO, specify the destination patch panel and ports. &lt;br&gt; For BMMR and REMOTE, specify the location and Letter of Authorization. Required by the spec. */
    @JsonProperty("zSide")
    private OpticalConnectZSideRequest zSide;

    /** Ordering and billing reference details for this connection. */
    @JsonProperty("order")
    private OpticalConnectOrder order;

    /** The {@code account} property. */
    @JsonProperty("account")
    private SimplifiedAccount account;

    /** Contacts to notify about connection configuration and status changes */
    @JsonProperty("notifications")
    private List<OpticalConnectNotification> notifications;

    /** BMMR infrastructure ownership type. &lt;br&gt; EQUINIX - Equinix-operated. &lt;br&gt; SELF - customer-operated. &lt;br&gt; Required only when connectionDestinationType is BMMR. */
    @JsonProperty("bmmrType")
    private OpticalConnectPostRequestBmmrType bmmrType;
}
