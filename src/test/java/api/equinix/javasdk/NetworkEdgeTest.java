package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.enums.PackageCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NetworkEdgeTest {

    static NetworkEdge networkEdge;

    static LocalDateTime endDateTime = LocalDateTime.now();
    static LocalDateTime startDateTime = endDateTime.minusDays(1);

    @BeforeAll
    static void obtainTestingData() {
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        networkEdge = new NetworkEdge(new BasicEquinixCredentials(accessKey, secretKey));
        networkEdge.authenticate();
    }

    @Test
    void setup() {
        byte[] orderSummary = networkEdge.setup().getOrderSummary(
                RequestBuilder.orderSummary()
                        .withAccountNumber(606828)
                .withMetro(MetroCode.SV)
                .withVendorPackage("CSR1000V")
                .withLicenseType(LicenseType.SUB)
                .withThroughput(500)
                .withThroughputUnit(BandwidthUnit.MBPS)
                .withTermLength(1)
                .withCore(4)
                .withDeviceManagementType(DeviceManagementType.EQUINIX_CONFIGURED)
                .withSoftwarePackage(PackageCode.IPBASE));

        assertNotNull(orderSummary);
    }

    @Test
    void devices() {
    }

    @Test
    void publicKeys() {
    }

    @Test
    void deviceLinks() {
    }

    @Test
    void sshUsers() {
    }

    @Test
    void aclTemplates() {
    }

    @Test
    void vpns() {
    }

    @Test
    void bgpPeerings() {
    }

    @Test
    void backups() {
    }
}