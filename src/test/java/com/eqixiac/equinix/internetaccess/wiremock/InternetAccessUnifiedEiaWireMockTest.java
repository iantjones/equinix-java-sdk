package com.eqixiac.equinix.internetaccess.wiremock;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.fabric.model.EiaService;
import com.eqixiac.equinix.fabric.model.IpBlock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.eqixiac.equinix.core.ResponseStubs.stubSingleton;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The Unified EIA API (Fabric v4 {@code /internetAccessServices}, {@code /ipBlocks}) is reachable
 * from {@link InternetAccess} over the same core client.
 */
class InternetAccessUnifiedEiaWireMockTest extends WireMockTestBase {

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

    @Test
    void unifiedServicesGetHitsFabricV4() {
        stubSingleton(wireMock, "/fabric/v4/internetAccessServices/.*", "/json/fabric/eia_service_response.json");

        EiaService service = internetAccess.unifiedServices().getByUuid("a8ba52de-faae-43b5-b0b1-6904d37ee011");

        assertNotNull(service);
        wireMock.verify(getRequestedFor(urlPathEqualTo(
                "/fabric/v4/internetAccessServices/a8ba52de-faae-43b5-b0b1-6904d37ee011")));
        assertSame(internetAccess.unifiedServices(), internetAccess.unifiedServices());
    }

    @Test
    void ipBlocksGetHitsFabricV4() {
        stubSingleton(wireMock, "/fabric/v4/ipBlocks/.*", "/json/fabric/ip_block_response.json");

        IpBlock block = internetAccess.ipBlocks().getByUuid("31fbdb3f-8def-410d-868b-ef920878affb");

        assertNotNull(block);
        wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/ipBlocks/31fbdb3f-8def-410d-868b-ef920878affb")));
    }
}
