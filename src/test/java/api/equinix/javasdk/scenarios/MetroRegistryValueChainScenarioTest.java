package api.equinix.javasdk.scenarios;

import api.equinix.javasdk.Equinix;
import api.equinix.javasdk.EquinixConfig;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.geo.SpeedOfLightLatency;
import api.equinix.javasdk.design.value.savings.SavingsEstimate;
import api.equinix.javasdk.fabric.model.MetroRegistry;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.internetaccess.model.Ibx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline (WireMock) scenario for the full site-selection value chain a real user runs over ONE
 * {@link Equinix} session:
 *
 * <ol>
 *   <li>load the enriched {@code MetroRegistry} (Fabric metros + EIA per-IBX detail);</li>
 *   <li>feed registry IBXs into {@link SpeedOfLightLatency} to pick a site;</li>
 *   <li>feed the chosen site's metro into the design {@code SavingsCalculator}, whose default
 *       rate-card chain prices the interconnect from live Fabric pricing.</li>
 * </ol>
 *
 * <p>Individual links of this chain are covered elsewhere ({@code MetroRegistryEnrichmentWireMockTest},
 * {@code ValueDefaultRateCardWireMockTest}); this scenario asserts the chain END-TO-END: registry
 * data flows into the calculator input, and all three wire families (Fabric metros, EIA ibxs,
 * Fabric prices) execute over the same shared core with a single OAuth token.</p>
 */
@DisplayName("Enriched MetroRegistry → latency → SavingsCalculator over one session")
class MetroRegistryValueChainScenarioTest extends WireMockTestBase {

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("registry IBX drives latency-based site choice, then a live-priced savings estimate")
    void registryToLatencyToSavings() throws Exception {
        stubPaginatedGet(wireMock, "/fabric/v4/metros", "/json/fabric/paginated_metros_list.json");
        stubPaginatedGet(wireMock, "/internetAccess/v2/ibxs", "/json/internetaccess/paginated_ibxs.json");
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices.json");

        try (Equinix eq = new Equinix(testCredentials(), EquinixConfig.builder()
                .autoLoadMetros(false)
                .enrichMetroRegistry(true)
                .build())) {
            redirectToWireMock(eq.fabric());
            eq.authenticate();

            // Step 1 — enriched registry: Fabric metros + EIA per-IBX geo detail in one load.
            MetroRegistry registry = eq.metroRegistry();
            assertTrue(registry.isEnriched(), "EIA detail merged into the metro registry");

            // Step 2 — latency-based site choice between two candidate IBXs from the registry.
            Ibx sv5 = registry.ibx("SV5").orElseThrow();
            Ibx la4 = registry.ibx("LA4").orElseThrow();
            double rttMillis = SpeedOfLightLatency.roundTrip().millisBetween(sv5, la4);
            assertTrue(rttMillis > 4 && rttMillis < 8,
                    "SV5<->LA4 RTT floor ~5 ms from registry geo detail, was " + rttMillis);

            // The winning site's metro code comes from the registry record, not a hardcoded value.
            MetroCode chosenMetro = MetroCode.valueOf(sv5.getMetroCode());
            assertEquals(MetroCode.SV, chosenMetro);

            // Step 3 — savings estimate for that metro; the default chain prices the
            // interconnect live from fabric.prices() over the SAME session.
            SavingsEstimate estimate = eq.design().savingsCalculator()
                    .egressTerabytes(50)
                    .fromCloud(CloudProviderType.AWS)
                    .inRegion("us-east-1")
                    .viaMetro(chosenMetro)
                    .bandwidthMbps(100)
                    .calculate();

            // Fixture row EVPL_VC_SV_DC_100 (100 Mbps, MRC 250.00) matches the registry-chosen SV metro.
            assertTrue(estimate.isEquinixPriced(), "interconnect priced from the live catalogue");
            assertEquals(0, new BigDecimal("250.00").compareTo(estimate.getEquinixMonthlyCost()));
            assertTrue(estimate.isEgressPriced(), "AWS egress rates resolved from the reference layer");
            assertTrue(estimate.isComplete());
            assertEquals(MetroCode.SV, estimate.getMetro());

            // Wire: all three families executed over the shared core...
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metros")));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .withQueryParam("service.connection.type", equalTo("IA_C")));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .withQueryParam("service.connection.type", equalTo("IA_VC")));
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/prices/search"))
                    .withRequestBody(containing("/type"))
                    .withRequestBody(containing("VIRTUAL_CONNECTION_PRODUCT")));

            // ...with a single OAuth token for the entire chain.
            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));
        }
    }
}
