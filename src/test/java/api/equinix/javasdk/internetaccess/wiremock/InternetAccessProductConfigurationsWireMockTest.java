package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.enums.BillingType;
import api.equinix.javasdk.internetaccess.enums.Redundancy;
import api.equinix.javasdk.internetaccess.enums.RoutingProtocolType;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.CustomerRouteConfiguration;
import api.equinix.javasdk.internetaccess.model.DedicatedBandwidthConfiguration;
import api.equinix.javasdk.internetaccess.model.DedicatedPortDefaultConfiguration;
import api.equinix.javasdk.internetaccess.model.PortConfiguration;
import api.equinix.javasdk.internetaccess.model.RoutingProtocolConfiguration;
import api.equinix.javasdk.internetaccess.model.VirtualBandwidthConfiguration;
import api.equinix.javasdk.internetaccess.model.VirtualConnectionDefaultConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * WireMock-backed coverage for the Equinix Internet Access (EIA) v1 product / attribute
 * configuration read surface exposed via {@code internetAccess.productConfigurations()}.
 *
 * <p>Every op is a {@code GET} under {@code /internetAccess/v1/<rootUri>} (from the
 * {@code defaultVersion: 1} groups in {@code apiParams_InternetAccess.json}, resolved through the
 * {@code internetAccess/v{version}/{rootUri}/{requestUri}} uriFormat). These tests pin the exact
 * path, verb and query-parameter placement for each overload — including the optional-parameter
 * combinations and the (sometimes deeply-nested) query-param key names the internal clients emit —
 * and confirm the read-only model deserialization.
 */
class InternetAccessProductConfigurationsWireMockTest extends WireMockTestBase {

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

    // --- fixtures ----------------------------------------------------------------------------

    private static String page(String dataJson) {
        return "{ \"pagination\": { \"offset\": 0, \"limit\": 50, \"total\": 1 }, \"data\": [" + dataJson + "] }";
    }

