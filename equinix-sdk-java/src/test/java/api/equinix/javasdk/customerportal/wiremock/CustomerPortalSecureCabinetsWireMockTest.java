package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.ProductAvailability;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetOrderRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Secure Cabinets.
 *
 * <p>Exercises the order submission {@code secureCabinets().createOrder(...)} (POST
 * {@code /securecabinet/v1/orders}) and the availability lookup
 * {@code secureCabinets().getProductsAvailability(accountNumber)} (GET
 * {@code /securecabinet/v1/availability/{accountNumber}}).</p>
 */
class CustomerPortalSecureCabinetsWireMockTest extends WireMockTestBase {

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
    @DisplayName("createOrder()")
    class CreateOrder {

        @Test
        @DisplayName("POSTs to the secure cabinet orders endpoint and returns the order id")
        void createsOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/securecabinet/v1/orders"))
                    .willReturn(aResponse().withStatus(202)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"orderNumber\":\"1-126546546546\"}")));

            OrderResponse response = customerPortal.secureCabinets().createOrder(
                    SecureCabinetOrderRequest.builder("128745", "SV5", "TERM_36_MONTHS",
                                    Map.of("numberOfCabinets", 2, "drawCapacity", 5.0))
                            .customerReference("PO-2024-9981")
                            .build());

            assertNotNull(response);
            assertEquals("1-126546546546", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/securecabinet/v1/orders"))
                    .withRequestBody(matchingJsonPath("$.accountNumber", equalTo("128745")))
                    .withRequestBody(matchingJsonPath("$.ibxCode", equalTo("SV5")))
                    .withRequestBody(matchingJsonPath("$.contractTerm", equalTo("TERM_36_MONTHS"))));
        }
    }

    @Nested
    @DisplayName("getProductsAvailability()")
    class Availability {

        @Test
        @DisplayName("returns the availability list for an account")
        void returnsAvailability() {
            wireMock.stubFor(get(urlPathMatching("/securecabinet/v1/availability/.*"))
                    .willReturn(okJson("[{\"ibx\":\"SV5\",\"maximumNumberOfCabinetsToOrder\":10,"
                            + "\"minimumDrawCapacityPerCabinet\":1.0,\"maximumDrawCapacityPerCabinet\":17.3,"
                            + "\"fabricPortSpeed\":\"1G\"}]")));

            List<? extends ProductAvailability> availabilities =
                    customerPortal.secureCabinets().getProductsAvailability("128745");

            assertNotNull(availabilities);
            assertEquals(1, availabilities.size());
            assertEquals("SV5", availabilities.get(0).getIbx());
            assertEquals(10, availabilities.get(0).getMaximumNumberOfCabinetsToOrder());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/securecabinet/v1/availability/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.secureCabinets().getProductsAvailability("128745"));
        }
    }
}
