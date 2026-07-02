package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.HealthStatus;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Health endpoint.
 */
class FabricHealthWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("health() returns UP status")
    void healthReturnsUpStatus() {
        wireMock.stubFor(get(urlPathMatching("/fabric/v4/health"))
                .willReturn(okJson(loadFixture("/json/fabric/health_response.json"))));

        HealthStatus health = fabric.health();
        assertNotNull(health);
        assertEquals("https://api.equinix.com/fabric/v4/health", health.getHref());
        assertEquals("4.0", health.getVersion());
        assertEquals("2024.11", health.getRelease());
        assertEquals("UP", health.getState());
        assertNotNull(health.getApiServices());
        assertEquals("/fabric/v4/connections", health.getApiServices().getRoute());
        assertEquals("UP", health.getApiServices().getStatus());
    }

    @Test
    @DisplayName("health() 500 throws EquinixServerException")
    void healthServerError() {
        wireMock.stubFor(get(urlPathMatching("/fabric/v4/health"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Service unavailable\"}]")));

        assertThrows(EquinixServerException.class, () -> fabric.health());
    }
}
