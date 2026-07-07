package api.equinix.javasdk.networkedge;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.networkedge.enums.Vendor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for the {@link Vendor} enum.
 *
 * <p>The Network Edge API returns two wire forms: the display form used by
 * {@code VirtualDeviceType.vendor} (e.g. "Palo Alto Networks") and the uppercase code
 * form used by {@code VirtualDeviceDetailsResponse.deviceTypeVendor} (e.g.
 * "PALO_ALTO_NETWORKS"). Both must map to the same constant, and unknown values must
 * fall back to {@link Vendor#UNKNOWN} rather than failing the read.</p>
 */
class VendorDeserializationTest {

    private static final ObjectMapper objectMapper = Constants.mapper();

    private static Vendor read(String wireValue) throws Exception {
        return objectMapper.readValue("\"" + wireValue + "\"", Vendor.class);
    }

    @ParameterizedTest(name = "display form \"{0}\" -> {1}")
    @CsvSource({
            "Cisco,                CISCO",
            "Juniper Networks,     JUNIPER_NETWORKS",
            "Palo Alto Networks,   PALO_ALTO_NETWORKS",
            "Fortinet,             FORTINET",
            "VERSA Networks,       VERSA_NETWORKS",
            "VMWare,               VMWWARE",
            "Silver Peak,          SILVER_PEAK",
            "Check Point,          CHECK_POINT",
            "Aruba,                ARUBA",
            "Arista,               ARISTA",
            "F5,                   F5",
            "BlueCat,              BLUECAT",
            "Zscaler,              ZSCALER",
            "Aviatrix,             AVIATRIX"
    })
    void displayForm_isDeserialized(String wireValue, Vendor expected) throws Exception {
        assertEquals(expected, read(wireValue));
    }

    @ParameterizedTest(name = "spec code form \"{0}\" -> {1}")
    @CsvSource({
            // deviceTypeVendor enum values from the network-edgev1 spec.
            "CISCO,               CISCO",
            "PALO_ALTO_NETWORKS,  PALO_ALTO_NETWORKS",
            "JUNIPER,             JUNIPER_NETWORKS",
            "FORTINET,            FORTINET",
            "ARISTA,              ARISTA",
            "ARUBA,               ARUBA",
            "F5,                  F5",
            "BLUECAT,             BLUECAT",
            "ZSCALER,             ZSCALER",
            "CHECK_POINT,         CHECK_POINT",
            "VMWARE,              VMWWARE",
            "SILVER_PEAK,         SILVER_PEAK",
            "VERSA_NETWORKS,      VERSA_NETWORKS",
            "AVIATRIX,            AVIATRIX"
    })
    void specCodeForm_isDeserialized(String wireValue, Vendor expected) throws Exception {
        assertEquals(expected, read(wireValue));
    }

    @Test
    void unrecognisedValue_mapsToUnknown() throws Exception {
        assertEquals(Vendor.UNKNOWN, read("Some Future Vendor"));
    }

    @Test
    void serializesToDisplayForm() throws Exception {
        // @JsonValue serializes the display form used in request bodies.
        assertEquals("\"Palo Alto Networks\"", objectMapper.writeValueAsString(Vendor.PALO_ALTO_NETWORKS));
        assertEquals("\"Zscaler\"", objectMapper.writeValueAsString(Vendor.ZSCALER));
    }
}
