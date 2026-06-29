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

package api.equinix.javasdk.design.peering.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a network's presence at a facility from the PeeringDB API.
 *
 * <p>Each {@code netfac} entry indicates that a network (identified by ASN) is physically
 * present in a specific data center facility. This is distinct from IX peering presence
 * and indicates colocation or cross-connect capability.</p>
 *
 * @author ianjones
 * @see PeeringDbFacility
 * @see PeeringDbNetwork
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeeringDbNetFac {

    @JsonProperty("id")
    private int id;

    @JsonProperty("net_id")
    private int netId;

    @JsonProperty("fac_id")
    private int facId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("local_asn")
    private long localAsn;

    @JsonProperty("city")
    private String city;

    @JsonProperty("country")
    private String country;

    @JsonProperty("status")
    private String status;
}
