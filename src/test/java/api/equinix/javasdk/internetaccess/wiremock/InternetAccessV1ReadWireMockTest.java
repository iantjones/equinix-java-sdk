package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.enums.AgreementType;
import api.equinix.javasdk.internetaccess.enums.BillingType;
import api.equinix.javasdk.internetaccess.enums.ConnectionType;
import api.equinix.javasdk.internetaccess.enums.OrderState;
import api.equinix.javasdk.internetaccess.enums.PatchPanelType;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderCategory;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderStatus;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderType;
import api.equinix.javasdk.internetaccess.enums.Redundancy;
import api.equinix.javasdk.internetaccess.enums.RoutingProtocolType;
import api.equinix.javasdk.internetaccess.enums.ServiceOrderType;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.AccountAgreement;
import api.equinix.javasdk.internetaccess.model.AccountDetails;
import api.equinix.javasdk.internetaccess.model.Cage;
import api.equinix.javasdk.internetaccess.model.Ibx;
import api.equinix.javasdk.internetaccess.model.OrderDetails;
import api.equinix.javasdk.internetaccess.model.PatchPanel;
import api.equinix.javasdk.internetaccess.model.PurchaseOrder;
import api.equinix.javasdk.internetaccess.model.RoutingProtocolConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock-backed tests for the Equinix Internet Access (EIA) v1 read surfaces — the single-IBX
 * get, accounts/agreements, attribute/default configurations, purchase orders, order history and
 * the product-availability inventory. These verify the v1 URI/version routing (the
 * {@code defaultVersion: 1} groups in {@code apiParams_InternetAccess.json}), path/query parameter
 * placement, and the typed read-model deserialization.
 */
class InternetAccessV1ReadWireMockTest extends WireMockTestBase {

    static InternetAccess internetAccess;

