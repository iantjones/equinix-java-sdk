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

import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.RoutingProtocolClientImpl;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.json.RoutingProtocolJson;
import api.equinix.javasdk.fabric.model.wrappers.RoutingProtocolWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class RoutingProtocolOperator extends ResourceImpl<RoutingProtocol> {

    @Getter
    private final Pageable<RoutingProtocol> serviceClient;

    public RoutingProtocolOperator(Pageable<RoutingProtocol> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public RoutingProtocolBuilder create() {
        return new RoutingProtocolBuilder();
    }

    /**
     * Begins a fluent update of an existing routing protocol, identified by its parent
     * connection and uuid.
     *
     * @param connectionId the uuid of the parent connection
     * @param uuid the uuid of the routing protocol to update
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.RoutingProtocolOperator.RoutingProtocolUpdater} object.
     */
    public RoutingProtocolUpdater update(String connectionId, String uuid) {
        return new RoutingProtocolUpdater(connectionId, uuid);
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

        private String bgpAuthKey;
        private Boolean asOverrideEnabled;

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

        /**
         * Sets the BGP authorization key (MD5 peering password) for a BGP routing protocol.
         *
         * @param bgpAuthKey the BGP authorization key
         * @return this builder
         */
        public RoutingProtocolOperator.RoutingProtocolBuilder withBgpAuthKey(String bgpAuthKey) {
            this.bgpAuthKey = bgpAuthKey;
            return this;
        }

        /**
         * Enables or disables AS number override on a BGP routing protocol.
         *
         * @param asOverrideEnabled whether to enable AS number override
         * @return this builder
         */
        public RoutingProtocolOperator.RoutingProtocolBuilder withAsOverrideEnabled(Boolean asOverrideEnabled) {
            this.asOverrideEnabled = asOverrideEnabled;
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

    /**
     * Fluent builder for updating an existing routing protocol. Each typed setter records a
     * {@code replace} change operation; {@link #save()} sends them as one {@code PATCH}
     * (an op/path/value array, content-type {@code application/json}) and returns the refreshed model.
     *
     * <p>Routing protocols are parent-keyed, so both the parent connection id and the routing
     * protocol uuid are captured up front.</p>
     *
     * <pre>{@code routingProtocol.update(connectionId).name("New-Name").save();}</pre>
     */
    public class RoutingProtocolUpdater {

        private final String connectionId;
        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected RoutingProtocolUpdater(String connectionId, String uuid) {
            this.connectionId = connectionId;
            this.uuid = uuid;
        }

        /**
         * Replaces the routing protocol name.
         *
         * @param name the new name
         * @return this updater
         */
        public RoutingProtocolUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        /**
         * Replaces whether BGP IPv4 is enabled on this routing protocol.
         *
         * @param enabled the new enabled flag
         * @return this updater
         */
        public RoutingProtocolUpdater bgpIpv4Enabled(Boolean enabled) {
            operations.add(PatchOperation.replace("/bgpIpv4/enabled", enabled));
            return this;
        }

        /**
         * Replaces whether BGP IPv6 is enabled on this routing protocol.
         *
         * @param enabled the new enabled flag
         * @return this updater
         */
        public RoutingProtocolUpdater bgpIpv6Enabled(Boolean enabled) {
            operations.add(PatchOperation.replace("/bgpIpv6/enabled", enabled));
            return this;
        }

        /**
         * Adds an arbitrary change operation, for paths not covered by the typed setters above.
         *
         * @param operation the patch operation
         * @return this updater
         */
        public RoutingProtocolUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the routing protocol refreshed from the server.
         *
         * @return the updated {@link api.equinix.javasdk.fabric.model.RoutingProtocol}
         */
        public RoutingProtocol save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            RoutingProtocolJson routingProtocolJson = ((RoutingProtocolClientImpl) RoutingProtocolOperator.this.getServiceClient()).update(connectionId, uuid, operations);
            return new RoutingProtocolWrapper(routingProtocolJson, RoutingProtocolOperator.this.getServiceClient());
        }
    }
}
