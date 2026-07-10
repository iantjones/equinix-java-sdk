package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.PhonePreferenceToCall;
import api.equinix.javasdk.customerportal.enums.SmartHandsContactType;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.TroubleTicketOrderLocation;
import api.equinix.javasdk.customerportal.model.TroubleTicketType;
import api.equinix.javasdk.customerportal.model.json.creators.IbxLocation;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketContact;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketServiceDetails;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Trouble Ticket Orders v1 client, covering the
 * {@code placeOrder(...)} create ({@code POST /v1/orders/troubleticket}) — the shared request
 * envelope of {@code ibxLocation}, {@code serviceDetails} and {@code contacts}, returning the
 * generated order number from the capitalized {@code OrderNumber} result property.
 */
class CustomerPortalTroubleTicketOrdersWireMockTest extends WireMockTestBase {

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

    private TroubleTicketOrderRequest sampleRequest() {
        IbxLocation ibxLocation = new IbxLocation("AM1",
                List.of(new IbxLocation.Cage("AM1:01:001MC3", "12345")));
        TroubleTicketServiceDetails serviceDetails =
                TroubleTicketServiceDetails.builder("2024-11-10T03:00:00Z", "net01")
                        .additionalDetails("Intermittent packet loss on cross-connect")
                        .build();
        List<TroubleTicketContact> contacts = List.of(
                TroubleTicketContact.registered(SmartHandsContactType.ORDERING, "jondoe@test.com"),
                TroubleTicketContact.registered(SmartHandsContactType.NOTIFICATION, "jondoe@test.com"),
                TroubleTicketContact.technical("John Doe", "1111111", PhonePreferenceToCall.ANYTIME));

        return TroubleTicketOrderRequest.builder(ibxLocation, serviceDetails, contacts)
                .customerReferenceNumber("RSS41244")
                .build();
    }

    @Nested
    @DisplayName("placeOrder()")
    class PlaceOrder {

        @Test
        @DisplayName("POSTs the trouble ticket order and returns the generated order number")
        void placesOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/orders/troubleticket"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"OrderNumber\":\"1-128726682521\"}")));

            OrderResponse response = customerPortal.troubleTicketOrders().placeOrder(sampleRequest());

            assertNotNull(response);
            assertEquals("1-128726682521", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/orders/troubleticket"))
                    .withRequestBody(matchingJsonPath("$.ibxLocation.ibx", equalTo("AM1")))
                    .withRequestBody(matchingJsonPath("$.ibxLocation.cages[0].cage", equalTo("AM1:01:001MC3")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.incidentDateTime", equalTo("2024-11-10T03:00:00Z")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.problemCode", equalTo("net01")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].contactType", equalTo("ORDERING")))
                    .withRequestBody(matchingJsonPath("$.contacts[2].name", equalTo("John Doe")))
                    .withRequestBody(matchingJsonPath("$.customerReferenceNumber", equalTo("RSS41244"))));
        }
    }

    @Nested
    @DisplayName("getTypes()")
    class GetTypes {

        @Test
        @DisplayName("GETs the reference types endpoint with no query params")
        void listsAllTypes() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/troubleticket/types"))
                    .willReturn(okJson(loadFixture(
                            "/json/customerportal/trouble_ticket_types_response.json"))));

            List<? extends TroubleTicketType> types = customerPortal.troubleTicketOrders().getTypes();

            assertNotNull(types);
            assertEquals(2, types.size());
            assertEquals("net01", types.get(1).getCode());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/troubleticket/types"))
                    .withQueryParam("category", absent()));
        }

        @Test
        @DisplayName("GETs the reference types endpoint filtered by category")
        void listsTypesByCategory() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/troubleticket/types"))
                    .willReturn(okJson(loadFixture(
                            "/json/customerportal/trouble_ticket_types_response.json"))));

            List<? extends TroubleTicketType> types =
                    customerPortal.troubleTicketOrders().getTypes("Network");

            assertNotNull(types);
            assertEquals(2, types.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/troubleticket/types"))
                    .withQueryParam("category", equalTo("Network")));
        }
    }

    @Nested
    @DisplayName("getLocations()")
    class GetLocations {

        @Test
        @DisplayName("GETs the reference locations endpoint with no query params")
        void listsAllLocations() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/troubleticket/locations"))
                    .willReturn(okJson(loadFixture(
                            "/json/customerportal/trouble_ticket_locations_response.json"))));

            List<? extends TroubleTicketOrderLocation> locations =
                    customerPortal.troubleTicketOrders().getLocations();

            assertNotNull(locations);
            assertEquals(2, locations.size());
            assertEquals("AM1", locations.get(0).getIbx());
            assertEquals("AM1:01:001MC3", locations.get(0).getCages().get(0).getCage());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/troubleticket/locations"))
                    .withQueryParam("detail", absent())
                    .withQueryParam("ibxs", absent())
                    .withQueryParam("cages", absent()));
        }

        @Test
        @DisplayName("GETs the reference locations endpoint filtered by detail, ibxs and cages")
        void listsLocationsFiltered() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/troubleticket/locations"))
                    .willReturn(okJson(loadFixture(
                            "/json/customerportal/trouble_ticket_locations_response.json"))));

            List<? extends TroubleTicketOrderLocation> locations =
                    customerPortal.troubleTicketOrders().getLocations(true, "AM1", "AM1:01:001MC3");

            assertNotNull(locations);
            assertEquals(2, locations.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/troubleticket/locations"))
                    .withQueryParam("detail", equalTo("true"))
                    .withQueryParam("ibxs", equalTo("AM1"))
                    .withQueryParam("cages", equalTo("AM1:01:001MC3")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 on getLocations() throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/v1/orders/troubleticket/locations",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.troubleTicketOrders().getLocations());
        }

        @Test
        @DisplayName("404 on getTypes() throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/orders/troubleticket/types",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.troubleTicketOrders().getTypes());
        }

        @Test
        @DisplayName("500 on placeOrder() (POST) throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/orders/troubleticket",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.troubleTicketOrders().placeOrder(sampleRequest()));
        }
    }
}
