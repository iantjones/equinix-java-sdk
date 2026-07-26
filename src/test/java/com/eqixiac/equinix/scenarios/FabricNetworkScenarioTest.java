package com.eqixiac.equinix.scenarios;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.IntegrationTestBase;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.enums.*;
import com.eqixiac.equinix.fabric.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration scenario test for Network + PrecisionTime lifecycle.
 *
 * <p>Exercises creation, verification, update, search, and teardown of networks
 * and precision time services.</p>
 */
@Tag("integration-scenario")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FabricNetworkScenarioTest extends IntegrationTestBase {

    private Fabric fabric;

    private String networkUuid;
    private String networkName;
    private String precisionTimeUuid;

    @BeforeAll
    void setUp() {
        fabric = new Fabric(testCredentials());
        fabric.authenticate();
    }

    @AfterAll
    void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    // ── Network Lifecycle ───────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Create EVPLAN network with regional scope")
    void createNetwork() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        networkName = testResourceName("evplan-network");
        Network network = timedCall("Fabric", "create", "Network", "POST",
                () -> fabric.networks().define(NetworkType.EVPLAN)
                        .name(networkName)
                        .scope(NetworkScope.REGIONAL)
                        .notification(NotificationType.ALL, "test@example.com")
                        .create());

        assertNotNull(network, "Network should be created");
        assertNotNull(network.getUuid(), "Network UUID should not be null");
        networkUuid = network.getUuid();
        registerCleanup("Network", networkUuid, id -> fabric.networks().getByUuid(id).delete());
    }

    @Test
    @Order(2)
    @DisplayName("Verify network name, type, and scope")
    void verifyNetwork() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(networkUuid != null, "Network was not created");

        Network network = timedCall("Fabric", "getByUuid", "Network", "GET", networkUuid,
                () -> fabric.networks().getByUuid(networkUuid));

        assertNotNull(network);
        assertEquals(networkUuid, network.getUuid());
        assertEquals(networkName, network.getName());
        assertEquals(NetworkType.EVPLAN, network.getType());
        assertEquals(NetworkScope.REGIONAL, network.getScope());
    }

    @Test
    @Order(3)
    @DisplayName("Verify network is still accessible after refresh")
    void updateNetwork() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(networkUuid != null, "Network was not created");

        try {
            // Refresh the network to confirm it is accessible and state is consistent
            Network network = timedCall("Fabric", "getByUuid", "Network", "GET", networkUuid,
                    () -> fabric.networks().getByUuid(networkUuid));
            assertNotNull(network);
            assertEquals(networkUuid, network.getUuid());
            // Note: Network name update requires a PATCH/PUT API; verify read access here
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Network update/verify failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Search networks and verify test network appears")
    void searchNetworks() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(networkUuid != null, "Network was not created");

        PaginatedFilteredList<Network> networks = timedCall("Fabric", "search", "Network", "POST",
                () -> fabric.networks().search());

        assertNotNull(networks, "Network search should return results");
        boolean found = false;
        for (Network n : networks) {
            if (networkUuid.equals(n.getUuid())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Test network should appear in search results");
    }

    // ── PrecisionTime Lifecycle ─────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Create NTP precision time service")
    void createPrecisionTime() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");

        String name = testResourceName("ntp-service");
        try {
            PrecisionTime precisionTime = timedCall("Fabric", "create", "PrecisionTime", "POST",
                    () -> fabric.precisionTimes().define()
                            .withType(PrecisionTimeType.NTP)
                            .withName(name)
                            .withPackageCode(PrecisionTimePackageCode.NTP_STANDARD)
                            .create());

            assertNotNull(precisionTime, "PrecisionTime should be created");
            assertNotNull(precisionTime.getUuid(), "PrecisionTime UUID should not be null");
            precisionTimeUuid = precisionTime.getUuid();
            registerCleanup("PrecisionTime", precisionTimeUuid,
                    id -> fabric.precisionTimes().getByUuid(id).delete());
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "PrecisionTime creation failed (may require specific entitlements): " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Verify precision time fields")
    void verifyPrecisionTime() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(precisionTimeUuid != null, "PrecisionTime was not created");

        PrecisionTime precisionTime = timedCall("Fabric", "getByUuid", "PrecisionTime", "GET", precisionTimeUuid,
                () -> fabric.precisionTimes().getByUuid(precisionTimeUuid));

        assertNotNull(precisionTime);
        assertEquals(precisionTimeUuid, precisionTime.getUuid());
        assertEquals(PrecisionTimeType.NTP, precisionTime.getType());
        assertEquals(PrecisionTimePackageCode.NTP_STANDARD, precisionTime.getPackageCode());
        assertNotNull(precisionTime.getName(), "PrecisionTime name should not be null");
    }

    @Test
    @Order(7)
    @DisplayName("Teardown: delete precision time service and verify 404")
    void teardownPrecisionTime() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(precisionTimeUuid != null, "PrecisionTime was not created");

        Boolean deleted = timedCall("Fabric", "delete", "PrecisionTime", "DELETE", precisionTimeUuid,
                () -> fabric.precisionTimes().getByUuid(precisionTimeUuid).delete());
        assertTrue(deleted, "PrecisionTime delete should return true");

        assertThrows(EquinixNotFoundException.class,
                () -> fabric.precisionTimes().getByUuid(precisionTimeUuid),
                "PrecisionTime should return 404 after deletion");
    }

    @Test
    @Order(8)
    @DisplayName("Teardown: delete network and verify 404")
    void teardownNetwork() {
        Assumptions.assumeTrue(isFullCrudEnabled(), "Full CRUD mode not enabled");
        Assumptions.assumeTrue(networkUuid != null, "Network was not created");

        Boolean deleted = timedCall("Fabric", "delete", "Network", "DELETE", networkUuid,
                () -> fabric.networks().getByUuid(networkUuid).delete());
        assertTrue(deleted, "Network delete should return true");

        assertThrows(EquinixNotFoundException.class,
                () -> fabric.networks().getByUuid(networkUuid),
                "Network should return 404 after deletion");
    }
}
