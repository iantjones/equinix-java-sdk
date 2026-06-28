package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.ContractTerm;
import api.equinix.javasdk.customerportal.enums.DimensionUnit;
import api.equinix.javasdk.customerportal.enums.FabricPortSpeed;
import api.equinix.javasdk.customerportal.enums.SecureCabinetContactAvailability;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.ProductAvailability;
import api.equinix.javasdk.customerportal.model.json.creators.CabinetDimension;
import api.equinix.javasdk.customerportal.model.json.creators.CabinetDimensions;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetContact;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetContactPhone;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetOrderItem;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetOrderRequest;
import org.junit.jupiter.api.*;

import java.util.List;

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

            CabinetDimensions dimensions = new CabinetDimensions(
                    new CabinetDimension(600, DimensionUnit.MILLIMETER),
                    new CabinetDimension(1200, DimensionUnit.MILLIMETER),
                    new CabinetDimension(2200, DimensionUnit.MILLIMETER));
            SecureCabinetOrderItem orderItem = new SecureCabinetOrderItem(5.0, true, 2, dimensions, true);

            OrderResponse response = customerPortal.secureCabinets().createOrder(
                    SecureCabinetOrderRequest.builder("128745", "SV5", ContractTerm.TERM_36_MONTHS, orderItem)
                            .customerReference("PO-2024-9981")
                            .technicalContact(new SecureCabinetContact("John", "Smith", "john@smith.com")
                                    .phone(new SecureCabinetContactPhone("4915126449706", SecureCabinetContactAvailability.WORK_HOURS)))
                            .build());

            assertNotNull(response);
            assertEquals("1-126546546546", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/securecabinet/v1/orders"))
                    .withRequestBody(matchingJsonPath("$.accountNumber", equalTo("128745")))
                    .withRequestBody(matchingJsonPath("$.ibxCode", equalTo("SV5")))
                    .withRequestBody(matchingJsonPath("$.contractTerm", equalTo("TERM_36_MONTHS")))
                    .withRequestBody(matchingJsonPath("$.orderItem.numberOfCabinets", equalTo("2")))
                    .withRequestBody(matchingJsonPath("$.orderItem.cabinetDimensions.width.unit", equalTo("MILLIMETER")))
                    .withRequestBody(matchingJsonPath("$.technicalContact.phone.availability", equalTo("WORK_HOURS"))));
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
                            + "\"cabinetDimensions\":{\"width\":{\"value\":600,\"unit\":\"MILLIMETER\"},"
                            + "\"depth\":{\"value\":1200,\"unit\":\"MILLIMETER\"},"
                            + "\"height\":{\"value\":2200,\"unit\":\"MILLIMETER\"}},"
                            + "\"acCircuitConfiguration\":{\"voltage\":220,\"soldAmperage\":8.9,"
                            + "\"phase\":\"SINGLE\",\"receptacle\":\"IEC 60309 1P+N+E\"},"
                            + "\"pduConfiguration\":{\"model\":\"UU30009L\"},"
                            + "\"fabricPortSpeed\":\"SPEED_1_GBPS\"}]")));

            List<? extends ProductAvailability> availabilities =
                    customerPortal.secureCabinets().getProductsAvailability("128745");

            assertNotNull(availabilities);
            assertEquals(1, availabilities.size());
            ProductAvailability availability = availabilities.get(0);
            assertEquals("SV5", availability.getIbx());
            assertEquals(10, availability.getMaximumNumberOfCabinetsToOrder());
            assertEquals(DimensionUnit.MILLIMETER, availability.getCabinetDimensions().getWidth().getUnit());
            assertEquals(600, availability.getCabinetDimensions().getWidth().getValue());
            assertEquals(220, availability.getAcCircuitConfiguration().getVoltage());
            assertEquals("IEC 60309 1P+N+E", availability.getAcCircuitConfiguration().getReceptacle());
            assertEquals("UU30009L", availability.getPduConfiguration().getModel());
            assertEquals(FabricPortSpeed.SPEED_1_GBPS, availability.getFabricPortSpeed());
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
