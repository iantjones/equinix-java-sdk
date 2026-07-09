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
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a network (autonomous system) from the PeeringDB API.
 *
 * <p>Contains identity, peering policy, traffic profile, and routing information
 * for a network registered in PeeringDB. Key fields for peering intelligence include
 * the peering policy ({@code policyGeneral}), network type ({@code infoType}),
 * traffic ratio, and route server participation preferences.</p>
 *
 * @author ianjones
 * @see <a href="https://docs.peeringdb.com/api_specs/">PeeringDB API Specs</a>
 */
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeeringDbNetwork {

    @JsonProperty("id")
    private int id;

    @JsonProperty("asn")
    private long asn;

    @JsonProperty("name")
    private String name;

    @JsonProperty("aka")
    private String aka;

    @JsonProperty("name_long")
    private String nameLong;

    @JsonProperty("website")
    private String website;

    @JsonProperty("irr_as_set")
    private String irrAsSet;

    @JsonProperty("info_type")
    private String infoType;

    @JsonProperty("info_prefixes4")
    private int infoPrefixes4;

    @JsonProperty("info_prefixes6")
    private int infoPrefixes6;

    @JsonProperty("info_traffic")
    private String infoTraffic;

    @JsonProperty("info_ratio")
    private String infoRatio;

    @JsonProperty("info_scope")
    private String infoScope;

    @JsonProperty("info_unicast")
    private boolean infoUnicast;

    @JsonProperty("info_multicast")
    private boolean infoMulticast;

    @JsonProperty("info_ipv6")
    private boolean infoIpv6;

    @JsonProperty("info_never_via_route_servers")
    private boolean infoNeverViaRouteServers;

    @JsonProperty("policy_url")
    private String policyUrl;

    @JsonProperty("policy_general")
    private String policyGeneral;

    @JsonProperty("policy_locations")
    private String policyLocations;

    @JsonProperty("policy_ratio")
    private boolean policyRatio;

    @JsonProperty("policy_contracts")
    private String policyContracts;

    @JsonProperty("ix_count")
    private int ixCount;

    @JsonProperty("fac_count")
    private int facCount;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("status")
    private String status;
}
