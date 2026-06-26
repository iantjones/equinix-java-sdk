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
 * Represents a network's presence at an internet exchange LAN from the PeeringDB API.
 *
 * <p>Each {@code netixlan} entry represents a single peering session (or port) that a network
 * has at an internet exchange. Key fields include port speed, IPv4/IPv6 addresses,
 * route server participation, and BFD support.</p>
 *
 * @author ianjones
 * @see PeeringDbIx
 * @see PeeringDbNetwork
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeeringDbNetIxlan {

    @JsonProperty("id")
    private int id;

    @JsonProperty("net_id")
    private int netId;

    @JsonProperty("ix_id")
    private int ixId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("ixlan_id")
    private int ixlanId;

    @JsonProperty("notes")
    private String notes;

    /** Port speed in Mbps. */
    @JsonProperty("speed")
    private int speed;

    @JsonProperty("asn")
    private long asn;

    @JsonProperty("ipaddr4")
    private String ipaddr4;

    @JsonProperty("ipaddr6")
    private String ipaddr6;

    /** Whether this network peers via the IX route servers (MLPE). */
    @JsonProperty("is_rs_peer")
    private boolean isRsPeer;

    /** Whether BFD (Bidirectional Forwarding Detection) is supported. */
    @JsonProperty("bfd_support")
    private boolean bfdSupport;

    @JsonProperty("operational")
    private boolean operational;

    @JsonProperty("status")
    private String status;
}
