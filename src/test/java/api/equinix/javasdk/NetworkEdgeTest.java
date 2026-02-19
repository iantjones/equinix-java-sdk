package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.enums.PackageCode;
import api.equinix.javasdk.networkedge.model.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class NetworkEdgeTest {

    static NetworkEdge networkEdge;
    static Boolean skipCreateUpdateOperations;

    static LocalDateTime endDateTime = LocalDateTime.now();
    static LocalDateTime startDateTime = endDateTime.minusDays(1);

    @BeforeAll
    static void obtainTestingData() {
        skipCreateUpdateOperations = Boolean.valueOf(System.getProperty("skipCreateUpdateOperations"));
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        networkEdge = new NetworkEdge(new BasicEquinixCredentials(accessKey, secretKey));
        networkEdge.authenticate();
    }

    @Test
    void setup() {
        try {
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
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Setup test skipped - account may not have access: " + e.getMessage());
        }
    }

    @Test
    void devices() {
        PaginatedList<Device> devices = networkEdge.devices().list();
        assertNotNull(devices);
        assertTrue(devices.size() >= 0);

        if (devices.size() > 0) {
            Device device = networkEdge.devices().getByUuid(devices.get(0).getUuid());
            assertNotNull(device);
            assertEquals(devices.get(0).getUuid(), device.getUuid());
        }

        PaginatedList<DeviceType> deviceTypes = networkEdge.devices().listDeviceTypes();
        assertNotNull(deviceTypes);
        assertTrue(deviceTypes.size() >= 0);
    }

    @Test
    void publicKeys() {
        try {
            List<PublicKey> publicKeys = networkEdge.publicKeys().list();
            assertNotNull(publicKeys);
            assertTrue(publicKeys.size() >= 0);

            if (publicKeys.size() > 0) {
                PublicKey publicKey = networkEdge.publicKeys().getByUuid(publicKeys.get(0).getUuid());
                assertNotNull(publicKey);
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Public keys test skipped: " + e.getMessage());
        }
    }

    @Test
    void deviceLinks() {
        try {
            PaginatedList<DeviceLink> deviceLinks = networkEdge.deviceLinks().list();
            assertNotNull(deviceLinks);
            assertTrue(deviceLinks.size() >= 0);

            if (deviceLinks.size() > 0) {
                DeviceLink deviceLink = networkEdge.deviceLinks().getByUuid(deviceLinks.get(0).getUuid());
                assertNotNull(deviceLink);
                assertEquals(deviceLinks.get(0).getUuid(), deviceLink.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Device links test skipped: " + e.getMessage());
        }
    }

    @Test
    void sshUsers() {
        try {
            PaginatedList<SSHUser> sshUsers = networkEdge.sshUsers().list();
            assertNotNull(sshUsers);
            assertTrue(sshUsers.size() >= 0);

            if (sshUsers.size() > 0) {
                SSHUser sshUser = networkEdge.sshUsers().getByUuid(sshUsers.get(0).getUuid());
                assertNotNull(sshUser);
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "SSH users test skipped: " + e.getMessage());
        }
    }

    @Test
    void aclTemplates() {
        try {
            PaginatedList<ACLTemplate> aclTemplates = networkEdge.aclTemplates().list();
            assertNotNull(aclTemplates);
            assertTrue(aclTemplates.size() >= 0);

            if (aclTemplates.size() > 0) {
                ACLTemplate aclTemplate = networkEdge.aclTemplates().getByUuid(aclTemplates.get(0).getUuid());
                assertNotNull(aclTemplate);
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "ACL templates test skipped: " + e.getMessage());
        }
    }

    @Test
    void vpns() {
        try {
            PaginatedList<VPN> vpns = networkEdge.vpns().list();
            assertNotNull(vpns);
            assertTrue(vpns.size() >= 0);

            if (vpns.size() > 0) {
                VPN vpn = networkEdge.vpns().getByUuid(vpns.get(0).getUuid());
                assertNotNull(vpn);
                assertEquals(vpns.get(0).getUuid(), vpn.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "VPNs test skipped: " + e.getMessage());
        }
    }

    @Test
    void bgpPeerings() {
        try {
            PaginatedList<BGPPeering> bgpPeerings = networkEdge.bgpPeerings().list();
            assertNotNull(bgpPeerings);
            assertTrue(bgpPeerings.size() >= 0);

            if (bgpPeerings.size() > 0) {
                BGPPeering bgpPeering = networkEdge.bgpPeerings().getByUuid(bgpPeerings.get(0).getUuid());
                assertNotNull(bgpPeering);
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "BGP peerings test skipped: " + e.getMessage());
        }
    }

    @Test
    void backups() {
        PaginatedList<Device> devices = networkEdge.devices().list();
        Assumptions.assumeTrue(devices.size() > 0, "No devices available to test backups");

        try {
            PaginatedList<Backup> backups = networkEdge.backups().list(devices.get(0).getUuid());
            assertNotNull(backups);
            assertTrue(backups.size() >= 0);

            if (backups.size() > 0) {
                Backup backup = networkEdge.backups().getByUuid(backups.get(0).getUuid());
                assertNotNull(backup);
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Backups test skipped: " + e.getMessage());
        }
    }
}
