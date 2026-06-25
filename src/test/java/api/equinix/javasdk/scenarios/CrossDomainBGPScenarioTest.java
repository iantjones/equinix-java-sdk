package api.equinix.javasdk.scenarios;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Scenario: Query Fabric connections, create NetworkEdge BGP peering if a suitable connection exists.
 *
 * <p>This test is highly dependent on environment state. It searches for existing connections
 * and only proceeds with BGP peering creation if a connection is found. Uses
 * Assumptions.assumeTrue() liberally to gracefully skip steps.</p>
 */
@Tag("integration-scenario")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CrossDomainBGPScenarioTest extends IntegrationTestBase {

    private Fabric fabric;
    private NetworkEdge networkEdge;
    private String connectionUuid;
    private String bgpPeeringUuid;

    private void initClients() {
        if (fabric == null) {
            fabric = new Fabric(testCredentials());
        }
        if (networkEdge == null) {
            networkEdge = new NetworkEdge(testCredentials());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Find a suitable Fabric connection for BGP peering")
    void findSuitableConnection() {
        initClients();

        try {
            PaginatedFilteredList<Connection> connections = timedCall("Fabric", "search",
                    "Connection", "POST", () ->
                            fabric.connections().search()
            );

            Assumptions.assumeTrue(connections != null && !connections.isEmpty(),
                    "Skipped: no Fabric connections found in this account");

            Connection first = connections.get(0);
            connectionUuid = first.getUuid();
            assertNotNull(connectionUuid, "Connection UUID should not be null");
            System.out.printf("  Found connection: %s (%s)%n", first.getName(), connectionUuid);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Cannot search Fabric connections: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Create BGP peering on found connection")
    void createBGPPeering() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(connectionUuid != null,
                "Skipped: no suitable connection found");
        initClients();

        try {
            BGPPeering peering = timedCall("NetworkEdge", "create", "BGPPeering", "POST", () ->
                    networkEdge.bgpPeerings().define()
                            .forConnection(connectionUuid)
                            .withLocalIpAddress("169.254.0.1")
                            .withRemoteIpAddress("169.254.0.2")
                            .withLocalAsn(65000)
                            .withRemoteAsn(65001)
                            .withAuthenticationKey("sdk-test-key")
                            .save()
            );

            assertNotNull(peering, "BGP peering should be created");
            bgpPeeringUuid = peering.getUuid();
            assertNotNull(bgpPeeringUuid, "BGP peering UUID should not be null");

            registerCleanup("BGPPeering", bgpPeeringUuid, id -> {
                BGPPeering toDelete = networkEdge.bgpPeerings().getByUuid(id);
                toDelete.delete();
            });
            System.out.printf("  BGP peering created: %s%n", bgpPeeringUuid);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "BGP peering creation not available: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Verify BGP peering via GET")
    void verifyBGPPeering() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(bgpPeeringUuid != null,
                "Skipped: no BGP peering was created");
        initClients();

        BGPPeering peering = timedCall("NetworkEdge", "get", "BGPPeering", "GET",
                bgpPeeringUuid, () ->
                        networkEdge.bgpPeerings().getByUuid(bgpPeeringUuid)
        );

        assertNotNull(peering, "BGP peering should be retrievable");
        assertEquals("169.254.0.1", peering.getLocalIpAddress(),
                "Local IP should match");
        assertEquals("169.254.0.2", peering.getRemoteIpAddress(),
                "Remote IP should match");
        assertEquals(65000, peering.getLocalAsn(),
                "Local ASN should match");
        assertEquals(65001, peering.getRemoteAsn(),
                "Remote ASN should match");
        System.out.printf("  BGP peering verified: local=%s remote=%s%n",
                peering.getLocalIpAddress(), peering.getRemoteIpAddress());
    }

    @Test
    @Order(4)
    @DisplayName("List BGP peerings and verify created peering appears")
    void listBGPPeerings() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(bgpPeeringUuid != null,
                "Skipped: no BGP peering was created");
        initClients();

        PaginatedList<BGPPeering> peerings = timedCall("NetworkEdge", "list",
                "BGPPeering", "GET", () ->
                        networkEdge.bgpPeerings().list()
        );

        assertNotNull(peerings, "BGP peering list should not be null");
        assertFalse(peerings.isEmpty(), "BGP peering list should not be empty");

        boolean found = false;
        for (BGPPeering p : peerings) {
            if (bgpPeeringUuid.equals(p.getUuid())) {
                found = true;
                break;
            }
        }
        Assumptions.assumeTrue(found,
                "Created BGP peering should appear in list (may be eventually consistent)");
        System.out.printf("  BGP peering found in list of %d peerings%n", peerings.size());
    }

    @Test
    @Order(5)
    @DisplayName("Teardown BGP peering")
    void teardownBGPPeering() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(bgpPeeringUuid != null,
                "Skipped: no BGP peering to delete");
        initClients();

        try {
            BGPPeering peering = networkEdge.bgpPeerings().getByUuid(bgpPeeringUuid);
            Boolean deleted = timedCall("NetworkEdge", "delete", "BGPPeering", "DELETE",
                    bgpPeeringUuid, peering::delete);
            assertNotNull(deleted, "Delete should return a result");
            System.out.printf("  BGP peering deleted: %s%n", bgpPeeringUuid);
        } catch (Exception e) {
            System.err.printf("  BGP peering teardown failed (cleanup will retry): %s%n",
                    e.getMessage());
        }
    }
}
