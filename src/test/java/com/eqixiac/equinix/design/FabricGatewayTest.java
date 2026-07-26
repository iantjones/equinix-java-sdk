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

package com.eqixiac.equinix.design;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.design.optimizer.MetroOptimizer;
import com.eqixiac.equinix.design.peering.PeeringIntelligence;
import com.eqixiac.equinix.fabric.client.CloudRouters;
import com.eqixiac.equinix.fabric.client.Connections;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.Prices;
import com.eqixiac.equinix.fabric.client.RoutingProtocols;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link FabricGateway} composition boundary: {@code Fabric} implements the gateway,
 * and the value-add engines depend on the interface (so they can be driven by a stub, not only a
 * concrete {@code Fabric} wired to live HTTP).
 */
class FabricGatewayTest {

    private static final FabricGateway STUB_GATEWAY = new FabricGateway() {
        public Metros metros() { return null; }
        public ServiceProfiles serviceProfiles() { return null; }
        public CloudRouters cloudRouters() { return null; }
        public Connections connections() { return null; }
        public RoutingProtocols routingProtocols() { return null; }
        public Prices prices() { return null; }
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
