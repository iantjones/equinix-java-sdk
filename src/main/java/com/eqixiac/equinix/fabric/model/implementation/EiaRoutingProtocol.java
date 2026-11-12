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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.EiaBgpAuthType;
import com.eqixiac.equinix.fabric.enums.EiaRoutingProtocolType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * The routing protocol of an EIA service as read back (spec {@code InternetAccessRoutingProtocol}
 * and its BGP variant {@code InternetAccessRoutingProtocolBgp}, 4.34.0).
 *
 * <p>The spec-deprecated BGP properties {@code customerAsn}, {@code customerAsnRange} and
 * {@code bgpAuthKey} are not modelled: the ASN is the {@code asn} reference and the
 * presence of a key is {@code bgpAuthType}.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EiaRoutingProtocol {

    /** The routing protocol type ({@code BGP}, {@code DIRECT} or {@code STATIC}). */
    @JsonProperty("type")
    private EiaRoutingProtocolType type;

    /** The BGP export policy, for example {@code FULL}. */
    @JsonProperty("exportPolicy")
    private String exportPolicy;

    /** The ASN resource used for the BGP session ({@code /fabric/v4/asns/{uuid}}). */
    @JsonProperty("asn")
    private AsnRef asn;

    /** Whether a BGP authentication key is configured. */
    @JsonProperty("bgpAuthType")
    private EiaBgpAuthType bgpAuthType;

    /** Reference to an ASN resource: {@code {"href": ..., "uuid": ...}}. */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AsnRef {
        /** The ASN resource URI. */
        @JsonProperty("href")
        private String href;

        /** The ASN resource uuid. */
        @JsonProperty("uuid")
        private String uuid;
    }
}
