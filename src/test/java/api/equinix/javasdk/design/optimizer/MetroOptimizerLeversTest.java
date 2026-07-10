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

package api.equinix.javasdk.design.optimizer;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.enums.ComplianceZone;
import api.equinix.javasdk.design.optimizer.enums.OptimizationStrategy;
import api.equinix.javasdk.design.optimizer.enums.RedundancyTier;
import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import api.equinix.javasdk.design.optimizer.enums.ScoreCategory;
import api.equinix.javasdk.design.optimizer.enums.WorkloadType;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.RiskFinding;
import api.equinix.javasdk.design.optimizer.model.ScoreComponent;
import api.equinix.javasdk.design.optimizer.model.ScoringWeights;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the {@link MetroOptimizer} configuration surface that
 * {@code MetroOptimizerEngineRunTest} does not: the whole {@code constraints()} sub-builder
 * (exclusions, required regions/metros, compliance, redundancy, budget, latency bound,
 * metro-count caps), custom {@link ScoringWeights}, the soft {@code preferProvider(...)}
 * overloads, the {@code term(...)} pricing lever, and every {@link OptimizationStrategy}.
 *
 * <p>The stubbed {@link FabricGateway} serves four metros — DC, DA, SV in AMER and LD in
 * EMEA — with inter-metro {@code avgLatency} data, and one AWS service profile present in
 * DC and DA only. Each test flips exactly one lever and asserts its observable effect on
 * the candidate set, ranking, risk findings, or pricing inputs.</p>
 */
