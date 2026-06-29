package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.ConnectionService;
import api.equinix.javasdk.customerportal.enums.ConnectorType;
import api.equinix.javasdk.customerportal.enums.CrossConnectMediaType;
import api.equinix.javasdk.customerportal.enums.ProtocolType;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectDeinstallRequest;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ContactUpdate;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectUpdateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.Layer1ASide;
import api.equinix.javasdk.customerportal.model.json.creators.Layer1DeinstallDetail;
import api.equinix.javasdk.customerportal.model.json.creators.Layer1Detail;
import api.equinix.javasdk.customerportal.model.json.creators.Layer1PatchPanel;
import api.equinix.javasdk.customerportal.model.json.creators.Layer1ZSide;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Cross Connect orders.
 *
 * <p>Exercises {@code order(...)} (POST {@code /colocations/v2/orders/crossConnects}),
 * {@code update(...)} (PATCH {@code .../{orderId}}) and {@code deinstall(...)} (POST
 * {@code .../deinstall}). All three return the order id from the {@code Location} header.</p>
 */
class CustomerPortalCrossConnectsWireMockTest extends WireMockTestBase {

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
        @DisplayName("POSTs the order and returns the Location-header order id")
        void placesOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/crossConnects"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Location", "/orders/1-23232322")));

            Layer1ASide aSide = Layer1ASide.builder(
                            new Layer1PatchPanel("PP-A"),
                            ConnectionService.SINGLE_MODE_FIBER,
                            CrossConnectMediaType.SINGLE_MODE_FIBER,
                            ProtocolType.GIGABIT_ETHERNET,
                            ConnectorType.LC)
                    .build();
            Layer1ZSide zSide = Layer1ZSide.withPatchPanel(new Layer1PatchPanel("PP-Z"), ConnectorType.LC);

            OrderResponse response = customerPortal.crossConnects().order(
                    CrossConnectOrderRequest.builder(
                                    List.of(Layer1Detail.builder(aSide, zSide).build()))
                            .customerReferenceId("XC-1042")
                            .description("Primary DB cross connect")
                            .build());

            assertNotNull(response);
            assertEquals("1-23232322", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/crossConnects"))
                    .withRequestBody(matchingJsonPath("$.customerReferenceId", equalTo("XC-1042")))
                    .withRequestBody(matchingJsonPath("$.details[0].aSide.connectionService", equalTo("SINGLE_MODE_FIBER")))
                    .withRequestBody(matchingJsonPath("$.details[0].aSide.protocolType", equalTo("GIGABIT_ETHERNET")))
                    .withRequestBody(matchingJsonPath("$.details[0].zSide.connectorType", equalTo("LC")))
                    .withRequestBody(matchingJsonPath("$.details")));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("PATCHes the order by id and returns the order id")
        void updatesOrder() {
            wireMock.stubFor(patch(urlPathEqualTo("/colocations/v2/orders/crossConnects/1-23232322"))
                    .willReturn(aResponse().withStatus(202)
                            .withHeader("Location", "/orders/1-23232322")));

            OrderResponse response = customerPortal.crossConnects().update("1-23232322",
                    new CrossConnectUpdateRequest(List.of(new ContactUpdate(List.of("jdoe")))));

            assertNotNull(response);
            assertEquals("1-23232322", response.getOrderId());

            wireMock.verify(patchRequestedFor(urlPathEqualTo("/colocations/v2/orders/crossConnects/1-23232322")));
        }
    }

    @Nested
    @DisplayName("deinstall()")
    class Deinstall {

        @Test
        @DisplayName("POSTs the deinstall order and returns the order id")
        void deinstalls() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/crossConnects/deinstall"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Location", "/orders/1-99887766")));

            OrderResponse response = customerPortal.crossConnects().deinstall(
                    CrossConnectDeinstallRequest.builder(
                                    List.of(Layer1DeinstallDetail.of("5-123456")), "2025-01-15")
                            .description("Decommission")
                            .build());

            assertNotNull(response);
            assertEquals("1-99887766", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/crossConnects/deinstall"))
                    .withRequestBody(matchingJsonPath("$.removalDate", equalTo("2025-01-15"))));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/colocations/v2/orders/crossConnects",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.crossConnects().order(
                            CrossConnectOrderRequest.builderRaw(List.of(Map.of("aSide", Map.of("ibx", "SV5")))).build()));
        }
    }
}