    @BeforeAll
    static void setUp() {
        internetAccess = new InternetAccess(testCredentials());
        redirectToWireMock(internetAccess);
        internetAccess.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (internetAccess != null) internetAccess.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private static String page(String dataJson) {
        return "{ \"pagination\": { \"offset\": 0, \"limit\": 50, \"total\": 1 }, \"data\": [" + dataJson + "] }";
    }

    @Test
    void getIbx_routesToV1AndDeserializes() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/ibxs/WA1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"href\": \"https://api.equinix.com/internetAccess/ibxs/WA1\", \"ibx\": \"WA1\", "
                                + "\"metroCode\": \"WA\", \"metroName\": \"Warsaw\", \"countryCode\": \"PL\", \"region\": \"EMEA\" }")));

        Ibx ibx = internetAccess.ibxs().getByCode("WA1", ConnectionType.IA_C, "COLO");

        assertEquals("WA1", ibx.getIbxCode());
        assertEquals("Warsaw", ibx.getMetroName());
        assertEquals("WA", ibx.getMetroCode());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/ibxs/WA1"))
                .withQueryParam("service.connection.type", equalTo("IA_C"))
                .withQueryParam("connection.aside.accessPoint.type", equalTo("COLO")));
    }

    @Test
    void listAccounts_routesToV1WithRequiredQueryParam() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"accountNumber\": \"100013200\", \"accountName\": \"Acme\", "
                                + "\"billing\": { \"currency\": \"USD\", \"poBearing\": true, \"poExempted\": false, \"signatureRequired\": true } }"))));

        PaginatedList<AccountDetails> accounts = internetAccess.accounts().list("SG1");

        assertEquals(1, accounts.size());
        assertEquals("100013200", accounts.get(0).getAccountNumber());
        assertNotNull(accounts.get(0).getBilling());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/accounts"))
                .withQueryParam("operationalUnits.ibxs.ibx", equalTo("SG1")));
    }

    @Test
    void accountAgreements_combinesPathAndQueryParams() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts/100013200/agreements"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"ibx\": \"WA1\", \"type\": \"MCA_GTC\", \"valid\": true }"))));

        PaginatedList<AccountAgreement> agreements = internetAccess.accounts().agreements("100013200", "WA1");

        assertEquals(1, agreements.size());
        assertEquals(AgreementType.MCA_GTC, agreements.get(0).getType());
        assertEquals(Boolean.TRUE, agreements.get(0).getValid());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/accounts/100013200/agreements"))
                .withQueryParam("ibx", equalTo("WA1")));
    }

    @Test
    void routingConfigurations_routesToV1AndDeserializesTypedEnums() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/routingProtocolConfigurations"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"useCase\": \"MAIN\", \"type\": \"SINGLE_PORT\", "
                                + "\"routingProtocol\": { \"type\": \"BGP\" } }"))));

        PaginatedList<RoutingProtocolConfiguration> configs =
                internetAccess.productConfigurations().routingConfigurations(UseCase.MAIN);

        assertEquals(1, configs.size());
        assertEquals(UseCase.MAIN, configs.get(0).getUseCase());
        assertEquals(Redundancy.SINGLE_PORT, configs.get(0).getType());
        assertEquals(RoutingProtocolType.BGP, configs.get(0).getRoutingProtocol().getType());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/routingProtocolConfigurations"))
                .withQueryParam("useCase", equalTo("MAIN")));
    }

    @Test
    void dedicatedBandwidthConfigurations_sendsOptionalParams() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/dedicatedBandwidthConfigurations"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"useCase\": \"MAIN\", \"bandwidth\": 1000, \"billing\": \"FIXED\" }"))));

        internetAccess.productConfigurations().dedicatedBandwidthConfigurations(UseCase.MAIN, BillingType.FIXED, 10000);

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/dedicatedBandwidthConfigurations"))
                .withQueryParam("useCase", equalTo("MAIN"))
                .withQueryParam("billing", equalTo("FIXED"))
                .withQueryParam("connection.aside.accessPoint.port.physicalPort.speed", equalTo("10000")));
    }

    @Test
    void getPurchaseOrders_combinesPathAndQueryParams() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts/100013200/purchaseOrders"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"type\": \"STANDARD_PURCHASE_ORDER\", \"number\": \"PO-1\", "
                                + "\"amount\": 1234.56, \"draft\": false, \"status\": \"ACTIVE\", \"currency\": \"USD\" }"))));

        PaginatedList<PurchaseOrder> orders =
                internetAccess.purchaseOrders().list("100013200", "WA1", PurchaseOrderCategory.INTERCONNECTION);

        assertEquals(1, orders.size());
        assertEquals(PurchaseOrderType.STANDARD_PURCHASE_ORDER, orders.get(0).getType());
        assertEquals(PurchaseOrderStatus.ACTIVE, orders.get(0).getStatus());
        assertEquals(0, new java.math.BigDecimal("1234.56").compareTo(orders.get(0).getAmount()));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/accounts/100013200/purchaseOrders"))
                .withQueryParam("locations.ibx", equalTo("WA1"))
                .withQueryParam("category", equalTo("INTERCONNECTION")));
    }

    @Test
    void getPurchaseOrder_singleGetByNumber() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/accounts/100013200/purchaseOrders/PO-1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"type\": \"BLANKET_PURCHASE_ORDER\", \"number\": \"PO-1\", \"currency\": \"EUR\", \"draft\": true }")));

        PurchaseOrder order = internetAccess.purchaseOrders().get("100013200", "PO-1");

        assertEquals(PurchaseOrderType.BLANKET_PURCHASE_ORDER, order.getType());
        assertEquals("PO-1", order.getNumber());
    }

    @Test
    void getOrder_singleGetWithFlattenedAllOf() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/orders/abc-123"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"href\": \"https://api.equinix.com/internetAccess/v1/orders/abc-123\", "
                                + "\"uuid\": \"abc-123\", \"number\": \"1-9234239473\", \"type\": \"NEW\", "
                                + "\"status\": \"AWAITING_SIGNATURE\", \"signature\": { \"signatory\": \"DELEGATE\", "
                                + "\"delegate\": { \"email\": \"d@e.com\" } } }")));

        OrderDetails order = internetAccess.orders().get("abc-123");

        assertEquals("abc-123", order.getUuid());
        assertEquals("1-9234239473", order.getNumber());
        assertEquals(ServiceOrderType.NEW, order.getType());
        assertEquals(OrderState.AWAITING_SIGNATURE, order.getStatus());
        assertEquals("d@e.com", order.getSignature().getDelegate().getEmail());
    }

    @Test
    void getOrder_deserializesSubmittedStatus() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/orders/sub-1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"href\": \"https://api.equinix.com/internetAccess/v1/orders/sub-1\", "
                                + "\"uuid\": \"sub-1\", \"number\": \"1-9234239474\", \"type\": \"NEW\", "
                                + "\"status\": \"SUBMITTED\", \"draft\": false }")));

        OrderDetails order = internetAccess.orders().get("sub-1");

        assertEquals("sub-1", order.getUuid());
        assertEquals(OrderState.SUBMITTED, order.getStatus());
    }

    @Test
    void getCages_routesToV1WithRequiredQueryParams() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/cages"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"spaceId\": \"S1\", \"number\": \"C1\", \"cabinetsCount\": 3, "
                                + "\"location\": { \"ibx\": \"WA1\" }, \"account\": { \"accountNumber\": \"100013200\" } }"))));

        PaginatedList<Cage> cages = internetAccess.cages().list("WA1", "100013200");

        assertEquals(1, cages.size());
        assertEquals("S1", cages.get(0).getSpaceId());
        assertEquals(Integer.valueOf(3), cages.get(0).getCabinetsCount());
        assertEquals("WA1", cages.get(0).getLocation().getIbx());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/cages"))
                .withQueryParam("location.ibx", equalTo("WA1"))
                .withQueryParam("account.accountNumber", equalTo("100013200")));
    }

    @Test
    void getPatchPanels_deserializesTypedEnumsAndPorts() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(page("{ \"number\": \"PP1\", \"type\": \"EQUINIX_PROVIDED\", \"prewired\": true, "
                                + "\"availablePorts\": [1,2,3], \"ownedPorts\": [ { \"number\": 1, \"ownershipType\": \"USED\", "
                                + "\"ownerType\": \"CROSS_CONNECT\" } ], \"mediaTypes\": [\"SMF\"], \"dedicatedMediaType\": \"SMF\" }"))));

        PaginatedList<PatchPanel> panels =
                internetAccess.patchPanels().list("WA1", "100013200", "S1", "S2");

        assertEquals(1, panels.size());
        assertEquals(PatchPanelType.EQUINIX_PROVIDED, panels.get(0).getType());
        assertEquals(Boolean.TRUE, panels.get(0).getPrewired());
        assertEquals(3, panels.get(0).getAvailablePorts().size());
        assertFalse(panels.get(0).getOwnedPorts().isEmpty());

        wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/patchPanels"))
                .withQueryParam("location.ibx", equalTo("WA1"))
                .withQueryParam("account.accountNumber", equalTo("100013200"))
                .withQueryParam("cage.spaceId", equalTo("S1"))
                .withQueryParam("cabinet.spaceId", equalTo("S2")));
    }
}
