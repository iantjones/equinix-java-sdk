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

package com.eqixiac.equinix.internetaccess.model.implementation;

import com.eqixiac.equinix.internetaccess.enums.IpBlockType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * IP block product breakdown of an Equinix Internet Access (EIA) v1 price entry. Present for
 * IP-block price results.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpBlockProductPrice {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private IpBlockType type;

    @JsonProperty("location")
    private IbxLocation location;

    @JsonProperty("ipv4")
    private CustomerIpBlock ipv4;

    @JsonProperty("ipv6")
    private CustomerIpBlock ipv6;

    /**
     * The prefix length (subnet size) of an {@link IpBlockProductPrice} IP family.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerIpBlock {

        /**
         * The prefix lengths priced. The spec types {@code prefixLength} as an integer, but the live
         * price search returned an array on 2026-11-12 (path
         * {@code service.routingProtocol.ipv4.customerRoute.ipBlock.prefixLength}); both shapes are read.
         */
        @JsonProperty("prefixLength")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<Integer> prefixLengths;

        /**
         * @return the first (or only) prefix length, or {@code null} when none was returned
         */
        @com.fasterxml.jackson.annotation.JsonIgnore
        public Integer getPrefixLength() {
            return prefixLengths == null || prefixLengths.isEmpty() ? null : prefixLengths.get(0);
        }
    }
}
