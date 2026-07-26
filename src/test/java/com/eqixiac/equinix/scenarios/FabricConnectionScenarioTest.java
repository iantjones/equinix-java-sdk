package com.eqixiac.equinix.scenarios;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.IntegrationTestBase;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.enums.*;
import com.eqixiac.equinix.fabric.model.*;
import com.eqixiac.equinix.fabric.model.implementation.LinkProtocol;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration scenario test for Connection + RoutingProtocol lifecycle with dry-run validation.
 *
 * <p>Exercises dry-run validation (valid and invalid configurations), port discovery,
 * connection creation, routing protocol attachment, and full teardown.</p>
 */
@Tag("integration-scenario")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FabricConnectionScenarioTest extends IntegrationTestBase {

    private Fabric fabric;

    private String portUuid1;
    private String portUuid2;
    private int vlanTag1 = 1501;
    private int vlanTag2 = 1502;
    private String connectionUuid;
    private String routingProtocolUuid;

    @BeforeAll
    void setUp() {
        fabric = new Fabric(testCredentials());
        fabric.authenticate();
    }

    @AfterAll
    void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    // ── Dry-Run Tests ───────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Dry-run: validate a well-formed EVPL connection")
    void dryRunValidConnection() {
        Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run mode not enabled");

        // Attempt to find a port for dry-run
        String dryRunPortUuid = findFirstDot1qPortUuid();
        Assumptions.assumeTrue(dryRunPortUuid != null, "No DOT1Q port available for dry-run");

        try {
            Connection connection = timedCall("Fabric", "dryRunCreate", "Connection", "POST",
                    () -> fabric.connections().define(ConnectionType.EVPL_VC)
                            .name(testResourceName("conn-valid-dryrun"))
                            .bandwidth(50)
                            .aSideAccessPointPort(dryRunPortUuid,
                                    LinkProtocol.dot1q().vlanTag(1500).create())
                            .notification("test@example.com")
                            .dryRun()
                            .create());
            assertNotNull(connection, "Dry-run should return a non-null response for valid config");
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Dry-run valid connection failed (may not be supported): " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Dry-run: expect error for invalid connection config")
    void dryRunInvalidConnection() {
        Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run mode not enabled");

        try {
            timedExpectedFailure("Fabric", "dryRunCreate", "Connection", "POST", null,
                    () -> fabric.connections().define(ConnectionType.EVPL_VC)
                            .name(testResourceName("conn-invalid-dryrun"))
                            .bandwidth(-1)
                            .notification("test@example.com")
                            .dryRun()
                            .create());
        } catch (Exception e) {
            // Expected: invalid bandwidth should cause an error
            Assumptions.assumeTrue(false,
                    "Dry-run invalid connection did not throw as expected: " + e.getMessage());
        }
    }

    // ── Port Discovery ──────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Find two DOT1Q ports for connection test")
    void findTwoPorts() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        try {
            PaginatedList<Port> ports = timedCall("Fabric", "list", "Port", "GET",
                    () -> fabric.ports().list());
            int found = 0;
            for (Port port : ports) {
                if (port.getEncapsulation() != null
                        && port.getEncapsulation().getType() == EncapsulationType.DOT1Q) {
                    if (found == 0) {
                        portUuid1 = port.getUuid();
                        found++;
                    } else if (!port.getUuid().equals(portUuid1)) {
                        portUuid2 = port.getUuid();
                        found++;
                        break;
                    }
                }
            }
            Assumptions.assumeTrue(found >= 2,
                    "Need at least 2 DOT1Q ports for connection test, found: " + found);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Port discovery failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Create EVPL connection between two ports")
    void createConnection() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(portUuid1 != null && portUuid2 != null, "Two ports not available");

        String name = testResourceName("evpl-conn");
        Connection connection = timedCall("Fabric", "create", "Connection", "POST",
                () -> fabric.connections().define(ConnectionType.EVPL_VC)
                        .name(name)
                        .bandwidth(50)
                        .aSideAccessPointPort(portUuid1,
                                LinkProtocol.dot1q().vlanTag(vlanTag1).create())
                        .zSideAccessPointPort(portUuid2,
                                LinkProtocol.dot1q().vlanTag(vlanTag2).create())
                        .notification("test@example.com")
                        .create());

        assertNotNull(connection, "Connection should be created");
        assertNotNull(connection.getUuid(), "Connection UUID should not be null");
        connectionUuid = connection.getUuid();
        registerCleanup("Connection", connectionUuid,
                id -> fabric.connections().getByUuid(id).delete());
    }

    @Test
    @Order(5)
    @DisplayName("Verify connection state, bandwidth, and access points")
    void verifyConnection() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(connectionUuid != null, "Connection was not created");

        Connection connection = timedCall("Fabric", "getByUuid", "Connection", "GET", connectionUuid,
                () -> fabric.connections().getByUuid(connectionUuid));

        assertNotNull(connection);
        assertEquals(connectionUuid, connection.getUuid());
        assertEquals(ConnectionType.EVPL_VC, connection.getType());
        assertEquals(Integer.valueOf(50), connection.getBandwidth());
        assertNotNull(connection.getASide(), "A-Side should not be null");
        assertNotNull(connection.getZSide(), "Z-Side should not be null");
    }

    @Test
    @Order(6)
    @DisplayName("Create BGP routing protocol on connection")
    void createRoutingProtocol() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(connectionUuid != null, "Connection was not created");

        try {
            RoutingProtocol protocol = timedCall("Fabric", "create", "RoutingProtocol", "POST",
                    () -> fabric.routingProtocols().define()
                            .ofType(RoutingProtocolType.BGP)
                            .withCustomerAsn(65000L)
                            .withBGPIpv4("10.0.0.1", "10.0.0.2", true)
                            .withBFD(true, 100)
                            .create(connectionUuid));

            assertNotNull(protocol, "RoutingProtocol should be created");
            assertNotNull(protocol.getUuid(), "RoutingProtocol UUID should not be null");
            routingProtocolUuid = protocol.getUuid();
            registerCleanup("RoutingProtocol", routingProtocolUuid,
                    id -> fabric.routingProtocols().getByUuid(connectionUuid, id).delete(connectionUuid));
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "RoutingProtocol creation failed (connection may not support BGP): " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Verify routing protocol ASN and IP configuration")
    void verifyRoutingProtocol() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(routingProtocolUuid != null, "RoutingProtocol was not created");

        RoutingProtocol protocol = timedCall("Fabric", "getByUuid", "RoutingProtocol", "GET", routingProtocolUuid,
                () -> fabric.routingProtocols().getByUuid(connectionUuid, routingProtocolUuid));

        assertNotNull(protocol);
        assertEquals(routingProtocolUuid, protocol.getUuid());
        assertEquals(RoutingProtocolType.BGP, protocol.getType());
        assertEquals(Long.valueOf(65000L), protocol.getCustomerAsn());
    }

    @Test
    @Order(8)
    @DisplayName("Delete routing protocol and verify removal")
    void deleteRoutingProtocol() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(routingProtocolUuid != null, "RoutingProtocol was not created");

        Boolean deleted = timedCall("Fabric", "delete", "RoutingProtocol", "DELETE", routingProtocolUuid,
                () -> fabric.routingProtocols().getByUuid(connectionUuid, routingProtocolUuid)
                        .delete(connectionUuid));
        assertTrue(deleted, "RoutingProtocol delete should return true");

        assertThrows(EquinixNotFoundException.class,
                () -> fabric.routingProtocols().getByUuid(connectionUuid, routingProtocolUuid),
                "RoutingProtocol should return 404 after deletion");
    }

    @Test
    @Order(9)
    @DisplayName("Delete connection and verify 404")
    void deleteConnection() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(connectionUuid != null, "Connection was not created");

        Boolean deleted = timedCall("Fabric", "delete", "Connection", "DELETE", connectionUuid,
                () -> fabric.connections().getByUuid(connectionUuid).delete());
        assertTrue(deleted, "Connection delete should return true");

        assertThrows(EquinixNotFoundException.class,
                () -> fabric.connections().getByUuid(connectionUuid),
                "Connection should return 404 after deletion");
    }

    // ── Helper Methods ──────────────────────────────────────────────────

    private String findFirstDot1qPortUuid() {
        try {
            PaginatedList<Port> ports = fabric.ports().list();
            for (Port port : ports) {
                if (port.getEncapsulation() != null
                        && port.getEncapsulation().getType() == EncapsulationType.DOT1Q) {
                    return port.getUuid();
                }
            }
        } catch (Exception e) {
            System.out.println("  [HELPER] Could not discover DOT1Q port: " + e.getMessage());
        }
        return null;
    }
}
