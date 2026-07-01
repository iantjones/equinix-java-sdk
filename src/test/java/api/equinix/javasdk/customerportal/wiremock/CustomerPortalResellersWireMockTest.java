package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.Service;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.implementation.Address;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Resellers.
 *
 * <p>The Resellers client exposes single-resource retrieval through
 * {@code getResellerCustomer(accountNumber, customerAccountNumber)} rather than a
 * plain {@code getByUuid(uuid)}; these tests exercise that accessor.</p>
 */
class CustomerPortalResellersWireMockTest extends WireMockTestBase {

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
    @DisplayName("getResellerCustomer()")
    class GetResellerCustomer {

        @Test
        @DisplayName("returns reseller customer for valid account numbers")
        void returnsResellerCustomer() {
            stubSingleton(wireMock, "/v2/resellers/.*",
                    "/json/customerportal/reseller_customer_response.json");

            ResellerCustomer customer = customerPortal.resellers()
                    .getResellerCustomer("256891", "778231");
            assertNotNull(customer);
            assertEquals("778231", customer.getCustomerAccountNumber());
            assertEquals("Northwind Trading Co.", customer.getCustomerAccountName());
            assertEquals("256891", customer.getAccountNumber());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/resellers/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Reseller customer not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.resellers().getResellerCustomer("256891", "invalid"));
        }
    }

    @Nested
    @DisplayName("define(...).create()")
    class Create {

        private static final String ACCOUNT_NUMBER = "256891";
        private static final String CREATE_PATH = "/v2/resellers/" + ACCOUNT_NUMBER + "/customers";

        @Test
        @DisplayName("posts the customerAccountName to the resellers customers path, returning true on 201")
        void postsCustomerName() {
            wireMock.stubFor(post(urlPathEqualTo(CREATE_PATH))
                    .willReturn(aResponse().withStatus(201)));

            Boolean result = customerPortal.resellers()
                    .define(ACCOUNT_NUMBER, "Acme Corporation")
                    .create();

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo(CREATE_PATH))
                    .withRequestBody(matchingJsonPath("$.customerAccountName", equalTo("Acme Corporation"))));
        }

        @Test
        @DisplayName("serializes the notification contact, permitted service and address")
        void postsFullBody() {
            wireMock.stubFor(post(urlPathEqualTo(CREATE_PATH))
                    .willReturn(aResponse().withStatus(200)));

            Boolean result = customerPortal.resellers()
                    .define(ACCOUNT_NUMBER, "Acme Corporation")
                    .withResellerNotificationContact("noc@acme.example")
                    .withPermittedService(Service.NETWORK_EDGE)
                    .withAddress(new Address("123 Main St", "Denver", "CO", "US", "80202"))
                    .create();

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo(CREATE_PATH))
                    .withRequestBody(matchingJsonPath("$.customerAccountName", equalTo("Acme Corporation")))
                    .withRequestBody(matchingJsonPath("$.resellerNotificationContact", equalTo("noc@acme.example")))
                    .withRequestBody(matchingJsonPath("$.permittedServices[0]", equalTo("NETWORK_EDGE")))
                    .withRequestBody(matchingJsonPath("$.address.addressLine1", equalTo("123 Main St")))
                    .withRequestBody(matchingJsonPath("$.address.city", equalTo("Denver")))
                    .withRequestBody(matchingJsonPath("$.address.countryCode", equalTo("US"))));
        }

        @Test
        @DisplayName("409 conflict throws EquinixConflictException")
        void conflictThrows() {
            stubErrorInline(wireMock, "/v2/resellers/.*",
                    409, "[{\"errorCode\":\"ERR-409\",\"errorMessage\":\"Customer already exists\"}]");

            assertThrows(EquinixConflictException.class,
                    () -> customerPortal.resellers()
                            .define(ACCOUNT_NUMBER, "Acme Corporation")
                            .create());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v2/resellers/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.resellers().getResellerCustomer("256891", "778231"));
        }
    }
}
