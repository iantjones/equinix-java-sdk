package api.equinix.javasdk.core;

import api.equinix.javasdk.Equinix;
import api.equinix.javasdk.EquinixConfig;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.RetryPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies {@link EquinixConfig} defaults and the metro auto-load behaviour it controls.
 */
class EquinixConfigWireMockTest extends WireMockTestBase {

    @BeforeEach
    void clean() {
        resetStubs();
    }

    @AfterEach
    void resetCount() {
        wireMock.resetRequests();
    }

    private void stubMetros() {
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/metros"))
                .willReturn(okJson("{\"pagination\":{\"offset\":0,\"limit\":20,\"total\":0},\"data\":[]}")));
    }

    @Test
    @DisplayName("defaults: production, auto-load metros on, no retry override")
    void defaults() {
        EquinixConfig config = EquinixConfig.defaults();
        assertFalse(config.isSandbox());
        assertTrue(config.isAutoLoadMetros());
        assertNull(config.getRetryPolicy());
    }

    @Test
    @DisplayName("builder overrides only the fields set, leaving the rest at their defaults")
    void builderOverrides() {
        RetryPolicy noRetry = RetryPolicy.none();
        EquinixConfig config = EquinixConfig.builder()
                .autoLoadMetros(false)
                .retryPolicy(noRetry)
                .build();
        assertFalse(config.isAutoLoadMetros());
        assertFalse(config.isSandbox());            // untouched -> default
        assertSame(noRetry, config.getRetryPolicy());
    }

    @Test
    @DisplayName("auto-load (the default) loads the metro catalogue during authenticate()")
    void autoLoad_on() throws Exception {
        stubMetros();
        try (Fabric fabric = new Fabric(testCredentials())) {   // default config -> auto-load on
            redirectToWireMock(fabric);
            fabric.authenticate();

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metros")));
            assertNotNull(fabric.metroRegistry());              // already cached; no second fetch
        }
    }

    @Test
    @DisplayName("autoLoadMetros=false skips the metro load during authenticate()")
    void autoLoad_off() throws Exception {
        stubMetros();
        EquinixConfig config = EquinixConfig.builder().autoLoadMetros(false).build();
        try (Fabric fabric = new Fabric(testCredentials(), config)) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            wireMock.verify(0, getRequestedFor(urlPathEqualTo("/fabric/v4/metros")));
        }
    }

    @Test
    @DisplayName("an Equinix session auto-loads the shared metro catalogue on authenticate()")
    void session_autoLoad() throws Exception {
        stubMetros();
        try (Equinix eq = new Equinix(testCredentials())) {
            redirectToWireMock(eq.fabric());
            eq.authenticate();

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metros")));
        }
    }
}
