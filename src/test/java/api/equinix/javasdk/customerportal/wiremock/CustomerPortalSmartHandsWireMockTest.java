package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.customerportal.enums.PhonePreferenceToCall;
import api.equinix.javasdk.customerportal.enums.SmartHandsContactType;
import api.equinix.javasdk.customerportal.enums.SmartHandsScheduleType;
import api.equinix.javasdk.customerportal.model.SmartHandResponse;
import api.equinix.javasdk.customerportal.model.SmartHandType;
import api.equinix.javasdk.customerportal.model.SmartHandsLocation;
import api.equinix.javasdk.customerportal.model.json.creators.ContactInfo;
import api.equinix.javasdk.customerportal.model.json.creators.IbxLocation;
import api.equinix.javasdk.customerportal.model.json.creators.ScheduleInfo;
import api.equinix.javasdk.customerportal.model.json.creators.SmartHandsRequestJson;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Smart Hands v1 client, covering the typed
 * POST creates under {@code /v1/orders/smarthands/{type}} (shared request envelope plus a
 * per-type {@code serviceDetails} object) and the locations/types reference GETs.
 */
class CustomerPortalSmartHandsWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    @BeforeAll
    static void setUp() {
        customerPortal = new CustomerPortal(testCredentials());
        redirectToWireMock(customerPortal);
        customerPortal.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (customerPortal != null) customerPortal.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private SmartHandsRequestJson sampleRequest(Map<String, Object> serviceDetails) {
        IbxLocation ibxLocation = new IbxLocation("AM1",
                List.of(new IbxLocation.Cage("AM1:01:001MC3", "12345")));
        List<ContactInfo> contacts = List.of(
                ContactInfo.registered(SmartHandsContactType.ORDERING, "jondoe@test.com"),
                ContactInfo.registered(SmartHandsContactType.NOTIFICATION, "jondoe@test.com"),
                ContactInfo.technical("John Doe", "1111111", PhonePreferenceToCall.ANYTIME));
        ScheduleInfo schedule = new ScheduleInfo(SmartHandsScheduleType.STANDARD);

        return SmartHandsRequestJson.builder(ibxLocation, contacts, schedule, serviceDetails)
                .customerReferenceNumber("RSS41244")
                .build();
    }

    @Test
    @DisplayName("createEquipmentInstall posts to the typed path with serviceDetails")
    void createEquipmentInstall_postsToTypedPathWithServiceDetails() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/orders/smarthands/equipmentInstall"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"orderNumber\":\"1-128726682521\"}")));

        Map<String, Object> serviceDetails = Map.of(
                "deviceLocation", "abc location",
                "scopeOfWork", "Install my equipment");

        SmartHandResponse response = customerPortal.smartHandsRequests().createEquipmentInstall(sampleRequest(serviceDetails));

        assertNotNull(response);
        assertEquals("1-128726682521", response.getOrderNumber());
        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/orders/smarthands/equipmentInstall"))
                .withRequestBody(matchingJsonPath("$.ibxLocation.ibx", equalTo("AM1")))
                .withRequestBody(matchingJsonPath("$.serviceDetails.deviceLocation", equalTo("abc location")))
                .withRequestBody(matchingJsonPath("$.schedule.scheduleType", equalTo("STANDARD"))));
    }

    @Test
    @DisplayName("createCageEscort posts to the cageEscort path")
    void createCageEscort_postsToCageEscortPath() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/orders/smarthands/cageEscort"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"orderNumber\":\"1-999\"}")));

        SmartHandResponse response = customerPortal.smartHandsRequests()
                .createCageEscort(sampleRequest(Map.of("scopeOfWork", "Escort required")));

        assertEquals("1-999", response.getOrderNumber());
        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/orders/smarthands/cageEscort")));
    }

    @Test
    @DisplayName("listTypes returns the supported types")
    void listTypes_returnsSupportedTypes() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/orders/smarthands/types"))
                .willReturn(okJson("{\"smarthands\":[{\"type\":\"EQUIPMENT_INSTALL\","
                        + "\"typeDescription\":\"Equipment Install\"}]}")));

        List<? extends SmartHandType> types = customerPortal.smartHandsRequests().listTypes();

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals("EQUIPMENT_INSTALL", types.get(0).getType());
        assertEquals("Equipment Install", types.get(0).getTypeDescription());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/smarthands/types")));
    }

    @Test
    @DisplayName("listLocations returns the permitted locations")
    void listLocations_returnsPermittedLocations() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/orders/smarthands/locations"))
                .willReturn(okJson("{\"locations\":[{\"ibx\":\"AM1\",\"cages\":[{\"cage\":\"AM1:01:001MC3\","
                        + "\"accounts\":[{\"number\":\"136008\",\"name\":\"Service Corporation\"}]}]}]}")));

        List<? extends SmartHandsLocation> locations = customerPortal.smartHandsRequests().listLocations();

        assertNotNull(locations);
        assertEquals(1, locations.size());
        assertEquals("AM1", locations.get(0).getIbx());
        assertEquals("AM1:01:001MC3", locations.get(0).getCages().get(0).getCage());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/smarthands/locations"))
                .withQueryParam("detail", absent())
                .withQueryParam("ibxs", absent())
                .withQueryParam("cages", absent()));
    }

    @Test
    @DisplayName("listLocations with filters forwards detail/ibxs/cages query params")
    void listLocations_withFiltersForwardsQueryParams() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/orders/smarthands/locations"))
                .willReturn(okJson("{\"locations\":[{\"ibx\":\"AM1\"}]}")));

        List<? extends SmartHandsLocation> locations = customerPortal.smartHandsRequests()
                .listLocations(true, "AM1,AM2", "AM1:02:002MC1");

        assertNotNull(locations);
        assertEquals("AM1", locations.get(0).getIbx());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/smarthands/locations"))
                .withQueryParam("detail", equalTo("true"))
                .withQueryParam("ibxs", equalTo("AM1,AM2"))
                .withQueryParam("cages", equalTo("AM1:02:002MC1")));
    }
}
