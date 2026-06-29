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

package api.equinix.javasdk.internetaccess.model.implementation;

import api.equinix.javasdk.internetaccess.enums.CustomerAsnRange;
import api.equinix.javasdk.internetaccess.enums.ExportPolicy;
import api.equinix.javasdk.internetaccess.enums.RoutingProtocolType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Routing protocol of an Equinix Internet Access (EIA) v2 service, as returned in the service read
 * model.
 *
 * <p>The protocol is polymorphic by {@code type} ({@code DIRECT}, {@code STATIC} or {@code BGP});
 * this read view flattens all variants, exposing the common fields together with the BGP-specific
 * fields (ASN, export policy, authentication type) that are only populated when {@code type} is
 * {@code BGP}.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoutingProtocolReadModel {

    @JsonProperty("href")
    private String href;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private RoutingProtocolType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("ipv4")
    private RoutingProtocolIpFamily ipv4;

    @JsonProperty("ipv6")
    private RoutingProtocolIpFamily ipv6;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("tags")
    private List<String> tags;

    // BGP-specific fields (populated only when type == BGP).

    @JsonProperty("customerAsn")
    private Long customerAsn;

    @JsonProperty("customerAsnRange")
    private CustomerAsnRange customerAsnRange;

    @JsonProperty("equinixAsn")
    private Long equinixAsn;

    @JsonProperty("bgpAuthenticationType")
    private String bgpAuthenticationType;

    @JsonProperty("exportPolicy")
    private ExportPolicy exportPolicy;

    /**
     * IPv4 or IPv6 routing details: the customer routes and peerings of a
     * {@link RoutingProtocolReadModel}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoutingProtocolIpFamily {

        @JsonProperty("customerRoutes")
        private List<CustomerRoute> customerRoutes;

        @JsonProperty("peerings")
        private List<Peering> peerings;
    }

    /**
     * A customer route of a {@link RoutingProtocolReadModel}. Provider-assigned (PA) routes carry an
     * {@code ipBlock}; provider-independent (PI) routes carry a {@code prefix}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerRoute {

        @JsonProperty("importPolicy")
        private String importPolicy;

        @JsonProperty("prefix")
        private String prefix;

        @JsonProperty("ipBlock")
        private RoutingIpBlock ipBlock;
    }

    /**
     * Provider-assigned IP block referenced by a PA {@link CustomerRoute}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoutingIpBlock {

        @JsonProperty("href")
        private String href;

        @JsonProperty("uuid")
        private String uuid;

        @JsonProperty("type")
        private String type;

        @JsonProperty("prefix")
        private String prefix;

        @JsonProperty("prefixLength")
        private Integer prefixLength;
    }

    /**
     * A peering of a {@link RoutingProtocolReadModel}: the Equinix/customer peer IPs and VRRP state.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Peering {

        @JsonProperty("customerPeerIps")
        private List<String> customerPeerIps;

        @JsonProperty("equinixPeerIps")
        private List<String> equinixPeerIps;

        @JsonProperty("equinixVRRPIp")
        private String equinixVRRPIp;

        @JsonProperty("customerVRRPIp")
        private String customerVRRPIp;

        @JsonProperty("vrrpEnabled")
        private Boolean vrrpEnabled;

        @JsonProperty("peerSubnet")
        private PeerSubnet peerSubnet;

        @JsonProperty("connections")
        private List<PeeringConnection> connections;
    }

    /**
     * Subnet used for a {@link Peering}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PeerSubnet {

        @JsonProperty("prefix")
        private String prefix;

        @JsonProperty("prefixLength")
        private Integer prefixLength;
    }

    /**
     * A connection referenced by a {@link Peering}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PeeringConnection {

        @JsonProperty("href")
        private String href;

        @JsonProperty("uuid")
        private String uuid;
    }
}
