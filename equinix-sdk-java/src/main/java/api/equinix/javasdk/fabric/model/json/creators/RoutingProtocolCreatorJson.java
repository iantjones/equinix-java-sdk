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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.implementation.BFDConfig;
import api.equinix.javasdk.fabric.model.implementation.BGPConnectionIpv4;
import api.equinix.javasdk.fabric.model.implementation.BGPConnectionIpv6;
import api.equinix.javasdk.fabric.model.implementation.DirectConnectionIpv4;
import api.equinix.javasdk.fabric.model.implementation.DirectConnectionIpv6;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
public class RoutingProtocolCreatorJson {

    @JsonProperty("type")
    private RoutingProtocolType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("bgpIpv4")
    private BGPIpv4Config bgpIpv4;

    @JsonProperty("bgpIpv6")
    private BGPIpv6Config bgpIpv6;

    @JsonProperty("directIpv4")
    private DirectIpv4Config directIpv4;

    @JsonProperty("directIpv6")
    private DirectIpv6Config directIpv6;

    @JsonProperty("bfd")
    private BFDConfigCreate bfd;

    @JsonProperty("customerAsn")
    private Long customerAsn;

    @JsonProperty("equinixAsn")
    private Long equinixAsn;

    @Setter(AccessLevel.PACKAGE)
    public static class BGPIpv4Config {

        @JsonProperty("customerPeerIp")
        private String customerPeerIp;

        @JsonProperty("equinixPeerIp")
        private String equinixPeerIp;

        @JsonProperty("enabled")
        private Boolean enabled;
    }

    @Setter(AccessLevel.PACKAGE)
    public static class BGPIpv6Config {

        @JsonProperty("customerPeerIp")
        private String customerPeerIp;

        @JsonProperty("equinixPeerIp")
        private String equinixPeerIp;

        @JsonProperty("enabled")
        private Boolean enabled;
    }

    @Setter(AccessLevel.PACKAGE)
    public static class DirectIpv4Config {

        @JsonProperty("equinixIfaceIp")
        private String equinixIfaceIp;
    }

    @Setter(AccessLevel.PACKAGE)
    public static class DirectIpv6Config {

        @JsonProperty("equinixIfaceIp")
        private String equinixIfaceIp;
    }

    @Setter(AccessLevel.PACKAGE)
    public static class BFDConfigCreate {

        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("interval")
        private Integer interval;
    }

    public RoutingProtocolCreatorJson(RoutingProtocolOperator.RoutingProtocolBuilder routingProtocolBuilder) {
        this.type = routingProtocolBuilder.getType();
        this.name = routingProtocolBuilder.getName();
        this.customerAsn = routingProtocolBuilder.getCustomerAsn();
        this.equinixAsn = routingProtocolBuilder.getEquinixAsn();

        if (routingProtocolBuilder.getBgpIpv4CustomerPeerIp() != null) {
            BGPIpv4Config ipv4Config = new BGPIpv4Config();
            ipv4Config.setCustomerPeerIp(routingProtocolBuilder.getBgpIpv4CustomerPeerIp());
            ipv4Config.setEquinixPeerIp(routingProtocolBuilder.getBgpIpv4EquinixPeerIp());
            ipv4Config.setEnabled(routingProtocolBuilder.getBgpIpv4Enabled());
            this.bgpIpv4 = ipv4Config;
        }

        if (routingProtocolBuilder.getBgpIpv6CustomerPeerIp() != null) {
            BGPIpv6Config ipv6Config = new BGPIpv6Config();
            ipv6Config.setCustomerPeerIp(routingProtocolBuilder.getBgpIpv6CustomerPeerIp());
            ipv6Config.setEquinixPeerIp(routingProtocolBuilder.getBgpIpv6EquinixPeerIp());
            ipv6Config.setEnabled(routingProtocolBuilder.getBgpIpv6Enabled());
            this.bgpIpv6 = ipv6Config;
        }

        if (routingProtocolBuilder.getDirectIpv4EquinixIfaceIp() != null) {
            DirectIpv4Config directIpv4Config = new DirectIpv4Config();
            directIpv4Config.setEquinixIfaceIp(routingProtocolBuilder.getDirectIpv4EquinixIfaceIp());
            this.directIpv4 = directIpv4Config;
        }

        if (routingProtocolBuilder.getDirectIpv6EquinixIfaceIp() != null) {
            DirectIpv6Config directIpv6Config = new DirectIpv6Config();
            directIpv6Config.setEquinixIfaceIp(routingProtocolBuilder.getDirectIpv6EquinixIfaceIp());
            this.directIpv6 = directIpv6Config;
        }

        if (routingProtocolBuilder.getBfdEnabled() != null) {
            BFDConfigCreate bfdConfig = new BFDConfigCreate();
            bfdConfig.setEnabled(routingProtocolBuilder.getBfdEnabled());
            bfdConfig.setInterval(routingProtocolBuilder.getBfdInterval());
            this.bfd = bfdConfig;
        }
    }
}
