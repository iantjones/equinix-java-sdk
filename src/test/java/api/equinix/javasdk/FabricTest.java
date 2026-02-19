package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class FabricTest {

    static Fabric fabric;
    static String portUuid;
    static Boolean skipCreateUpdateOperations;

    static LocalDateTime endDateTime = LocalDateTime.now();
    static LocalDateTime startDateTime = endDateTime.minusDays(1);

    @BeforeAll
    static void obtainTestingData() {
        skipCreateUpdateOperations = Boolean.valueOf(System.getProperty("skipCreateUpdateOperations"));
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        fabric = new Fabric(new BasicEquinixCredentials(accessKey, secretKey));
        fabric.authenticate();

        PaginatedList<Port> ports = fabric.ports().list();

        for (Port port : ports) {
            if (port.getEncapsulation().getType() == EncapsulationType.DOT1Q) {
                portUuid = port.getUuid();
                break;
            }
        }

        Assertions.assertNotNull(portUuid);
    }

    @Test
    void metros() {
        PaginatedList<Metro> metros = fabric.metros().list();
        assertTrue(metros.size() > 0);

        Metro metro = fabric.metros().getByMetroCode(MetroCode.LA);
        assertEquals(MetroCode.LA, metro.getCode());

        PaginatedList<Metro> myMetros = fabric.metros().list(MetroPresence.MY_PORTS);
        assertTrue(myMetros.size() > 0);
    }

    @Test
    void ports() {
        PaginatedList<Port> ports = fabric.ports().list();
        assertTrue(ports.size() > 0);

        Port port = fabric.ports().getByUuid(ports.get(0).getUuid());
        assertEquals(port.getUuid(), ports.get(0).getUuid());

        PortStatistic portStatistic = fabric.ports().getStatistics(ports.get(0).getUuid(), startDateTime, endDateTime);
        assertNotNull(portStatistic.getStats());
    }

    @Test
    void serviceTokens() {
        PaginatedList<ServiceToken> serviceTokens = fabric.serviceTokens().list();
        assertTrue(serviceTokens.size() > 0);

        ServiceToken serviceToken = fabric.serviceTokens().getByUuid(serviceTokens.get(0).getUuid());
        assertEquals(serviceToken.getUuid(), serviceTokens.get(0).getUuid());

        if (!skipCreateUpdateOperations) {
            ServiceToken newServiceToken = fabric.serviceTokens()
                    .define(Side.A_Side)
                    .ofType(ServiceTokenType.VC_TOKEN)
                    .withExpiry(30)
                    .forConnectionType(ConnectionType.EVPL_VC)
                    .allowRemoteConnection(true)
                    .allowCustomBandwidth(true)
                    .withBandwidthLimit(20)
                    .forAccessPointType(AccessPointType.COLO)
                    .onPortUuid(portUuid)
                    .usingProtocolDot1q(1527)
                    .withNotificationEmail("test@example.com")
                    .create();

            assertNotNull(newServiceToken.getUuid());
            assertTrue(newServiceToken.delete());
        }
    }

    @Test
    void connections() {
        PaginatedFilteredList<Connection> connections = fabric.connections().search();
        assertNotNull(connections);
        assertTrue(connections.size() >= 0);

        if (connections.size() > 0) {
            Connection connection = fabric.connections().getByUuid(connections.get(0).getUuid());
            assertNotNull(connection);
            assertEquals(connections.get(0).getUuid(), connection.getUuid());
        }
    }

    @Test
    void cloudRouters() {
        PaginatedFilteredList<CloudRouter> cloudRouters = fabric.cloudRouters().search();
        assertNotNull(cloudRouters);
        assertTrue(cloudRouters.size() >= 0);

        if (cloudRouters.size() > 0) {
            CloudRouter cloudRouter = fabric.cloudRouters().getByUuid(cloudRouters.get(0).getUuid());
            assertNotNull(cloudRouter);
            assertEquals(cloudRouters.get(0).getUuid(), cloudRouter.getUuid());
        }

        PaginatedList<CloudRouterPackage> packages = fabric.cloudRouters().routerPackages();
        assertNotNull(packages);
        assertTrue(packages.size() >= 0);
    }

    @Test
    void streams() {
        PaginatedList<Stream> streams = fabric.streams().list();
        assertNotNull(streams);
        assertTrue(streams.size() >= 0);

        if (streams.size() > 0) {
            Stream stream = fabric.streams().getByUuid(streams.get(0).getUuid());
            assertNotNull(stream);
            assertEquals(streams.get(0).getUuid(), stream.getUuid());
        }
    }

    @Test
    void routeFilters() {
        PaginatedFilteredList<RouteFilter> routeFilters = fabric.routeFilters().search();
        assertNotNull(routeFilters);
        assertTrue(routeFilters.size() >= 0);

        if (routeFilters.size() > 0) {
            RouteFilter routeFilter = fabric.routeFilters().getByUuid(routeFilters.get(0).getUuid());
            assertNotNull(routeFilter);
            assertEquals(routeFilters.get(0).getUuid(), routeFilter.getUuid());
        }
    }

    @Test
    void networks() {
        PaginatedFilteredList<Network> networks = fabric.networks().search();
        assertNotNull(networks);
        assertTrue(networks.size() >= 0);

        if (networks.size() > 0) {
            Network network = fabric.networks().getByUuid(networks.get(0).getUuid());
            assertNotNull(network);
            assertEquals(networks.get(0).getUuid(), network.getUuid());
        }
    }

    @Test
    void health() {
        HealthStatus healthStatus = fabric.health();
        assertNotNull(healthStatus);
    }

    @Test
    void serviceProfiles() {
        PaginatedList<ServiceProfile> serviceProfiles = fabric.serviceProfiles().list();
        assertTrue(serviceProfiles.size() > 0);
    }
}
