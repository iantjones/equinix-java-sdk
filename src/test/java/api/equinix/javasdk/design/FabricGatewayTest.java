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

package api.equinix.javasdk.design;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.design.optimizer.MetroOptimizer;
import api.equinix.javasdk.design.peering.PeeringIntelligence;
import api.equinix.javasdk.fabric.client.CloudRouters;
import api.equinix.javasdk.fabric.client.Connections;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.RoutingProtocols;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link FabricGateway} composition boundary: {@code Fabric} implements the gateway,
 * and the value-add engines depend on the interface (so they can be driven by a stub, not only a
 * concrete {@code Fabric} wired to live HTTP).
 */
class FabricGatewayTest {

    /** Minimal stand-in proving the engines no longer require a concrete {@code Fabric}. */
    private static final FabricGateway STUB_GATEWAY = new FabricGateway() {
        public Metros metros() { return null; }
        public ServiceProfiles serviceProfiles() { return null; }
        public CloudRouters cloudRouters() { return null; }
        public Connections connections() { return null; }
        public RoutingProtocols routingProtocols() { return null; }
    };

    @Test
    void fabricImplementsFabricGateway() {
        assertTrue(FabricGateway.class.isAssignableFrom(Fabric.class));
    }

    @Test
    void engineBuildersAcceptAStubGateway() {
        // Builders capture the gateway without touching it; the dependency is the interface.
        assertNotNull(MetroOptimizer.builder(STUB_GATEWAY));
        assertNotNull(PeeringIntelligence.builder(STUB_GATEWAY));
        assertNotNull(PeeringIntelligence.builder(STUB_GATEWAY, "peeringdb-key"));
    }
}
