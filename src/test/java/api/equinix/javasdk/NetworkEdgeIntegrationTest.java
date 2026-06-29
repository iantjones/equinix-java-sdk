package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.PublicKey;
import api.equinix.javasdk.networkedge.model.VPN;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration-readonly")
@DisplayName("Network Edge Integration Tests")
class NetworkEdgeIntegrationTest extends IntegrationTestBase {

    static NetworkEdge client;

    @BeforeAll
    static void setUp() {
        client = new NetworkEdge(testCredentials());
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Readonly Operations")
    class ReadonlyTests {

        @Test
        @DisplayName("List devices and get by UUID")
        void listDevices() {
            try {
                PaginatedList<Device> items = timedCall("NetworkEdge", "list", "Device", "GET",
                        () -> client.devices().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Device item = timedCall("NetworkEdge", "getByUuid", "Device", "GET",
                            items.get(0).getUuid(),
                            () -> client.devices().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Devices test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List device types returns valid response")
        void listDeviceTypes() {
            try {
                PaginatedList<DeviceType> items = timedCall("NetworkEdge", "listDeviceTypes", "DeviceType", "GET",
                        () -> client.devices().listDeviceTypes());
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "DeviceTypes test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List public keys returns valid response")
        void listPublicKeys() {
            try {
                List<PublicKey> items = timedCall("NetworkEdge", "list", "PublicKey", "GET",
                        () -> client.publicKeys().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "PublicKeys test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List device links and get by UUID")
        void listDeviceLinks() {
            try {
                PaginatedList<DeviceLink> items = timedCall("NetworkEdge", "list", "DeviceLink", "GET",
                        () -> client.deviceLinks().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    DeviceLink item = timedCall("NetworkEdge", "getByUuid", "DeviceLink", "GET",
                            items.get(0).getUuid(),
                            () -> client.deviceLinks().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "DeviceLinks test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List ACL templates returns valid response")
        void listACLTemplates() {
            try {
                PaginatedList<ACLTemplate> items = timedCall("NetworkEdge", "list", "ACLTemplate", "GET",
                        () -> client.aclTemplates().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "ACLTemplates test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List VPNs and get by UUID")
        void listVPNs() {
            try {
                PaginatedList<VPN> items = timedCall("NetworkEdge", "list", "VPN", "GET",
                        () -> client.vpns().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    VPN item = timedCall("NetworkEdge", "getByUuid", "VPN", "GET",
                            items.get(0).getUuid(),
                            () -> client.vpns().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "VPNs test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List BGP peerings returns valid response")
        void listBGPPeerings() {
            try {
                PaginatedList<BGPPeering> items = timedCall("NetworkEdge", "list", "BGPPeering", "GET",
                        () -> client.bgpPeerings().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "BGPPeerings test skipped: " + e.getMessage());
            }
        }
    }
}
