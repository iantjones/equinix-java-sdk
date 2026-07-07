package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.PhonePreferenceToCall;
import api.equinix.javasdk.customerportal.enums.SmartHandsConnectorType;
import api.equinix.javasdk.customerportal.enums.SmartHandsContactType;
import api.equinix.javasdk.customerportal.enums.SmartHandsJumperType;
import api.equinix.javasdk.customerportal.enums.SmartHandsMediaType;
import api.equinix.javasdk.customerportal.enums.SmartHandsScheduleType;
import api.equinix.javasdk.customerportal.model.json.creators.ContactInfo;
import api.equinix.javasdk.customerportal.model.json.creators.EquipmentInstallDetails;
import api.equinix.javasdk.customerportal.model.json.creators.IbxLocation;
import api.equinix.javasdk.customerportal.model.json.creators.RunJumperCableDetails;
import api.equinix.javasdk.customerportal.model.json.creators.ScheduleInfo;
import api.equinix.javasdk.customerportal.model.json.creators.SmartHandsDevice;
import api.equinix.javasdk.customerportal.model.json.creators.SmartHandsRequestJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Serialization / deserialization coverage for the typed smart hands {@code serviceDetails}
 * creators. Exercises two representative order types: equipment-install (flat boolean/string
 * fields) and run-jumper-cable (enums plus a nested device array).
 */
class SmartHandsServiceDetailsTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = Constants.mapper();
    }

    private static List<ContactInfo> contacts() {
        return List.of(
                ContactInfo.registered(SmartHandsContactType.ORDERING, "jondoe@test.com"),
                ContactInfo.registered(SmartHandsContactType.NOTIFICATION, "jondoe@test.com"),
                ContactInfo.technical("John Doe", "1111111", PhonePreferenceToCall.ANYTIME));
    }

    private static IbxLocation ibxLocation() {
        return new IbxLocation("AM1", List.of(new IbxLocation.Cage("AM1:01:001MC3", "12345")));
    }

    @Test
    void equipmentInstallDetails_serializeWithinRequestEnvelope() throws Exception {
        EquipmentInstallDetails details = EquipmentInstallDetails
                .builder("abc location", false, "abc", false, true, true, true, "Install my equipment")
                .patchingInfo("Patch my equipment")
                .build();

        SmartHandsRequestJson request = SmartHandsRequestJson
                .builder(ibxLocation(), contacts(), new ScheduleInfo(SmartHandsScheduleType.STANDARD), details)
                .customerReferenceNumber("RSS41244")
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertEquals("AM1", json.path("ibxLocation").path("ibx").asText());
        assertEquals("STANDARD", json.path("schedule").path("scheduleType").asText());
        JsonNode sd = json.path("serviceDetails");
        assertEquals("abc location", sd.path("deviceLocation").asText());
        assertEquals("abc", sd.path("installationPoint").asText());
        assertFalse(sd.path("elevationDrawingAttached").asBoolean());
        assertTrue(sd.path("mountHardwareIncluded").asBoolean());
        assertTrue(sd.path("patchDevices").asBoolean());
        assertTrue(sd.path("powerItOn").asBoolean());
        assertEquals("Install my equipment", sd.path("scopeOfWork").asText());
        assertEquals("Patch my equipment", sd.path("patchingInfo").asText());
        // unset optional should be omitted (NON_NULL)
        assertTrue(sd.path("needSupportFromASubmarineCableStationEngineer").isMissingNode());
    }

    @Test
    void equipmentInstallDetails_deserialize() throws Exception {
        String body = "{\"deviceLocation\":\"abc location\",\"elevationDrawingAttached\":false,"
                + "\"installationPoint\":\"abc\",\"installedEquipmentPhotoRequired\":true,"
                + "\"mountHardwareIncluded\":true,\"patchDevices\":false,\"powerItOn\":true,"
                + "\"scopeOfWork\":\"Install my equipment\"}";

        EquipmentInstallDetails details = objectMapper.readValue(body, EquipmentInstallDetails.class);

        assertEquals("abc location", details.getDeviceLocation());
        assertEquals("abc", details.getInstallationPoint());
        assertFalse(details.getElevationDrawingAttached());
        assertTrue(details.getInstalledEquipmentPhotoRequired());
        assertTrue(details.getMountHardwareIncluded());
        assertFalse(details.getPatchDevices());
        assertTrue(details.getPowerItOn());
        assertEquals("Install my equipment", details.getScopeOfWork());
    }

    @Test
    void runJumperCableDetails_serializeEnumsAndDeviceArray() throws Exception {
        RunJumperCableDetails details = RunJumperCableDetails
                .builder("1", "Run my jumper")
                .jumperType(SmartHandsJumperType.PATCH_CABLE)
                .mediaType(SmartHandsMediaType.SINGLE_MODE)
                .connector(SmartHandsConnectorType.LC)
                .cableId("A01-5-10")
                .provideTxRxLightLevels(true)
                .deviceDetails(List.of(new SmartHandsDevice("Device Name", "50", "50")))
                .build();

        JsonNode sd = objectMapper.readTree(objectMapper.writeValueAsString(details));

        assertEquals("1", sd.path("quantity").asText());
        assertEquals("Run my jumper", sd.path("scopeOfWork").asText());
        // enums serialize to their wire (@JsonValue) values, not Java identifiers
        assertEquals("Patch Cable", sd.path("jumperType").asText());
        assertEquals("Single-mode", sd.path("mediaType").asText());
        assertEquals("LC", sd.path("connector").asText());
        assertEquals("A01-5-10", sd.path("cableId").asText());
        assertTrue(sd.path("provideTxRxLightLevels").asBoolean());
        assertEquals("Device Name", sd.path("deviceDetails").get(0).path("name").asText());
        assertEquals("50", sd.path("deviceDetails").get(0).path("slot").asText());
        assertEquals("50", sd.path("deviceDetails").get(0).path("port").asText());
    }

    @Test
    void runJumperCableDetails_deserializeEnumsFromWireValues() throws Exception {
        String body = "{\"quantity\":\"1\",\"scopeOfWork\":\"Run my jumper\",\"jumperType\":\"Patch Cable\","
                + "\"mediaType\":\"Single-mode\",\"connector\":\"LC\",\"cableId\":\"A01-5-10\","
                + "\"provideTxRxLightLevels\":true,"
                + "\"deviceDetails\":[{\"name\":\"Device Name\",\"slot\":\"50\",\"port\":\"50\"}]}";

        RunJumperCableDetails details = objectMapper.readValue(body, RunJumperCableDetails.class);

        assertEquals("1", details.getQuantity());
        assertEquals(SmartHandsJumperType.PATCH_CABLE, details.getJumperType());
        assertEquals(SmartHandsMediaType.SINGLE_MODE, details.getMediaType());
        assertEquals(SmartHandsConnectorType.LC, details.getConnector());
        assertEquals("A01-5-10", details.getCableId());
        assertTrue(details.getProvideTxRxLightLevels());
        assertEquals(1, details.getDeviceDetails().size());
        assertEquals("Device Name", details.getDeviceDetails().get(0).getName());
    }

    @Test
    void mapEscapeHatch_stillSupported() throws Exception {
        SmartHandsRequestJson request = SmartHandsRequestJson
                .builder(ibxLocation(), contacts(), new ScheduleInfo(SmartHandsScheduleType.STANDARD),
                        java.util.Map.of("scopeOfWork", "Ad-hoc work"))
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));
        assertEquals("Ad-hoc work", json.path("serviceDetails").path("scopeOfWork").asText());
    }
}
