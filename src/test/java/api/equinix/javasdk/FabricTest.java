package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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

        for(Port port : ports) {
            if(port.getEncapsulation().getType() == EncapsulationType.DOT1Q) {
                portUuid = port.getUuid();
                break;
            }
        }

        Assertions.assertNotNull(portUuid);
    }

    @Test
    void metros() {
        PaginatedList<Metro> metros = fabric.metros().list();
        Assertions.assertTrue(metros.size() > 0);

        Metro metro = fabric.metros().getByMetroCode(MetroCode.LA);
        Assertions.assertEquals(MetroCode.LA, metro.getCode());

        PaginatedList<Metro> myMetros = fabric.metros().list(MetroPresence.MY_PORTS);
        Assertions.assertTrue(myMetros.size() > 0);
    }

    @Test
    void ports() {
        PaginatedList<Port> ports = fabric.ports().list();
        Assertions.assertTrue(ports.size() > 0);

        Port port = fabric.ports().getByUuid(ports.get(0).getUuid());

        Assertions.assertEquals(port.getUuid(), ports.get(0).getUuid());

        PortStatistic portStatistic = fabric.ports().getStatistics(ports.get(0).getUuid(), startDateTime, endDateTime);
        Assertions.assertNotNull(portStatistic.getStats());
    }

    @Test
    void serviceTokens() {
        PaginatedList<ServiceToken> serviceTokens = fabric.serviceTokens().list();
        Assertions.assertTrue(serviceTokens.size() > 0);

        ServiceToken serviceToken = fabric.serviceTokens().getByUuid(serviceTokens.get(0).getUuid());
        Assertions.assertEquals(serviceToken.getUuid(), serviceTokens.get(0).getUuid());

        if(!skipCreateUpdateOperations) {
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
                    .withNotificationEmail("iajones@equinix.com")
                    .create();

            Assertions.assertNotNull(newServiceToken.getUuid());
            Assertions.assertTrue(newServiceToken.delete());
        }
    }

//    @Test
//    void connections() {
//        Pricing pricing = fabric.connections().getPricing(portUuid, MetroCode.LA);
//        Assertions.assertNotNull(pricing.getPriceList());
//    }

    @Test
    void serviceProfiles() {
        PaginatedList<ServiceProfile> serviceProfiles = fabric.serviceProfiles().list();
        Assertions.assertTrue(serviceProfiles.size() > 0);
    }
}