package com.eqixiac.equinix.operations.wiremock;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.operations.model.OrderResponse;
import com.eqixiac.equinix.operations.model.LookupLocation;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitCage;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitDetails;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitOrderRequest;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitUpdateDetails;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitUpdateRequest;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitVisitor;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Operations Work Visit orders.
 *
 * <p>Exercises {@code order(...)} (POST {@code /colocations/v2/orders/workVisits}) and
 * {@code update(...)} (PATCH {@code .../{orderId}}); both return the {@code Location}-header order
 * id. Cancellation is via {@code orders().cancel(orderId, reason)}. Permitted locations come from
 * Lookup v2 ({@code GET /colocations/v2/locations?permissionCode=WORK_VISIT}); Work Visit v1 is EOL.</p>
 */
class OperationsWorkVisitsWireMockTest extends WireMockTestBase {

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

    @Nested
    @DisplayName("order()")
    class Order {

        @Test
        @DisplayName("POSTs the work visit and returns the Location-header order id")
        void placesOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/workVisits"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Location", "/orders/1-44556677")));

            OrderResponse response = operations.workVisits().order(
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

            OrderResponse response = operations.workVisits().update("1-44556677",
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
        @DisplayName("GETs Lookup v2 /colocations/v2/locations with permissionCode=WORK_VISIT (live key workVisits)")
        void listsLocations() {
            // Live shape observed 2026-11-11: the array is keyed by the permission code.
            wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/locations"))
                    .willReturn(okJson("{\"workVisits\":[{\"ibx\":\"AM1\",\"accessRestricted\":false,"
                            + "\"specialPrivilege\":false}]}")));

            List<? extends LookupLocation> locations = operations.workVisits().listLocations();

            assertEquals(1, locations.size());
            assertEquals("AM1", locations.get(0).getIbx());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/locations"))
                    .withQueryParam("permissionCode", equalTo("WORK_VISIT"))
                    .withQueryParam("details", absent()));
            wireMock.verify(0, getRequestedFor(urlPathMatching("/v1/orders/workvisit.*")));
        }

        @Test
        @DisplayName("forwards ibxs/details")
        void forwardsQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/locations"))
                    .willReturn(okJson("{\"workVisits\":[{\"ibx\":\"AM1\"}]}")));

            operations.workVisits().listLocations(List.of("AM1"), true);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/locations"))
                    .withQueryParam("permissionCode", equalTo("WORK_VISIT"))
                    .withQueryParam("ibxs", equalTo("AM1"))
                    .withQueryParam("details", equalTo("true")));
        }

        @Test
        @DisplayName("200 with an empty JSON object returns an empty list")
        void emptyJsonObjectReturnsEmptyList() {
            wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/locations")).willReturn(okJson("{}")));

            List<? extends LookupLocation> locations =
                    assertDoesNotThrow(() -> operations.workVisits().listLocations());

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
                    () -> operations.workVisits().order(
                            WorkVisitOrderRequest.builder(Map.of("cages", List.of())).build()));
        }
    }
}
