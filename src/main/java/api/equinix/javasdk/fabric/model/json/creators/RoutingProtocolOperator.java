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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.RoutingProtocolClientImpl;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolJson;
import api.equinix.javasdk.fabric.model.wrappers.RoutingProtocolWrapper;
import lombok.Getter;

public class RoutingProtocolOperator extends ResourceImpl<RoutingProtocol> {

    @Getter
    private final Pageable<RoutingProtocol> serviceClient;

    public RoutingProtocolOperator(Pageable<RoutingProtocol> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public RoutingProtocolBuilder create() {
        return new RoutingProtocolBuilder();
    }

    @Getter
    public class RoutingProtocolBuilder {

        private RoutingProtocolType type;
        private String name;

        private String bgpIpv4CustomerPeerIp;
        private String bgpIpv4EquinixPeerIp;
        private Boolean bgpIpv4Enabled;

        private String bgpIpv6CustomerPeerIp;
        private String bgpIpv6EquinixPeerIp;
        private Boolean bgpIpv6Enabled;

        private String directIpv4EquinixIfaceIp;
        private String directIpv6EquinixIfaceIp;

        private Boolean bfdEnabled;
        private Integer bfdInterval;

        private Long customerAsn;
        private Long equinixAsn;

        protected RoutingProtocolBuilder() {
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder ofType(RoutingProtocolType type) {
            this.type = type;
            return this;
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder withBGPIpv4(String customerPeerIp, String equinixPeerIp, Boolean enabled) {
            this.bgpIpv4CustomerPeerIp = customerPeerIp;
            this.bgpIpv4EquinixPeerIp = equinixPeerIp;
            this.bgpIpv4Enabled = enabled;
            return this;
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder withBGPIpv6(String customerPeerIp, String equinixPeerIp, Boolean enabled) {
            this.bgpIpv6CustomerPeerIp = customerPeerIp;
            this.bgpIpv6EquinixPeerIp = equinixPeerIp;
            this.bgpIpv6Enabled = enabled;
            return this;
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder withDirectIpv4(String equinixIfaceIp) {
            this.directIpv4EquinixIfaceIp = equinixIfaceIp;
            return this;
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder withDirectIpv6(String equinixIfaceIp) {
            this.directIpv6EquinixIfaceIp = equinixIfaceIp;
            return this;
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder withBFD(Boolean enabled, Integer interval) {
            this.bfdEnabled = enabled;
            this.bfdInterval = interval;
            return this;
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder withCustomerAsn(Long customerAsn) {
            this.customerAsn = customerAsn;
            return this;
        }

        public RoutingProtocolOperator.RoutingProtocolBuilder withEquinixAsn(Long equinixAsn) {
            this.equinixAsn = equinixAsn;
            return this;
        }

        public RoutingProtocol create(String connectionId) {
            RoutingProtocolCreatorJson routingProtocolCreatorJson = new RoutingProtocolCreatorJson(this);
            RoutingProtocolJson routingProtocolJson = ((RoutingProtocolClientImpl) RoutingProtocolOperator.this.getServiceClient()).create(connectionId, routingProtocolCreatorJson);
            return new RoutingProtocolWrapper(routingProtocolJson, RoutingProtocolOperator.this.getServiceClient());
        }

        public RoutingProtocol create(Connection connection) {
            return create(connection.getUuid());
        }
    }
}
