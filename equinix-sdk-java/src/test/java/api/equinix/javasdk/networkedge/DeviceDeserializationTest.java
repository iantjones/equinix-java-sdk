package api.equinix.javasdk.networkedge;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.networkedge.enums.DeviceStatus;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link DeviceJson}.
 * Loads the device_response.json fixture and verifies all fields.
 */
class DeviceDeserializationTest {

    private static ObjectMapper objectMapper;
    private static DeviceJson device;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;

        InputStream is = DeviceDeserializationTest.class.getResourceAsStream("/json/networkedge/device_response.json");
        assertNotNull(is, "device_response.json fixture not found on classpath");

        device = objectMapper.readValue(is, DeviceJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("ed7891f4-7a67-11e9-9bea-1681be663d3e", device.getUuid());
    }

    @Test
    void name_isDeserialized() {
        assertEquals("My-CSR1000V-Device", device.getName());
    }

    @Test
    void deviceTypeCode_isDeserialized() {
        assertEquals("CSR1000V", device.getDeviceTypeCode());
    }

    @Test
    void deviceTypeName_isDeserialized() {
        assertEquals("Cisco CSR 1000V", device.getDeviceTypeName());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(DeviceStatus.PROVISIONED, device.getStatus());
    }

    @Test
    void licenseType_isDeserialized() {
        assertEquals(LicenseType.SUB, device.getLicenseType());
    }

    @Test
    void metroCode_isDeserialized() {
        assertEquals(MetroCode.SV, device.getMetroCode());
    }

    @Test
    void metroName_isDeserialized() {
        assertEquals("Silicon Valley", device.getMetroName());
    }

    @Test
    void ibx_isDeserialized() {
        assertEquals("SV5", device.getIbx());
    }

    @Test
    void region_isDeserialized() {
        assertEquals(Region.AMER, device.getRegion());
    }

    @Test
    void throughput_isDeserialized() {
        assertEquals(500.0, device.getThroughput());
    }

    @Test
    void hostName_isDeserialized() {
        assertEquals("csr1000v-sv5", device.getHostName());
    }

    @Test
    void packageCode_isDeserialized() {
        assertEquals("SEC", device.getPackageCode());
    }

    @Test
    void version_isDeserialized() {
        assertEquals("16.09.05", device.getVersion());
    }

    @Test
    void accountNumber_isDeserialized() {
        assertEquals(123456, device.getAccountNumber());
    }

    @Test
    void termLength_isDeserialized() {
        assertEquals(12, device.getTermLength());
    }

    @Test
    void additionalBandwidth_isDeserialized() {
        assertEquals(100, device.getAdditionalBandwidth());
    }

    @Test
    void interfaceCount_isDeserialized() {
        assertEquals(10, device.getInterfaceCount());
    }

    @Test
    void sshIpAddress_isDeserialized() {
        assertEquals("10.0.0.1", device.getSshIpAddress());
    }

    @Test
    void notifications_isDeserialized() {
        assertNotNull(device.getNotifications());
        assertEquals(1, device.getNotifications().size());
        assertEquals("ops@example.com", device.getNotifications().get(0));
    }

    @Test
    void vendorConfig_isDeserialized() {
        assertNotNull(device.getVendorConfig());
    }

    @Test
    void redundantUuid_isDeserialized() {
        assertEquals("fe8902g5-8b78-22f0-0cfa-2792cf774e4f", device.getRedundantUuid());
    }

    @Test
    void asn_isDeserialized() {
        assertEquals(65000, device.getAsn());
    }

    @Test
    void clusterSupported_isDeserialized() {
        assertNotNull(device.getClusterSupported());
        assertFalse(device.getClusterSupported());
    }

    @Test
    void deviceSerialNo_isDeserialized() {
        assertEquals("CSR-SN-12345", device.getDeviceSerialNo());
    }
}
