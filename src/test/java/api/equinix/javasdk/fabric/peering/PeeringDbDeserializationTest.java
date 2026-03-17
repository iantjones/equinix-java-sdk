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

package api.equinix.javasdk.fabric.peering;

import api.equinix.javasdk.fabric.peering.client.PeeringDbNetIxlan;
import api.equinix.javasdk.fabric.peering.client.PeeringDbNetwork;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for PeeringDB JSON response models.
 *
 * <p>Validates that PeeringDB API responses can be correctly deserialized
 * into the SDK's internal models with all fields mapped.</p>
 */
@DisplayName("PeeringDB Deserialization")
class PeeringDbDeserializationTest {

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Nested
    @DisplayName("Network (net) deserialization")
    class NetworkTests {

        private PeeringDbNetwork network;

        @BeforeEach
        void setUp() throws Exception {
            InputStream is = PeeringDbDeserializationTest.class
                    .getResourceAsStream("/json/peering/peeringdb_network_response.json");
            assertNotNull(is, "peeringdb_network_response.json fixture not found");
            JsonNode root = mapper.readTree(is);
            List<PeeringDbNetwork> networks = mapper.readValue(
                    root.get("data").traverse(), new TypeReference<List<PeeringDbNetwork>>() {});
            assertFalse(networks.isEmpty());
            network = networks.get(0);
        }

        @Test
        @DisplayName("ASN should be deserialized correctly")
        void asn_isDeserialized() {
            assertEquals(16509, network.getAsn());
        }

        @Test
        @DisplayName("Network name should be deserialized")
        void name_isDeserialized() {
            assertEquals("Amazon.com, Inc.", network.getName());
        }

        @Test
        @DisplayName("Policy general should be deserialized")
        void policyGeneral_isDeserialized() {
            assertEquals("Selective", network.getPolicyGeneral());
        }

        @Test
        @DisplayName("Info type should be deserialized")
        void infoType_isDeserialized() {
            assertEquals("Content", network.getInfoType());
        }

        @Test
        @DisplayName("Traffic info should be deserialized")
        void trafficInfo_isDeserialized() {
            assertEquals("100-200Gbps", network.getInfoTraffic());
            assertEquals("Heavy Outbound", network.getInfoRatio());
        }

        @Test
        @DisplayName("IPv6 flag should be deserialized")
        void ipv6_isDeserialized() {
            assertTrue(network.isInfoIpv6());
        }
    }

    @Nested
    @DisplayName("NetIxlan (IX presence) deserialization")
    class NetIxlanTests {

        private List<PeeringDbNetIxlan> ixPresences;

        @BeforeEach
        void setUp() throws Exception {
            InputStream is = PeeringDbDeserializationTest.class
                    .getResourceAsStream("/json/peering/peeringdb_netixlan_response.json");
            assertNotNull(is, "peeringdb_netixlan_response.json fixture not found");
            JsonNode root = mapper.readTree(is);
            ixPresences = mapper.readValue(
                    root.get("data").traverse(), new TypeReference<List<PeeringDbNetIxlan>>() {});
        }

        @Test
        @DisplayName("Should deserialize all 3 IX presences")
        void shouldDeserializeAll() {
            assertEquals(3, ixPresences.size());
        }

        @Test
        @DisplayName("First entry should have correct IX ID and speed")
        void firstEntry_ixIdAndSpeed() {
            PeeringDbNetIxlan first = ixPresences.get(0);
            assertEquals(1, first.getIxId());
            assertEquals(100000, first.getSpeed());
        }

        @Test
        @DisplayName("Route server peer flag should be deserialized")
        void routeServerPeer_isDeserialized() {
            assertTrue(ixPresences.get(0).isRsPeer());
            assertFalse(ixPresences.get(1).isRsPeer());
        }

        @Test
        @DisplayName("BFD support flag should be deserialized")
        void bfdSupport_isDeserialized() {
            assertTrue(ixPresences.get(0).isBfdSupport());
            assertFalse(ixPresences.get(2).isBfdSupport());
        }

        @Test
        @DisplayName("IPv4 and IPv6 addresses should be deserialized")
        void ipAddresses_areDeserialized() {
            assertEquals("206.126.236.20", ixPresences.get(0).getIpaddr4());
            assertEquals("2001:504:0:2::16509:1", ixPresences.get(0).getIpaddr6());
        }

        @Test
        @DisplayName("Null IPv6 address should be handled")
        void nullIpv6_isHandled() {
            assertNull(ixPresences.get(2).getIpaddr6());
        }

        @Test
        @DisplayName("ASN should be correct across all entries")
        void asn_isConsistent() {
            ixPresences.forEach(p -> assertEquals(16509, p.getAsn()));
        }

        @Test
        @DisplayName("Operational flag should be deserialized")
        void operational_isDeserialized() {
            assertTrue(ixPresences.get(0).isOperational());
        }
    }
}
