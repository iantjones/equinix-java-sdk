package com.eqixiac.equinix.customerportal.wiremock;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.customerportal.model.OrderResponse;
import com.eqixiac.equinix.customerportal.model.WorkVisitLocation;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitCage;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitDetails;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitUpdateDetails;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitUpdateRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitVisitor;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Work Visit orders.
 *
 * <p>Exercises {@code order(...)} (POST {@code /colocations/v2/orders/workVisits}) and
 * {@code update(...)} (PATCH {@code .../{orderId}}); both return the {@code Location}-header order
 * id. Cancellation is via {@code orders().cancel(orderId, reason)}. Also exercises the v1
 * permitted-locations GET ({@code /v1/orders/workvisit/locations}).</p>
 */
class CustomerPortalWorkVisitsWireMockTest extends WireMockTestBase {

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

    @Nested
    @DisplayName("order()")
    class Order {

        @Test
        @DisplayName("POSTs the work visit and returns the Location-header order id")
        void placesOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/workVisits"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Location", "/orders/1-44556677")));

            OrderResponse response = customerPortal.workVisits().order(
                    WorkVisitOrderRequest.builder(
                                    WorkVisitDetails.builder(
                                                    List.of(new WorkVisitCage("SV5:01:000ABC")),
                                                    "2025-03-01T09:00:00Z",
                                                    "2025-03-01T17:00:00Z",
                                                    List.of(WorkVisitVisitor.nonRegistered("David", "Park", "Acme")))
                                            .openCabinet(true)
                                            .build())
                            .description("Quarterly hardware maintenance")
                            .build());

            assertNotNull(response);
            assertEquals("1-44556677", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/workVisits"))
                    .withRequestBody(matchingJsonPath("$.details.cages[0].id", equalTo("SV5:01:000ABC")))
                    .withRequestBody(matchingJsonPath("$.details.visitors[0].firstName", equalTo("David")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Quarterly hardware maintenance"))));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("PATCHes the work visit by id and returns the order id")
        void updatesOrder() {
            wireMock.stubFor(patch(urlPathEqualTo("/colocations/v2/orders/workVisits/1-44556677"))
                    .willReturn(aResponse().withStatus(202)
                            .withHeader("Location", "/orders/1-44556677")));

            OrderResponse response = customerPortal.workVisits().update("1-44556677",
                    WorkVisitUpdateRequest.builder()
                            .details(new WorkVisitUpdateDetails().openCabinet(true))
                            .build());

            assertNotNull(response);
            assertEquals("1-44556677", response.getOrderId());

            wireMock.verify(patchRequestedFor(urlPathEqualTo("/colocations/v2/orders/workVisits/1-44556677")));
        }
    }

    @Nested
    @DisplayName("listLocations()")
    class ListLocations {

        @Test
        @DisplayName("GETs /v1/orders/workvisit/locations and returns the permitted locations")
        void listsLocations() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/workvisit/locations"))
                    .willReturn(okJson("{\"locations\":[{\"ibx\":\"AM1\",\"cages\":[{\"cage\":\"AM1:02:002MC1\","
                            + "\"cageTypes\":[\"Shared\"],\"accounts\":[{\"number\":\"1111\","
                            + "\"name\":\"ABC Network Services\",\"isCreditHold\":false,\"isPOBearing\":true,"
                            + "\"cabinets\":[{\"cabinet\":\"AM1:02:002MC1:0601\",\"cabinetType\":\"Shared\"}]}]}]}]}")));

            List<? extends WorkVisitLocation> locations = customerPortal.workVisits().listLocations();

            assertNotNull(locations);
            assertEquals(1, locations.size());
            assertEquals("AM1", locations.get(0).getIbx());
            assertEquals("AM1:02:002MC1", locations.get(0).getCages().get(0).getCage());
            assertEquals(List.of("Shared"), locations.get(0).getCages().get(0).getCageTypes());
            assertEquals("1111", locations.get(0).getCages().get(0).getAccounts().get(0).getNumber());
            assertEquals("ABC Network Services", locations.get(0).getCages().get(0).getAccounts().get(0).getName());
            assertEquals(false, locations.get(0).getCages().get(0).getAccounts().get(0).getIsCreditHold());
            assertEquals("AM1:02:002MC1:0601",
                    locations.get(0).getCages().get(0).getAccounts().get(0).getCabinets().get(0).getCabinet());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/workvisit/locations"))
                    .withQueryParam("detail", absent())
                    .withQueryParam("ibxs", absent())
                    .withQueryParam("cages", absent()));
        }

        @Test
        @DisplayName("forwards detail/ibxs/cages query params")
        void forwardsQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/workvisit/locations"))
                    .willReturn(okJson("{\"locations\":[{\"ibx\":\"AM1\"}]}")));

            List<? extends WorkVisitLocation> locations = customerPortal.workVisits()
                    .listLocations(true, "AM1,AM2", "AM1:02:002MC1");

            assertNotNull(locations);
            assertEquals("AM1", locations.get(0).getIbx());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/workvisit/locations"))
                    .withQueryParam("detail", equalTo("true"))
                    .withQueryParam("ibxs", equalTo("AM1,AM2"))
                    .withQueryParam("cages", equalTo("AM1:02:002MC1")));
        }

        @Test
        @DisplayName("200 with an empty JSON object returns an empty list")
        void emptyJsonObjectReturnsEmptyList() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/workvisit/locations"))
                    .willReturn(okJson("{}")));

            List<? extends WorkVisitLocation> locations =
                    assertDoesNotThrow(() -> customerPortal.workVisits().listLocations());

            assertNotNull(locations);
            assertTrue(locations.isEmpty());
        }

        @Test
        @DisplayName("204 with no body returns an empty list")
        void noContentReturnsEmptyList() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/workvisit/locations"))
                    .willReturn(aResponse().withStatus(204)));

            List<? extends WorkVisitLocation> locations =
                    assertDoesNotThrow(() -> customerPortal.workVisits().listLocations());

            assertNotNull(locations);
            assertTrue(locations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/colocations/v2/orders/workVisits",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.workVisits().order(
                            WorkVisitOrderRequest.builder(Map.of("cages", List.of())).build()));
        }
    }
}
