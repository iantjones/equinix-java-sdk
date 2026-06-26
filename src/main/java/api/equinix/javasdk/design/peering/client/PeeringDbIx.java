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
 * Represents an internet exchange point from the PeeringDB API.
 *
 * <p>Contains identity, location, and connectivity information for an IX. When scoped
 * to Equinix (org_id=2), represents one of the ~47 Equinix Internet Exchange locations
 * globally. The {@code city} and {@code country} fields are used for mapping to
 * Equinix {@code MetroCode} values.</p>
 *
 * @author ianjones
 * @see PeeringDbNetIxlan
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeeringDbIx {

    @JsonProperty("id")
    private int id;

    @JsonProperty("org_id")
    private int orgId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("name_long")
    private String nameLong;

    @JsonProperty("city")
    private String city;

    @JsonProperty("country")
    private String country;

    @JsonProperty("region_continent")
    private String regionContinent;

    @JsonProperty("media")
    private String media;

    @JsonProperty("proto_unicast")
    private boolean protoUnicast;

    @JsonProperty("proto_multicast")
    private boolean protoMulticast;

    @JsonProperty("proto_ipv6")
    private boolean protoIpv6;

    @JsonProperty("net_count")
    private int netCount;

    @JsonProperty("fac_count")
    private int facCount;

    @JsonProperty("website")
    private String website;

    @JsonProperty("tech_email")
    private String techEmail;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("status")
    private String status;
}
