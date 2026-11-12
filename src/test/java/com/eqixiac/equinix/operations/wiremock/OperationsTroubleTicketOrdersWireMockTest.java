package com.eqixiac.equinix.operations.wiremock;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.operations.enums.PhonePreferenceToCall;
import com.eqixiac.equinix.operations.enums.SmartHandsContactType;
import com.eqixiac.equinix.operations.model.OrderResponse;
import com.eqixiac.equinix.operations.model.TroubleTicketOrderLocation;
import com.eqixiac.equinix.operations.model.TroubleTicketType;
import com.eqixiac.equinix.operations.model.json.creators.IbxLocation;
import com.eqixiac.equinix.operations.model.json.creators.TroubleTicketContact;
import com.eqixiac.equinix.operations.model.json.creators.TroubleTicketOrderRequest;
import com.eqixiac.equinix.operations.model.json.creators.TroubleTicketServiceDetails;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the Operations Trouble Ticket Orders v1 client, covering the
 * {@code placeOrder(...)} create ({@code POST /v1/orders/troubleticket}) - the shared request
 * envelope of {@code ibxLocation}, {@code serviceDetails} and {@code contacts}, returning the
 * generated order number from the capitalized {@code OrderNumber} result property.
 */
class OperationsTroubleTicketOrdersWireMockTest extends WireMockTestBase {

    static Operations operations;

    @BeforeAll
    static void setUp() {
        operations = new Operations(testCredentials());
        redirectToWireMock(operations);
        operations.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (operations != null) operations.close();
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

            OrderResponse response = operations.troubleTicketOrders().placeOrder(sampleRequest());

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
                            "/json/operations/trouble_ticket_types_response.json"))));

            List<? extends TroubleTicketType> types = operations.troubleTicketOrders().getTypes();

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
                            "/json/operations/trouble_ticket_types_response.json"))));

            List<? extends TroubleTicketType> types =
                    operations.troubleTicketOrders().getTypes("Network");

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
                            "/json/operations/trouble_ticket_locations_response.json"))));

            List<? extends TroubleTicketOrderLocation> locations =
                    operations.troubleTicketOrders().getLocations();

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
                            "/json/operations/trouble_ticket_locations_response.json"))));

            List<? extends TroubleTicketOrderLocation> locations =
                    operations.troubleTicketOrders().getLocations(true, "AM1", "AM1:01:001MC3");

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
                    401, "{\"errors\":[\"Unauthorized\"],\"ticketId\":\"190619221547671\"}");

            assertThrows(EquinixAuthenticationException.class,
                    () -> operations.troubleTicketOrders().getLocations());
        }

        @Test
        @DisplayName("404 on getTypes() throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/orders/troubleticket/types",
                    404, "{\"errors\":[\"Not found\"],\"ticketId\":\"190619221547671\"}");

            assertThrows(EquinixNotFoundException.class,
                    () -> operations.troubleTicketOrders().getTypes());
        }

        @Test
        @DisplayName("500 on placeOrder() (POST) throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/orders/troubleticket",
                    500, "{\"errors\":[\"Internal server error\"],\"ticketId\":\"190619221547671\"}");

            assertThrows(EquinixServerException.class,
                    () -> operations.troubleTicketOrders().placeOrder(sampleRequest()));
        }
    }
}
