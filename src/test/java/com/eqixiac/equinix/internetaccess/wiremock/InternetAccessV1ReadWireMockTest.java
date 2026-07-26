package com.eqixiac.equinix.internetaccess.wiremock;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.enums.AgreementType;
import com.eqixiac.equinix.internetaccess.enums.BillingType;
import com.eqixiac.equinix.internetaccess.enums.ConnectionType;
import com.eqixiac.equinix.internetaccess.enums.OrderState;
import com.eqixiac.equinix.internetaccess.enums.PatchPanelType;
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderCategory;
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderStatus;
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderType;
import com.eqixiac.equinix.internetaccess.enums.Redundancy;
import com.eqixiac.equinix.internetaccess.enums.RoutingProtocolType;
import com.eqixiac.equinix.internetaccess.enums.ServiceOrderType;
import com.eqixiac.equinix.internetaccess.enums.UseCase;
import com.eqixiac.equinix.internetaccess.model.AccountAgreement;
import com.eqixiac.equinix.internetaccess.model.AccountDetails;
import com.eqixiac.equinix.internetaccess.model.Cabinet;
import com.eqixiac.equinix.internetaccess.model.Cage;
import com.eqixiac.equinix.internetaccess.model.Ibx;
import com.eqixiac.equinix.internetaccess.model.OrderDetails;
import com.eqixiac.equinix.internetaccess.model.PatchPanel;
import com.eqixiac.equinix.internetaccess.model.PurchaseOrder;
import com.eqixiac.equinix.internetaccess.model.RoutingProtocolConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.exception.EquinixAuthorizationException;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.exception.EquinixRateLimitException;

