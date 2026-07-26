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

package com.eqixiac.equinix.internetaccess.model.json.creators;

import com.eqixiac.equinix.internetaccess.enums.RoutingProtocolType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * {@code DIRECT} routing protocol nested in a {@link ServiceRequest}. Carries directly attached
 * IPv4/IPv6 peerings together with customer routes.
 */
@Getter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectRoutingProtocolRequest extends RoutingProtocolRequest {

    @JsonProperty("ipv4") private DirectRoutingProtocolIpv4Request ipv4;
    @JsonProperty("ipv6") private DirectRoutingProtocolIpv6Request ipv6;

    @Override
    public RoutingProtocolType getType() {
        return RoutingProtocolType.DIRECT;
    }
}
