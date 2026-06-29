package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.ShipmentCarrier;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.json.creators.InboundShipmentDetails;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentUpdateRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Shipment orders.
 *
 * <p>Exercises {@code order(...)} (POST {@code /colocations/v2/orders/shipments}) and
 * {@code update(...)} (PATCH {@code .../{orderId}}); both return the {@code Location}-header order
 * id. Cancellation is via {@code orders().cancel(orderId, reason)}.</p>
 */
class CustomerPortalShipmentsWireMockTest extends WireMockTestBase {

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
        @DisplayName("POSTs the shipment and returns the Location-header order id")
        void placesOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/shipments"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Location", "/orders/1-55667788")));

            OrderResponse response = customerPortal.shipments().order(
                    ShipmentOrderRequest.builder("INBOUND", "2025-02-01T10:00:00Z", "SV5:01:000ABC",
                                    InboundShipmentDetails.builder(ShipmentCarrier.FEDEX, List.of("123ABC"), 4).build())
                            .accountNumber("128745")
                            .description("Server delivery")
                            .build());

            assertNotNull(response);
            assertEquals("1-55667788", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/shipments"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("INBOUND")))
                    .withRequestBody(matchingJsonPath("$.cageId", equalTo("SV5:01:000ABC")))
                    .withRequestBody(matchingJsonPath("$.details.carrier", equalTo("FEDEX")))
                    .withRequestBody(matchingJsonPath("$.details.numberOfBoxes", equalTo("4"))));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("PATCHes the shipment by id and returns the order id")
        void updatesOrder() {
            wireMock.stubFor(patch(urlPathEqualTo("/colocations/v2/orders/shipments/1-55667788"))
                    .willReturn(aResponse().withStatus(202)
                            .withHeader("Location", "/orders/1-55667788")));

            OrderResponse response = customerPortal.shipments().update("1-55667788",
                    ShipmentUpdateRequest.builder().requestedDateTime("2025-02-03T10:00:00Z").build());

            assertNotNull(response);
            assertEquals("1-55667788", response.getOrderId());

            wireMock.verify(patchRequestedFor(urlPathEqualTo("/colocations/v2/orders/shipments/1-55667788")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/colocations/v2/orders/shipments",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.shipments().order(
                            ShipmentOrderRequest.builder("INBOUND", "2025-02-01T10:00:00Z", "SV5:01:000ABC",
                                    Map.of("numberOfBoxes", 1)).build()));
        }
    }
}
