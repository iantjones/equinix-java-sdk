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

package api.equinix.javasdk.internetaccess.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * IP Version 6 (IPv6) peering nested in a direct routing protocol request.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectPeeringIpv6Request {

    @JsonProperty("connection") private PeeringConnection connection;

    /** Peering IP addresses in Version 6 (IPv6). */
    @Singular("equinixPeerIp")
    @JsonProperty("equinixPeerIps") private List<String> equinixPeerIps;

    /** Virtual router group IP address in Version 6 (IPv6). */
    @JsonProperty("equinixVRRPIp") private String equinixVRRPIp;
}
