/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Equinix;
import api.equinix.javasdk.EquinixConfig;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.design.geo.SpeedOfLightLatency;
import api.equinix.javasdk.fabric.model.MetroRegistry;
import api.equinix.javasdk.internetaccess.model.Ibx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the {@code EquinixConfig.enrichMetroRegistry} option: when enabled, the metro-registry
 * load also pulls the EIA per-IBX catalogue ({@code GET /internetAccess/v2/ibxs}, both connection
 * types unioned) over the same client transport and exposes it via {@code MetroRegistry.ibx(...)};
 * when disabled (the default) no EIA call is made; when EIA fails the registry still loads from
 * Fabric alone (best-effort).
 */
@DisplayName("EquinixConfig.enrichMetroRegistry — cross-source metro/IBX enrichment")
class MetroRegistryEnrichmentWireMockTest extends WireMockTestBase {

    static final String METROS_PATH = "/fabric/v4/metros";
    static final String IBXS_PATH = "/internetAccess/v2/ibxs";

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
        stubPaginatedGet(wireMock, METROS_PATH, "/json/fabric/paginated_metros_list.json");
        stubPaginatedGet(wireMock, IBXS_PATH, "/json/internetaccess/paginated_ibxs.json");
    }

    private static EquinixConfig enrichedConfig() {
        return EquinixConfig.builder()
                .autoLoadMetros(false)
                .enrichMetroRegistry(true)
                .build();
    }

    @Test
    @DisplayName("enriched registry merges EIA per-IBX detail and feeds IBX-to-IBX latency")
    void enrichedRegistry() throws Exception {
        try (Fabric fabric = new Fabric(testCredentials(), enrichedConfig())) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            MetroRegistry registry = fabric.metroRegistry();

            assertTrue(registry.size() > 0, "Fabric metros loaded");
            assertTrue(registry.isEnriched(), "EIA detail merged");

            // Case-insensitive per-IBX lookup with full EIA detail.
            Ibx sv5 = registry.ibx("sv5").orElseThrow();
            assertEquals("SV", sv5.getMetroCode());
            assertEquals("US", sv5.getCountryCode());
            assertEquals(37.4029, sv5.getGeoCoordinates().getLatitude());

            // Metro-scoped detail listing (matched on the record's own metroCode).
            assertEquals(2, registry.ibxDetails("SV").size());
            assertEquals(1, registry.ibxDetails("LA").size());
            assertTrue(registry.ibxDetails("ZZ").isEmpty());

            // The point of it all: IBX-to-IBX latency straight from the registry.
            double rtt = SpeedOfLightLatency.roundTrip()
                    .millisBetween(sv5, registry.ibx("LA4").orElseThrow());
            assertTrue(rtt > 4 && rtt < 8, "SV5<->LA4 RTT floor ~5 ms, was " + rtt);

            // Both EIA connection types were queried (the listing is scoped per type).
            wireMock.verify(getRequestedFor(urlPathEqualTo(IBXS_PATH))
                    .withQueryParam("service.connection.type", equalTo("IA_C")));
            wireMock.verify(getRequestedFor(urlPathEqualTo(IBXS_PATH))
                    .withQueryParam("service.connection.type", equalTo("IA_VC")));
        }
    }

    @Test
    @DisplayName("disabled by default: no EIA call is made and the registry is un-enriched")
    void disabledByDefault() throws Exception {
        try (Fabric fabric = new Fabric(testCredentials(),
                EquinixConfig.builder().autoLoadMetros(false).build())) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            MetroRegistry registry = fabric.metroRegistry();

            assertTrue(registry.size() > 0);
            assertFalse(registry.isEnriched());
            assertTrue(registry.ibx("SV5").isEmpty());
            wireMock.verify(0, getRequestedFor(urlPathEqualTo(IBXS_PATH)));
        }
    }

    @Test
    @DisplayName("EIA failure is best-effort: the registry still loads from Fabric alone")
    void eiaFailureIsBestEffort() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo(IBXS_PATH))
                .willReturn(aResponse().withStatus(500).withBody("boom")));

        try (Fabric fabric = new Fabric(testCredentials(), enrichedConfig())) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            MetroRegistry registry = fabric.metroRegistry();

            assertTrue(registry.size() > 0, "Fabric metros still loaded");
            assertFalse(registry.isEnriched());
            assertTrue(registry.ibx("SV5").isEmpty());
        }
    }

    @Test
    @DisplayName("refresh() re-reads both sources in place — new IBXes appear on existing references")
    void refreshReloadsInPlace() throws Exception {
        try (Fabric fabric = new Fabric(testCredentials(), enrichedConfig())) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            MetroRegistry registry = fabric.metroRegistry();
            assertTrue(registry.ibx("SV5").isPresent());
            assertTrue(registry.ibx("DA3").isEmpty(), "DA3 not in the initial catalogue");

            // The live catalogue changes: a new IBX appears in EIA.
            wireMock.stubFor(get(urlPathEqualTo(IBXS_PATH)).willReturn(okJson(
                    "{\"pagination\":{\"offset\":0,\"limit\":100,\"total\":1},\"data\":[" +
                    "{\"ibxCode\":\"DA3\",\"metroCode\":\"DA\",\"countryCode\":\"US\"," +
                    "\"geoCoordinates\":{\"latitude\":32.8,\"longitude\":-96.8}}]}")));

            MetroRegistry refreshed = registry.refresh();

            assertSame(registry, refreshed, "refresh() swaps the snapshot in place");
            assertTrue(registry.ibx("DA3").isPresent(), "new IBX visible after refresh");
            assertTrue(registry.ibx("SV5").isEmpty(), "snapshot fully replaced, not merged");
            assertEquals(32.8, registry.ibx("DA3").orElseThrow().getGeoCoordinates().getLatitude());

            // reloadMetroRegistry() delegates to the same in-place refresh.
            assertSame(registry, fabric.reloadMetroRegistry());
        }
    }

    @Test
    @DisplayName("the option flows through an Equinix session to eq.metroRegistry()")
    void sessionFlow() throws Exception {
        try (Equinix eq = new Equinix(testCredentials(), enrichedConfig())) {
            redirectToWireMock(eq.fabric());
            eq.fabric().authenticate();

            MetroRegistry registry = eq.metroRegistry();

            assertTrue(registry.isEnriched(), "session config reached the shared-core Fabric");
            assertTrue(registry.ibx("LA4").isPresent());
        }
    }
}
