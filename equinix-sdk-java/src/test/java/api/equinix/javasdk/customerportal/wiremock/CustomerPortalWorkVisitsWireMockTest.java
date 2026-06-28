package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitCage;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitDetails;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitUpdateDetails;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitUpdateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitVisitor;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Work Visit orders.
 *
 * <p>Exercises {@code order(...)} (POST {@code /colocations/v2/orders/workVisits}) and
 * {@code update(...)} (PATCH {@code .../{orderId}}); both return the {@code Location}-header order
 * id. Cancellation is via {@code orders().cancel(orderId, reason)}.</p>
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