@DisplayName("MetroOptimizer constraints, weights, preferProvider, term, and strategies")
class MetroOptimizerLeversTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;
    private static final double SV_LAT = 37.3382, SV_LON = -121.8863;
    private static final double LD_LAT = 51.5074, LD_LON = -0.1278;

    private FabricGateway fabric;

    @BeforeEach
    void stubGateway() throws Exception {
        Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(
                connectedMetro("DA", 10.0), connectedMetro("SV", 60.0), connectedMetro("LD", 75.0)));
        Metro da = metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON, List.of(
                connectedMetro("DC", 10.0), connectedMetro("SV", 45.0)));
        Metro sv = metro("SV", "Silicon Valley", Region.AMER, SV_LAT, SV_LON, List.of(
                connectedMetro("DC", 60.0), connectedMetro("DA", 45.0)));
        Metro ld = metro("LD", "London", Region.EMEA, LD_LAT, LD_LON, List.of(
                connectedMetro("DC", 75.0)));

        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da, sv, ld), null, null, null, null));

        // AWS is present in DC and DA, absent in SV and LD (name must contain the
        // CloudProviderType.AWS provider name "Amazon Web Services" to match).
        ServiceProfile awsProfile = mock(ServiceProfile.class);
        when(awsProfile.getUuid()).thenReturn("sp-aws-1");
        when(awsProfile.getName()).thenReturn("Amazon Web Services Direct Connect");
        when(awsProfile.metros()).thenReturn(List.of(
                serviceProfileMetro("DC", "us-east-1"),
                serviceProfileMetro("DA", "us-east-1")));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.of(awsProfile), null, null, null, null));

        fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
    }

    /** A DC-anchored single-site builder with one workload and reference pricing (no HTTP). */
    private MetroOptimizer.Builder dcAnchoredBuilder() {
        return MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .rateCard(ReferenceRateCard.standard());
    }

    private static List<String> codes(OptimizationResult result) {
        return result.getRecommendations().stream()
                .map(r -> r.getMetroId().code())
                .collect(Collectors.toList());
    }

    // ── constraints(): metro and region pruning ──

    @Test
    @DisplayName("excludeMetro(SV) removes SV from the candidate set even with room for it")
    void excludeMetroPrunesCandidate() {
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().excludeMetro(MetroCode.SV).maxMetros(4).done()
                .optimize();

        List<String> codes = codes(result);
        assertEquals(3, codes.size(), "4 metros minus the excluded one: " + codes);
        assertFalse(codes.contains("SV"), "SV must be excluded: " + codes);
        assertTrue(codes.containsAll(List.of("DC", "DA", "LD")));
    }

    @Test
    @DisplayName("excludeMetro(String...) raw-code overload prunes the same way")
    void excludeMetroRawCodeOverload() {
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().excludeMetro("SV", "LD").maxMetros(4).done()
                .optimize();

        List<String> codes = codes(result);
        assertEquals(List.of("DA", "DC"), codes.stream().sorted().collect(Collectors.toList()));
    }

    @Test
    @DisplayName("requiredRegions(EMEA) restricts candidacy to EMEA metros only")
    void requiredRegionsRestrictsToRegion() {
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().requiredRegions(Region.EMEA).done()
                .optimize();

        assertEquals(List.of("LD"), codes(result), "only London is in EMEA");
    }

    @Test
    @DisplayName("excludedRegions(EMEA) removes every EMEA metro while AMER stays eligible")
    void excludedRegionsPrunesRegion() {
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().excludedRegions(Region.EMEA).maxMetros(4).done()
                .optimize();

        List<String> codes = codes(result);
        assertFalse(codes.contains("LD"), "LD (EMEA) must be pruned: " + codes);
        assertEquals(3, codes.size(), "all three AMER metros survive with maxMetros(4): " + codes);
    }

    @Test
    @DisplayName("requireMetro() force-includes a metro the required-provider filter would drop")
    void requireMetroOverridesProviderFilter() {
        // AWS is required and absent in SV, so SV would normally be filtered out
        // (MetroOptimizerEngineRunTest proves that). requireMetro(SV) must restore it.
        OptimizationResult result = dcAnchoredBuilder()
                .requireProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
                .constraints().requireMetro(MetroCode.SV).done()
                .optimize();

        List<String> codes = codes(result);
        assertTrue(codes.contains("SV"), "required metro must appear despite failing the AWS filter: " + codes);
        assertTrue(codes.contains("DC"), codes.toString());
        assertTrue(codes.contains("DA"), codes.toString());
    }

    // ── constraints(): compliance ──

    @Test
    @DisplayName("compliance(EU_GDPR) excludes every non-EMEA metro from candidacy")
    void complianceZoneFiltersRegions() {
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().compliance(ComplianceZone.EU_GDPR).done()
                .optimize();

        assertEquals(List.of("LD"), codes(result), "EU_GDPR allows EMEA only");
    }

    @Test
    @DisplayName("compliance(US_FEDRAMP) keeps AMER metros and drops EMEA")
    void complianceZoneKeepsAllowedRegion() {
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().compliance(ComplianceZone.US_FEDRAMP).maxMetros(4).done()
                .optimize();

        List<String> codes = codes(result);
        assertFalse(codes.contains("LD"), "US_FEDRAMP allows AMER only: " + codes);
        assertEquals(3, codes.size(), codes.toString());
    }

    // ── constraints(): metro-count cap ──

    @Test
    @DisplayName("maxMetros(1) caps the recommendation set at one, keeping the top-ranked metro")
    void maxMetrosCapsSelection() {
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().maxMetros(1).done()
                .optimize();

        assertEquals(1, result.getRecommendations().size());
        assertEquals(MetroId.of(MetroCode.DC), result.primaryMetro().getMetroId(),
                "the DC-anchored site makes DC the top-ranked metro");
    }

    // ── constraints(): budget ──

    @Test
    @DisplayName("monthlyBudget() drives the cost estimate's withinBudget verdict both ways")
    void monthlyBudgetDrivesWithinBudget() {
        OptimizationResult overBudget = dcAnchoredBuilder()
                .constraints().monthlyBudget(1, 10).done()
                .optimize();
        assertFalse(overBudget.getCostEstimate().isWithinBudget(),
                "a $10/mo ceiling cannot cover multi-metro interconnects: "
                        + overBudget.getCostEstimate().getMonthlyTotal());

        OptimizationResult withinBudget = dcAnchoredBuilder()
                .constraints().monthlyBudget(0, 1_000_000).done()
                .optimize();
        assertTrue(withinBudget.getCostEstimate().isWithinBudget());
    }

    // ── constraints(): latency bound ──

    @Test
    @DisplayName("maxLatencyMs() excludes metros beyond the bound to any site while near metros survive")
    void maxLatencyMsExcludesFarMetros() {
        // Site anchored at DC. Fabric avgLatency to the HQ metro: DC=0.5ms (self), DA=10ms,
        // SV=60ms, LD=75ms. A 15ms bound must keep DC and DA and exclude SV and LD from
        // candidacy entirely, even with maxMetros(4) leaving room for all four.
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().maxLatencyMs(15.0).maxMetros(4).done()
                .optimize();

        List<String> codes = codes(result);
        assertEquals(List.of("DA", "DC"), codes.stream().sorted().collect(Collectors.toList()),
                "only metros within 15ms of every site may be recommended: " + codes);

        // Every surviving metro honours the bound, so the risk path stays silent too.
        assertTrue(result.getRiskAssessment().getFindings().stream()
                        .noneMatch(f -> "LATENCY_THRESHOLD".equals(f.getCategory())),
                "no LATENCY_THRESHOLD risk when every candidate passed the hard filter");
    }

    @Test
    @DisplayName("maxLatencyMs() unset excludes nothing: all metros remain candidates")
    void maxLatencyMsUnsetExcludesNothing() {
        // Identical request without the bound: even 75ms-away London stays a candidate.
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().maxMetros(4).done()
                .optimize();

        List<String> codes = codes(result);
        assertEquals(List.of("DA", "DC", "LD", "SV"), codes.stream().sorted().collect(Collectors.toList()),
                "with no latency bound, no metro is excluded on latency: " + codes);
    }

    @Test
    @DisplayName("maxLatencyMs() + requireMetro(): a force-included metro beyond the bound is flagged, not excluded")
    void maxLatencyMsFlagsForcedViolations() {
        // Site anchored at DC. A 5ms bound filters DA (10ms), SV (60ms), and LD (75ms) out of
        // candidacy, but requireMetro(SV) bypasses the filter — the breach then surfaces as a
        // MEDIUM LATENCY_THRESHOLD risk finding against SV.
        OptimizationResult result = dcAnchoredBuilder()
                .constraints().maxLatencyMs(5.0).requireMetro(MetroCode.SV).done()
                .optimize();

        List<String> codes = codes(result);
        assertTrue(codes.contains("SV"), "the required metro must survive the latency filter: " + codes);
        assertFalse(codes.contains("DA"), "DA (10ms) is beyond the 5ms bound and not required: " + codes);

        List<RiskFinding> latencyFindings = result.getRiskAssessment().getFindings().stream()
                .filter(f -> "LATENCY_THRESHOLD".equals(f.getCategory()))
                .collect(Collectors.toList());

        assertFalse(latencyFindings.isEmpty(), "the forced 60ms metro must be flagged");
        assertTrue(latencyFindings.stream().allMatch(f -> f.getSeverity() == RiskSeverity.MEDIUM));
        assertTrue(latencyFindings.stream()
                        .anyMatch(f -> MetroId.of(MetroCode.SV).equals(f.getAffectedMetro())),
                "SV (60ms, force-included) carries the finding: " + latencyFindings);
        assertTrue(latencyFindings.stream()
                        .noneMatch(f -> MetroId.of(MetroCode.DC).equals(f.getAffectedMetro())),
                "DC (0.5ms) is within the bound and must not be flagged");
    }

    // ── constraints(): redundancy ──

    @Test
    @DisplayName("redundancy(MULTI_REGION) over a single-region candidate set raises a HIGH SINGLE_REGION risk")
    void redundancyTierRaisesSingleRegionRisk() {
        OptimizationResult result = dcAnchoredBuilder()
                .constraints()
                .requiredRegions(Region.AMER)
                .redundancy(RedundancyTier.MULTI_REGION)
                .done()
                .optimize();

        assertTrue(result.getRecommendations().size() >= 2, "multiple AMER metros selected");
        RiskFinding singleRegion = result.getRiskAssessment().getFindings().stream()
                .filter(f -> "SINGLE_REGION".equals(f.getCategory()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "expected a SINGLE_REGION finding, got: " + result.getRiskAssessment().getFindings()));
        assertEquals(RiskSeverity.HIGH, singleRegion.getSeverity(),
                "an unmet MULTI_REGION request escalates the single-region finding to HIGH");
        assertTrue(singleRegion.getDescription().contains("MULTI_REGION"), singleRegion.getDescription());
    }

    // ── preferProvider(...) and scoringWeights(...) ──

    @Test
    @DisplayName("preferProvider(CloudProviderType) is soft: metros without the provider stay candidates")
    void preferProviderKeepsNonMatchingMetros() {
        OptimizationResult result = dcAnchoredBuilder()
                .preferProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
                .constraints().maxMetros(4).done()
                .optimize();

        List<String> codes = codes(result);
        assertTrue(codes.contains("SV"), "SV lacks AWS but a preference never filters: " + codes);
        assertTrue(codes.contains("LD"), "LD lacks AWS but a preference never filters: " + codes);

        // Availability is still reported truthfully per metro.
        MetroRecommendation dc = result.getRecommendations().stream()
                .filter(r -> r.getMetroId().code().equals("DC")).findFirst().orElseThrow();
        assertTrue(dc.getAvailableProviders().stream()
                .anyMatch(p -> p.isAvailable() && "Amazon Web Services".equals(p.getProviderLabel())));
        MetroRecommendation sv = result.getRecommendations().stream()
                .filter(r -> r.getMetroId().code().equals("SV")).findFirst().orElseThrow();
        assertTrue(sv.getAvailableProviders().stream()
                .noneMatch(api.equinix.javasdk.design.optimizer.model.ProviderAvailability::isAvailable));
    }

    @Test
    @DisplayName("preferProvider(String) service-profile-name overload resolves availability by name")
    void preferProviderByProfileName() {
        OptimizationResult result = dcAnchoredBuilder()
                .preferProvider("Amazon Web Services").done()
                .constraints().maxMetros(4).done()
                .optimize();

        List<String> codes = codes(result);
        assertEquals(4, codes.size(), "no metro is filtered by a soft name preference: " + codes);

        MetroRecommendation da = result.getRecommendations().stream()
                .filter(r -> r.getMetroId().code().equals("DA")).findFirst().orElseThrow();
        assertTrue(da.getAvailableProviders().stream()
                        .anyMatch(p -> p.isAvailable() && "sp-aws-1".equals(p.getServiceProfileUuid())),
                "the name-matched profile's availability must surface for DA");
    }

    @Test
    @DisplayName("scoringWeights() re-ranks: the same request crowns a different primary under opposite weight profiles")
    void scoringWeightsFlipRanking() {
        // Site anchored at SV; AWS (soft preference) is available in DC and DA but not SV.
        // Latency-only weights must crown SV (0.5ms self-latency)...
        ScoringWeights latencyOnly = ScoringWeights.builder()
                .latencyWeight(1.0)
                .providerCoverageWeight(0.0)
                .costWeight(0.0)
                .redundancyWeight(0.0)
                .complianceWeight(0.0)
                .build();
        OptimizationResult latencyLed = MetroOptimizer.builder(fabric)
                .addSite("SV Office").nearestMetro(MetroCode.SV).headcount(500).done()
                .preferProvider(CloudProviderType.AWS).done()
                .scoringWeights(latencyOnly)
                .rateCard(ReferenceRateCard.standard())
                .optimize();
        assertEquals("SV", latencyLed.primaryMetro().getMetroId().code(),
                "latency-only weights must crown the 0.5ms self-latency metro");

        // ...while provider-only weights must dethrone SV (no AWS) for an AWS metro.
        ScoringWeights providerOnly = ScoringWeights.builder()
                .latencyWeight(0.0)
                .providerCoverageWeight(1.0)
                .costWeight(0.0)
                .redundancyWeight(0.0)
                .complianceWeight(0.0)
                .build();
        OptimizationResult providerLed = MetroOptimizer.builder(fabric)
                .addSite("SV Office").nearestMetro(MetroCode.SV).headcount(500).done()
                .preferProvider(CloudProviderType.AWS).done()
                .scoringWeights(providerOnly)
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        String primary = providerLed.primaryMetro().getMetroId().code();
        assertTrue(primary.equals("DC") || primary.equals("DA"),
                "provider-only weights must promote an AWS metro over SV, got " + primary);
        assertNotEquals("SV", primary);
    }

    // ── term(...) ──

    @Test
    @DisplayName("term() flows into every rate-card connection lookup; the default is MONTH_12")
    void termReachesRateCard() {
        List<Term> recorded = new ArrayList<>();
        RateCard recording = new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                recorded.add(term);
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(100), BigDecimal.ZERO,
                        Currency.getInstance("USD"), PriceSource.CUSTOM));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.CUSTOM;
            }
        };

        MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .rateCard(recording)
                .term(Term.MONTH_36)
                .optimize();

        assertFalse(recorded.isEmpty(), "cost estimation must consult the rate card");
        assertTrue(recorded.stream().allMatch(t -> t == Term.MONTH_36),
                "every pricing lookup must carry the configured term, got: " + recorded);

        recorded.clear();
        MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .rateCard(recording)
                .optimize();
        assertTrue(recorded.stream().allMatch(t -> t == Term.MONTH_12),
                "the default term is MONTH_12, got: " + recorded);
    }

    // ── strategy(...) across every OptimizationStrategy ──

    @ParameterizedTest(name = "strategy {0} runs to completion with its own weight profile")
    @EnumSource(OptimizationStrategy.class)
    @DisplayName("every OptimizationStrategy drives a full run and stamps its weights on the scores")
    void everyStrategyRuns(OptimizationStrategy strategy) {
        OptimizationResult result = dcAnchoredBuilder()
                .requireProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
                .strategy(strategy)
                .optimize();

        List<MetroRecommendation> recs = result.getRecommendations();
        assertFalse(recs.isEmpty(), "strategy " + strategy + " must produce recommendations");
        for (int i = 0; i < recs.size(); i++) {
            assertEquals(i + 1, recs.get(i).getRank(), "ranks are 1..N under " + strategy);
        }

        // Each strategy's weights sum to 1.0, so the normalized component weight the engine
        // stamps on every score must equal the strategy's raw weight for that dimension.
        for (MetroRecommendation rec : recs) {
            ScoreComponent latency = rec.getScore().getComponents().stream()
                    .filter(c -> c.getCategory() == ScoreCategory.LATENCY)
                    .findFirst().orElseThrow();
            assertEquals(strategy.getLatencyWeight(), latency.getWeight(), 1e-9,
                    "latency weight under " + strategy);
            ScoreComponent provider = rec.getScore().getComponents().stream()
                    .filter(c -> c.getCategory() == ScoreCategory.PROVIDER_COVERAGE)
                    .findFirst().orElseThrow();
            assertEquals(strategy.getProviderCoverageWeight(), provider.getWeight(), 1e-9,
                    "provider weight under " + strategy);
        }

        assertTrue(result.getExplanation().getMethodology().contains(strategy.name()),
                "the methodology names the strategy used");
    }

    // ── stub builders (same shapes as MetroOptimizerEngineRunTest) ──

    private static Metro metro(String code, String name, Region region, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(region);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(connected);
        return m;
    }

    private static ConnectedMetro connectedMetro(String code, double avgLatency) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"avgLatency\":" + avgLatency + "}",
                ConnectedMetro.class);
    }

    private static ServiceProfileMetro serviceProfileMetro(String code, String sellerRegion) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code
                        + "\",\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}",
                ServiceProfileMetro.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