import static com.eqixiac.equinix.core.ResponseStubs.stubErrorInline;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    /**
     * One page of a two-page (total 150, limit 100) result set, for paging-crossing tests. The
     * first request always carries {@code offset=0&limit=100} (the SDK's PAGE_LIMIT default);
     * page 2 must be requested at {@code offset=100} (server offset + server limit).
     */
    private static String twoPagePayload(int offset, String dataJson) {
        return "{ \"pagination\": { \"offset\": " + offset + ", \"limit\": 100, \"total\": 150 }, "
                + "\"data\": [" + dataJson + "] }";
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
                                + "\"state\": \"AWAITING_SIGNATURE\", \"signature\": { \"signatory\": \"DELEGATE\", "
                                + "\"delegate\": { \"email\": \"d@e.com\" } } }")));

        OrderDetails order = internetAccess.orders().get("abc-123");

        assertEquals("abc-123", order.getUuid());
        assertEquals("1-9234239473", order.getNumber());
        assertEquals(ServiceOrderType.NEW, order.getType());
        assertEquals(OrderState.AWAITING_SIGNATURE, order.getStatus());
        assertEquals("d@e.com", order.getSignature().getDelegate().getEmail());
    }

    @Test
    void getOrder_deserializesProcessingState() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/orders/sub-1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"href\": \"https://api.equinix.com/internetAccess/v1/orders/sub-1\", "
                                + "\"uuid\": \"sub-1\", \"number\": \"1-9234239474\", \"type\": \"NEW\", "
                                + "\"state\": \"PROCESSING\", \"draft\": false }")));

        OrderDetails order = internetAccess.orders().get("sub-1");

        assertEquals("sub-1", order.getUuid());
        assertEquals(OrderState.PROCESSING, order.getStatus());
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
    void getCages_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/cages"))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(okJson(twoPagePayload(0, "{ \"spaceId\": \"PAGE1_CAGE\" }"))));
        wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/cages"))
                .withQueryParam("offset", equalTo("100"))
                .willReturn(okJson(twoPagePayload(100, "{ \"spaceId\": \"PAGE2_CAGE\" }"))));

        PaginatedList<Cage> cages = internetAccess.cages().list("WA1", "100013200");
        assertEquals(1, cages.size());
        assertTrue(cages.hasNextPage());

        cages.loadAll();

        assertEquals(2, cages.size());
        assertEquals("PAGE1_CAGE", cages.get(0).getSpaceId());
        assertEquals("PAGE2_CAGE", cages.get(1).getSpaceId());
        assertFalse(cages.hasNextPage());

        // Page 2 request: offset advanced, limit carried, original filters re-sent.
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/internetAccess/v1/cages"))
                .withQueryParam("offset", equalTo("100"))
                .withQueryParam("limit", equalTo("100"))
                .withQueryParam("location.ibx", equalTo("WA1"))
                .withQueryParam("account.accountNumber", equalTo("100013200")));
    }

    @Test
    void getCages_forbidden403_throwsEquinixAuthorizationException() {
        stubErrorInline(wireMock, "/internetAccess/v1/cages",
                403, "[{\"errorCode\":\"EQ-3000403\",\"errorMessage\":\"Access denied\"}]");

        assertThrows(EquinixAuthorizationException.class,
                () -> internetAccess.cages().list("WA1", "100013200"));
    }

    @Test
    void getOrder_notFound404_throwsEquinixNotFoundException() {
        stubErrorInline(wireMock, "/internetAccess/v1/orders/missing-uuid",
                404, "[{\"errorCode\":\"EQ-3000404\",\"errorMessage\":\"Order not found\"}]");

        assertThrows(EquinixNotFoundException.class,
                () -> internetAccess.orders().get("missing-uuid"));
    }

    @Test
    void getPurchaseOrder_notFound404_throwsEquinixNotFoundException() {
        stubErrorInline(wireMock, "/internetAccess/v1/accounts/100013200/purchaseOrders/PO-MISSING",
                404, "[{\"errorCode\":\"EQ-3000404\",\"errorMessage\":\"Purchase order not found\"}]");

        assertThrows(EquinixNotFoundException.class,
                () -> internetAccess.purchaseOrders().get("100013200", "PO-MISSING"));
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

    /**
     * The EIA v1 cabinets product-availability lookup
     * ({@code GET /internetAccess/v1/cabinets}, the {@code CabinetsV1} group in
     * {@code apiParams_InternetAccess.json}). Confirms v1 URI/version routing, the optional
     * {@code cage.spaceId}/{@code location.ibx}/{@code account.accountNumber} filter placement, and
     * the read-only {@link Cabinet} deserialization.
     */
    @Nested
    class Cabinets {

        @Test
        void list_noArgs_routesToV1WithNoQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/cabinets"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page("{ \"spaceId\": \"CAB-1\", \"number\": \"0101\", \"patchPanelsCount\": 4, "
                                    + "\"cage\": { \"spaceId\": \"S1\" }, \"location\": { \"ibx\": \"WA1\" }, "
                                    + "\"account\": { \"accountNumber\": \"100013200\" } }"))));

            PaginatedList<Cabinet> cabinets = internetAccess.cabinets().list();

            assertEquals(1, cabinets.size());
            assertEquals("CAB-1", cabinets.get(0).getSpaceId());
            assertEquals("0101", cabinets.get(0).getNumber());
            assertEquals(Integer.valueOf(4), cabinets.get(0).getPatchPanelsCount());
            assertEquals("S1", cabinets.get(0).getCage().getSpaceId());
            assertEquals("WA1", cabinets.get(0).getLocation().getIbx());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/cabinets"))
                    .withQueryParam("cage.spaceId", absent())
                    .withQueryParam("location.ibx", absent())
                    .withQueryParam("account.accountNumber", absent()));
        }

        @Test
        void list_withFilters_sendsAllThreeQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/cabinets"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page("{ \"spaceId\": \"CAB-2\", \"number\": \"0202\", \"patchPanelsCount\": 2, "
                                    + "\"cage\": { \"spaceId\": \"S1\" }, \"location\": { \"ibx\": \"WA1\" }, "
                                    + "\"account\": { \"accountNumber\": \"100013200\" } }"))));

            PaginatedList<Cabinet> cabinets = internetAccess.cabinets().list("S1", "WA1", "100013200");

            assertEquals(1, cabinets.size());
            assertEquals("CAB-2", cabinets.get(0).getSpaceId());
            assertEquals("100013200", cabinets.get(0).getAccount().getAccountNumber());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/cabinets"))
                    .withQueryParam("cage.spaceId", equalTo("S1"))
                    .withQueryParam("location.ibx", equalTo("WA1"))
                    .withQueryParam("account.accountNumber", equalTo("100013200")));
        }

        @Test
        void list_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/cabinets"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(twoPagePayload(0, "{ \"spaceId\": \"PAGE1_CAB\" }"))));
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/cabinets"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(twoPagePayload(100, "{ \"spaceId\": \"PAGE2_CAB\" }"))));

            PaginatedList<Cabinet> cabinets = internetAccess.cabinets().list();
            assertEquals(1, cabinets.size());
            assertTrue(cabinets.hasNextPage());

            cabinets.loadAll();

            assertEquals(2, cabinets.size());
            assertEquals("PAGE1_CAB", cabinets.get(0).getSpaceId());
            assertEquals("PAGE2_CAB", cabinets.get(1).getSpaceId());
            assertFalse(cabinets.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/internetAccess/v1/cabinets"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100")));
        }

        @Test
        void list_unauthorized401_throwsEquinixAuthenticationException() {
            stubErrorInline(wireMock, "/internetAccess/v1/cabinets",
                    401, "[{\"errorCode\":\"EQ-3000401\",\"errorMessage\":\"Authentication failed\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> internetAccess.cabinets().list());
        }
    }

    /**
     * The EIA product-availability IBX inventory. Verifies the version routing across the two
     * Ibxs groups in {@code apiParams_InternetAccess.json}: the v2 list
     * ({@code GET /internetAccess/v2/ibxs}, the {@code Ibxs} group, {@code defaultVersion: 2})
     * driven by {@code availability(...)}, and the v1 single-IBX get
     * ({@code GET /internetAccess/v1/ibxs/{ibx}}, the {@code IbxsV1} group,
     * {@code defaultVersion: 1}) driven by {@code getByCode(...)} — plus the query-parameter
     * placement for each overload.
     */
    @Nested
    class IbxAvailability {

        @Test
        void availability_connectionTypeOnly_v2ListSendsOnlyConnectionType() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page("{ \"href\": \"https://api.equinix.com/internetAccess/ibxs/WA1\", "
                                    + "\"ibxCode\": \"WA1\", \"metroCode\": \"WA\", \"countryCode\": \"PL\", "
                                    + "\"countryName\": \"Poland\", \"region\": \"EMEA\" }"))));

            PaginatedList<Ibx> ibxs = internetAccess.ibxs().availability(ConnectionType.IA_C);

            assertEquals(1, ibxs.size());
            assertEquals("WA1", ibxs.get(0).getIbxCode());
            assertEquals("WA", ibxs.get(0).getMetroCode());

            // v2 route (defaultVersion 2, no requestUri) — only the required connection type,
            // the two optional narrowing params must be absent.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .withQueryParam("service.connection.type", equalTo("IA_C"))
                    .withQueryParam("connection.aside.accessPoint.type", absent())
                    .withQueryParam("asset.type", absent()));
        }

        @Test
        void availability_allFilters_v2ListSendsAllThreeQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(page("{ \"href\": \"https://api.equinix.com/internetAccess/ibxs/SG1\", "
                                    + "\"ibxCode\": \"SG1\", \"metroCode\": \"SG\", \"countryCode\": \"SG\", "
                                    + "\"region\": \"APAC\" }"))));

            PaginatedList<Ibx> ibxs =
                    internetAccess.ibxs().availability(ConnectionType.IA_VC, "COLO", "CABINET");

            assertEquals(1, ibxs.size());
            assertEquals("SG1", ibxs.get(0).getIbxCode());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .withQueryParam("service.connection.type", equalTo("IA_VC"))
                    .withQueryParam("connection.aside.accessPoint.type", equalTo("COLO"))
                    .withQueryParam("asset.type", equalTo("CABINET")));
        }

        @Test
        void getByCode_singleArg_v1GetSendsNoQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v1/ibxs/WA1"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody("{ \"href\": \"https://api.equinix.com/internetAccess/ibxs/WA1\", "
                                    + "\"ibx\": \"WA1\", \"metroCode\": \"WA\", \"metroName\": \"Warsaw\", "
                                    + "\"countryCode\": \"PL\", \"region\": \"EMEA\" }")));

            Ibx ibx = internetAccess.ibxs().getByCode("WA1");

            assertEquals("WA1", ibx.getIbxCode());
            assertEquals("Warsaw", ibx.getMetroName());
            assertEquals("WA", ibx.getMetroCode());

            // v1 route (IbxsV1 group, defaultVersion 1, requestUri {$ibx}); the single-arg
            // overload passes null for both optional filters, so no query string is sent.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/ibxs/WA1"))
                    .withQueryParam("service.connection.type", absent())
                    .withQueryParam("connection.aside.accessPoint.type", absent()));
        }

        @Test
        void availability_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(twoPagePayload(0, "{ \"ibxCode\": \"PAGE1_IBX\" }"))));
            wireMock.stubFor(get(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(twoPagePayload(100, "{ \"ibxCode\": \"PAGE2_IBX\" }"))));

            PaginatedList<Ibx> ibxs = internetAccess.ibxs().availability(ConnectionType.IA_C);
            assertEquals(1, ibxs.size());
            assertTrue(ibxs.hasNextPage());

            ibxs.loadAll();

            assertEquals(2, ibxs.size());
            assertEquals("PAGE1_IBX", ibxs.get(0).getIbxCode());
            assertEquals("PAGE2_IBX", ibxs.get(1).getIbxCode());
            assertFalse(ibxs.hasNextPage());

            // Page 2 request: offset advanced, limit carried, connection-type filter re-sent.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam("service.connection.type", equalTo("IA_C")));
        }

        @Test
        void availability_rateLimited429_throwsEquinixRateLimitException() {
            stubErrorInline(wireMock, "/internetAccess/v2/ibxs",
                    429, "[{\"errorCode\":\"EQ-3000429\",\"errorMessage\":\"Too many requests\"}]");

            assertThrows(EquinixRateLimitException.class,
                    () -> internetAccess.ibxs().availability(ConnectionType.IA_C));
        }

        @Test
        void getByCode_notFound404_throwsEquinixNotFoundException() {
            stubErrorInline(wireMock, "/internetAccess/v1/ibxs/ZZ9",
                    404, "[{\"errorCode\":\"EQ-3000404\",\"errorMessage\":\"IBX not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> internetAccess.ibxs().getByCode("ZZ9"));
        }
    }
}
