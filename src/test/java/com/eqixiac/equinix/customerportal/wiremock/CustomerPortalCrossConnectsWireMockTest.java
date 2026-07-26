package com.eqixiac.equinix.customerportal.wiremock;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.customerportal.enums.ConnectionService;
import com.eqixiac.equinix.customerportal.enums.ConnectorType;
import com.eqixiac.equinix.customerportal.enums.CrossConnectMediaType;
import com.eqixiac.equinix.customerportal.enums.ProtocolType;
import com.eqixiac.equinix.customerportal.model.OrderResponse;
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectDeinstallRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.ContactUpdate;
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectUpdateRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.Layer1ASide;
import com.eqixiac.equinix.customerportal.model.json.creators.Layer1DeinstallDetail;
import com.eqixiac.equinix.customerportal.model.json.creators.Layer1Detail;
import com.eqixiac.equinix.customerportal.model.json.creators.Layer1PatchPanel;
import com.eqixiac.equinix.customerportal.model.json.creators.Layer1ZSide;
import com.eqixiac.equinix.customerportal.model.json.creators.OrderPurchaseOrder;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.*;
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
                            .purchaseOrder(new OrderPurchaseOrder("NEW", "PO-12345", "2020-03-04",
                                    "2021-03-04", 10000.0, "85d9660a-f877-405a-b38e-8e61a4f77f44"))
                            .build());

            assertNotNull(response);
            assertEquals("1-23232322", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/crossConnects"))
                    .withRequestBody(matchingJsonPath("$.customerReferenceId", equalTo("XC-1042")))
                    .withRequestBody(matchingJsonPath("$.details[0].aSide.connectionService", equalTo("SINGLE_MODE_FIBER")))
                    .withRequestBody(matchingJsonPath("$.details[0].aSide.protocolType", equalTo("GIGABIT_ETHERNET")))
                    .withRequestBody(matchingJsonPath("$.details[0].zSide.connectorType", equalTo("LC")))
                    .withRequestBody(matchingJsonPath("$.purchaseOrder.type", equalTo("NEW")))
                    .withRequestBody(matchingJsonPath("$.purchaseOrder.number", equalTo("PO-12345")))
                    .withRequestBody(matchingJsonPath("$.purchaseOrder.startDate", equalTo("2020-03-04")))
                    .withRequestBody(matchingJsonPath("$.purchaseOrder.endDate", equalTo("2021-03-04")))
                    .withRequestBody(matchingJsonPath("$.purchaseOrder.amount", equalTo("10000.0")))
                    .withRequestBody(matchingJsonPath("$.purchaseOrder.attachmentId",
                            equalTo("85d9660a-f877-405a-b38e-8e61a4f77f44")))
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