    private static void stubList(String path, String dataJson) {
        wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(path))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(page(dataJson))));
    }

    // --- routingConfigurations ---------------------------------------------------------------

    @Nested
    class RoutingConfigurations {

        @Test
        void byUseCase_sendsUseCaseOnly() {
            stubList("/internetAccess/v1/routingProtocolConfigurations",
                    "{ \"useCase\": \"MAIN\", \"type\": \"SINGLE_PORT\", \"routingProtocol\": { \"type\": \"BGP\" } }");

            PaginatedList<RoutingProtocolConfiguration> configs =
                    internetAccess.productConfigurations().routingConfigurations(UseCase.MAIN);

            assertEquals(1, configs.size());
            assertEquals(UseCase.MAIN, configs.get(0).getUseCase());
            assertEquals(Redundancy.SINGLE_PORT, configs.get(0).getType());
            assertEquals(RoutingProtocolType.BGP, configs.get(0).getRoutingProtocol().getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/routingProtocolConfigurations"))
                    .withQueryParam("useCase", equalTo("MAIN"))
                    .withQueryParam("type", absent()));
        }

        @Test
        void byUseCaseAndType_sendsBothParams() {
            stubList("/internetAccess/v1/routingProtocolConfigurations",
                    "{ \"useCase\": \"BACKUP\", \"type\": \"DUAL_PORT\", \"routingProtocol\": { \"type\": \"STATIC\" } }");

            PaginatedList<RoutingProtocolConfiguration> configs =
                    internetAccess.productConfigurations().routingConfigurations(UseCase.BACKUP, Redundancy.DUAL_PORT);

            assertEquals(1, configs.size());
            assertEquals(Redundancy.DUAL_PORT, configs.get(0).getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/routingProtocolConfigurations"))
                    .withQueryParam("useCase", equalTo("BACKUP"))
                    .withQueryParam("type", equalTo("DUAL_PORT")));
        }
    }

    // --- dedicatedBandwidthConfigurations ----------------------------------------------------

    @Nested
    class DedicatedBandwidthConfigurations {

        @Test
        void byUseCase_sendsUseCaseOnly() {
            stubList("/internetAccess/v1/dedicatedBandwidthConfigurations",
                    "{ \"useCase\": \"MAIN\", \"bandwidth\": 1000, \"billing\": \"FIXED\" }");

            PaginatedList<DedicatedBandwidthConfiguration> configs =
                    internetAccess.productConfigurations().dedicatedBandwidthConfigurations(UseCase.MAIN);

            assertEquals(1, configs.size());
            assertNotNull(configs.get(0));

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/dedicatedBandwidthConfigurations"))
                    .withQueryParam("useCase", equalTo("MAIN"))
                    .withQueryParam("billing", absent())
                    .withQueryParam("connection.aside.accessPoint.port.physicalPort.speed", absent()));
        }

        @Test
        void byUseCaseBillingAndSpeed_sendsAllParams() {
            stubList("/internetAccess/v1/dedicatedBandwidthConfigurations",
                    "{ \"useCase\": \"MAIN\", \"bandwidth\": 1000, \"billing\": \"USAGE_BASED\" }");

            internetAccess.productConfigurations()
                    .dedicatedBandwidthConfigurations(UseCase.MAIN, BillingType.USAGE_BASED, 10000);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/dedicatedBandwidthConfigurations"))
                    .withQueryParam("useCase", equalTo("MAIN"))
                    .withQueryParam("billing", equalTo("USAGE_BASED"))
                    .withQueryParam("connection.aside.accessPoint.port.physicalPort.speed", equalTo("10000")));
        }
    }

    // --- virtualBandwidthConfigurations ------------------------------------------------------

    @Nested
    class VirtualBandwidthConfigurations {

        @Test
        void byUseCase_sendsUseCaseOnly() {
            stubList("/internetAccess/v1/virtualBandwidthConfigurations",
                    "{ \"useCase\": \"MAIN\", \"bandwidth\": 500, \"billing\": \"FIXED\" }");

            PaginatedList<VirtualBandwidthConfiguration> configs =
                    internetAccess.productConfigurations().virtualBandwidthConfigurations(UseCase.MAIN);

            assertEquals(1, configs.size());
            assertNotNull(configs.get(0));

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/virtualBandwidthConfigurations"))
                    .withQueryParam("useCase", equalTo("MAIN"))
                    .withQueryParam("billing", absent()));
        }

        @Test
        void byUseCaseAndBilling_sendsBothParams() {
            stubList("/internetAccess/v1/virtualBandwidthConfigurations",
                    "{ \"useCase\": \"BACKUP\", \"bandwidth\": 500, \"billing\": \"BURST_BASED\" }");

            internetAccess.productConfigurations()
                    .virtualBandwidthConfigurations(UseCase.BACKUP, BillingType.BURST_BASED);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/virtualBandwidthConfigurations"))
                    .withQueryParam("useCase", equalTo("BACKUP"))
                    .withQueryParam("billing", equalTo("BURST_BASED")));
        }
    }

    // --- virtualConnectionDefaultConfigurations ----------------------------------------------

    @Nested
    class VirtualConnectionDefaultConfigurations {

        @Test
        void byIbx_sendsIbxOnly() {
            stubList("/internetAccess/v1/virtualConnectionDefaultConfigurations",
                    "{ \"bandwidth\": 200 }");

            PaginatedList<VirtualConnectionDefaultConfiguration> configs =
                    internetAccess.productConfigurations().virtualConnectionDefaultConfigurations("WA1");

            assertEquals(1, configs.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/virtualConnectionDefaultConfigurations"))
                    .withQueryParam("connection.aside.accessPoint.location.ibx", equalTo("WA1"))
                    .withQueryParam("connection.aside.accessPoint.location.metroCode", absent()));
        }

        @Test
        void byIbxAndMetroCode_sendsBothParams() {
            stubList("/internetAccess/v1/virtualConnectionDefaultConfigurations",
                    "{ \"bandwidth\": 200 }");

            internetAccess.productConfigurations().virtualConnectionDefaultConfigurations("WA1", "WA");

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/virtualConnectionDefaultConfigurations"))
                    .withQueryParam("connection.aside.accessPoint.location.ibx", equalTo("WA1"))
                    .withQueryParam("connection.aside.accessPoint.location.metroCode", equalTo("WA")));
        }
    }

    // --- customerRouteConfigurations ---------------------------------------------------------

    @Nested
    class CustomerRouteConfigurations {

        @Test
        void byUseCase_sendsUseCaseOnly() {
            stubList("/internetAccess/v1/customerRouteConfigurations",
                    "{ \"useCase\": \"MAIN\" }");

            PaginatedList<CustomerRouteConfiguration> configs =
                    internetAccess.productConfigurations().customerRouteConfigurations(UseCase.MAIN);

            assertEquals(1, configs.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/customerRouteConfigurations"))
                    .withQueryParam("useCase", equalTo("MAIN"))
                    .withQueryParam("type", absent())
                    .withQueryParam("routingProtocol.type", absent()));
        }

        @Test
        void byUseCaseTypeAndRoutingProtocolType_sendsAllParams() {
            stubList("/internetAccess/v1/customerRouteConfigurations",
                    "{ \"useCase\": \"MAIN\" }");

            internetAccess.productConfigurations()
                    .customerRouteConfigurations(UseCase.MAIN, Redundancy.DUAL_PORT, RoutingProtocolType.BGP);

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/customerRouteConfigurations"))
                    .withQueryParam("useCase", equalTo("MAIN"))
                    .withQueryParam("type", equalTo("DUAL_PORT"))
                    .withQueryParam("routingProtocol.type", equalTo("BGP")));
        }
    }

    // --- dedicatedPortDefaultConfigurations --------------------------------------------------

    @Nested
    class DedicatedPortDefaultConfigurations {

        @Test
        void byIbx_sendsIbxParam() {
            stubList("/internetAccess/v1/dedicatedPortDefaultConfigurations",
                    "{ \"bandwidth\": 10000 }");

            PaginatedList<DedicatedPortDefaultConfiguration> configs =
                    internetAccess.productConfigurations().dedicatedPortDefaultConfigurations("SG1");

            assertEquals(1, configs.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/dedicatedPortDefaultConfigurations"))
                    .withQueryParam("connection.aside.accessPoint.location.ibx", equalTo("SG1")));
        }
    }

    // --- portConfigurations ------------------------------------------------------------------

    @Nested
    class PortConfigurations {

        @Test
        void byIbxAndUseCase_sendsBothParams() {
            stubList("/internetAccess/v1/portConfigurations",
                    "{ \"useCase\": \"MAIN\" }");

            PaginatedList<PortConfiguration> configs =
                    internetAccess.productConfigurations().portConfigurations("SG1", UseCase.MAIN);

            assertEquals(1, configs.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v1/portConfigurations"))
                    .withQueryParam("connection.aside.accessPoint.port.location.ibx", equalTo("SG1"))
                    .withQueryParam("useCase", equalTo("MAIN")));
        }
    }
}
