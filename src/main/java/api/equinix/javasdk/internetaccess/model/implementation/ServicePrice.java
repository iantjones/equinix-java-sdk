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

import api.equinix.javasdk.internetaccess.enums.AccessPointType;
import api.equinix.javasdk.internetaccess.enums.BillingType;
import api.equinix.javasdk.internetaccess.enums.ConnectionType;
import api.equinix.javasdk.internetaccess.enums.Redundancy;
import api.equinix.javasdk.internetaccess.enums.RoutingProtocolType;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Internet access service product breakdown of an Equinix Internet Access (EIA) v1 price entry.
 * Present for service price results.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicePrice {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private Redundancy type;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("minBandwidthCommit")
    private Integer minBandwidthCommit;

    @JsonProperty("billing")
    private BillingType billing;

    @JsonProperty("useCase")
    private UseCase useCase;

    @JsonProperty("connection")
    private ConnectionPrice connection;

    @JsonProperty("routingProtocol")
    private RoutingProtocolPrice routingProtocol;

    /**
     * Connection breakdown of a {@link ServicePrice}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConnectionPrice {

        @JsonProperty("uuid")
        private String uuid;

        @JsonProperty("type")
        private ConnectionType type;

        @JsonProperty("aSide")
        private ASidePrice aSide;
    }

    /**
     * A-side (subscriber) configuration of a {@link ConnectionPrice}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ASidePrice {

        @JsonProperty("accessPoint")
        private AccessPointPrice accessPoint;
    }

    /**
     * Access point of an {@link ASidePrice}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccessPointPrice {

        @JsonProperty("type")
        private AccessPointType type;

        @JsonProperty("location")
        private IbxLocation location;

        @JsonProperty("port")
        private PortPrice port;
    }

    /**
     * Port associated with an {@link AccessPointPrice}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PortPrice {

        @JsonProperty("uuid")
        private String uuid;

        @JsonProperty("physicalPortQuantity")
        private Integer physicalPortQuantity;

        @JsonProperty("physicalPort")
        private PhysicalPortPrice physicalPort;
    }

    /**
     * Physical port detail of a {@link PortPrice}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhysicalPortPrice {

        @JsonProperty("speed")
        private Integer speed;
    }

    /**
     * Routing protocol breakdown of a {@link ServicePrice}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoutingProtocolPrice {

        @JsonProperty("type")
        private RoutingProtocolType type;

        @JsonProperty("ipv4")
        private RoutingProtocolIpBlock ipv4;

        @JsonProperty("ipv6")
        private RoutingProtocolIpBlock ipv6;
    }

    /**
     * Customer-route IP block of a {@link RoutingProtocolPrice} IP family.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoutingProtocolIpBlock {

        @JsonProperty("customerRoute")
        private CustomerRoutePrice customerRoute;
    }

    /**
     * Customer route of a {@link RoutingProtocolIpBlock}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerRoutePrice {

        @JsonProperty("ipBlock")
        private IpBlockProductPrice.CustomerIpBlock ipBlock;
    }
}
