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

package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.http.request.PaginatedPostRequest;
import com.eqixiac.equinix.core.http.request.PaginatedRequest;
import com.eqixiac.equinix.core.http.request.RequestBody;
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.http.response.Pagination;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.FilteredPaginatedPost;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.RiskSeverity;
import com.eqixiac.equinix.design.optimizer.enums.ScoreCategory;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end regression cover for the Metro Optimizer's provider-to-metro resolution, reproduced
 * from a live failure against a real Equinix account on 2026-07-24.
 *
 * <p><strong>What broke.</strong> {@code design_optimize_placement} with
 * {@code require_clouds=[aws, azure]} reported "Analyzed 67 metros, 0 met constraints" and returned
 * zero recommendations; the same request without the cloud requirements returned 67 candidates. Two
 * defects combined:</p>
 * <ol>
 *   <li>the requirement was matched against the provider's <em>corporate</em> name
 *       ("Amazon Web Services") only, so a profile actually named "AWS Direct Connect" never
 *       matched and the availability index came back empty — which the required-provider filter
 *       then read as "no metro has this cloud" and dropped every metro;</li>
 *   <li>{@code buildProviderMetroMap} stopped at the first matching profile ({@code break}), so
 *       even a provider that <em>did</em> resolve only ever contributed one profile's metros.</li>
 * </ol>
 *
 * <p>The stubbed {@link FabricGateway} serves DC, DA, SV (AMER) and LD (EMEA) with inter-metro
 * {@code avgLatency} data; each test supplies its own service-profile set, named the way the Fabric
 * marketplace actually names them.</p>
 *
 * @see CloudProviderProfileMatchingTest
 */
@DisplayName("MetroOptimizer provider resolution (product-named service profiles)")
class MetroOptimizerProviderResolutionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;
    private static final double SV_LAT = 37.3382, SV_LON = -121.8863;
    private static final double LD_LAT = 51.5074, LD_LON = -0.1278;

    private List<Metro> allMetros;

    @BeforeEach
    void buildMetros() throws Exception {
        Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(
                connectedMetro("DA", 10.0), connectedMetro("SV", 60.0), connectedMetro("LD", 75.0)));
        Metro da = metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON, List.of(
                connectedMetro("DC", 10.0), connectedMetro("SV", 45.0)));
        Metro sv = metro("SV", "Silicon Valley", Region.AMER, SV_LAT, SV_LON, List.of(
                connectedMetro("DC", 60.0), connectedMetro("DA", 45.0)));
        Metro ld = metro("LD", "London", Region.EMEA, LD_LAT, LD_LON, List.of(
                connectedMetro("DC", 75.0)));
        allMetros = List.of(dc, da, sv, ld);
    }

    // ══════════════════════════════════════════════
    //  3. The headline regression
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("HEADLINE: requiring AWS + Azure against product-named profiles yields recommendations")
    void requiringTwoCloudsAgainstProductNamedProfilesStillRecommends() throws Exception {
        // Neither profile name contains a corporate name ("Amazon Web Services" / "Microsoft
        // Azure"), which is exactly the shape of the live account's marketplace. Under the old
        // corporate-name-only match both requirements resolved to an EMPTY metro map, the
        // required-provider filter then rejected all four metros, and this call returned zero
        // recommendations plus a CRITICAL REDUNDANCY_GAP — the live 2026-07-24 blackout.
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-1", "AWS Direct Connect",
                        serviceProfileMetro("DC", "us-east-1"), serviceProfileMetro("DA", "us-east-1")),
                profile("sp-azr-1", "Azure ExpressRoute",
                        serviceProfileMetro("DC", "east-us"), serviceProfileMetro("DA", "east-us")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .requireProvider(CloudProviderType.AZURE).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertFalse(result.getRecommendations().isEmpty(),
                "requiring two clouds that ARE present must not black out every metro");
        assertEquals(List.of("DA", "DC"), sortedCodes(result),
                "only the metros carrying both clouds are candidates");

        // Both clouds report as available, with the matched profile carried through.
        for (MetroRecommendation rec : result.getRecommendations()) {
            assertTrue(rec.getAvailableProviders().stream()
                            .anyMatch(p -> p.isAvailable() && "Amazon Web Services".equals(p.getProviderLabel())
                                    && "sp-aws-1".equals(p.getServiceProfileUuid())),
                    "AWS must resolve in " + rec.getMetroId() + ": " + rec.getAvailableProviders());
            assertTrue(rec.getAvailableProviders().stream()
                            .anyMatch(p -> p.isAvailable() && "Microsoft Azure".equals(p.getProviderLabel())
                                    && "sp-azr-1".equals(p.getServiceProfileUuid())),
                    "Azure must resolve in " + rec.getMetroId() + ": " + rec.getAvailableProviders());

            assertEquals("2/2 providers available", explanationFor(rec, ScoreCategory.PROVIDER_COVERAGE),
                    "the coverage explanation must count both required clouds, not report 0/0");
            assertEquals(100.0, rec.getScore().providerScore(), 1e-9,
                    "a metro with both required clouds scores full provider coverage");
        }

        // No lookup-miss finding, and the run is not reported as a coverage failure.
        assertTrue(findings(result, "PROVIDER_UNAVAILABLE").isEmpty(),
                "nothing is unresolved: " + findings(result, "PROVIDER_UNAVAILABLE"));
        assertTrue(result.getRiskAssessment().critical().isEmpty(),
                "no critical risk when both clouds resolve: " + result.getRiskAssessment().critical());
        assertTrue(result.getExplanation().getHumanReadable().contains("Analyzed 4 metros, 2 met constraints"),
                result.getExplanation().getHumanReadable());
        assertFalse(result.getExplanation().getHumanReadable().contains("No Fabric service profile matched"),
                result.getExplanation().getHumanReadable());
    }

    // ══════════════════════════════════════════════
    //  1. Every provider resolves end-to-end
    // ══════════════════════════════════════════════

    @ParameterizedTest(name = "{0} resolves end-to-end from a profile named \"{1}\"")
    @CsvSource({
            "AWS,           'AWS Direct Connect'",
            "AZURE,         'Azure ExpressRoute'",
            "GOOGLE_CLOUD,  'Google Cloud Partner Interconnect'",
            "ORACLE_CLOUD,  'Oracle FastConnect'",
            "IBM_CLOUD,     'IBM Cloud Direct Link'",
            "ALIBABA_CLOUD, 'Alibaba Express Connect'"
    })
    @DisplayName("every cloud provider resolves through the engine from its real product name")
    void everyProviderResolvesThroughTheEngine(CloudProviderType provider, String profileName) throws Exception {
        FabricGateway fabric = gatewayWith(
                profile("sp-1", profileName,
                        serviceProfileMetro("DC", "r1"), serviceProfileMetro("DA", "r1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(provider).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(List.of("DA", "DC"), sortedCodes(result),
                provider + " must resolve from '" + profileName + "' and gate candidacy on it");
        assertTrue(findings(result, "PROVIDER_UNAVAILABLE").isEmpty(),
                provider + " resolved, so no lookup-miss finding may be raised");
        assertNotNull(availability(result, "DC", provider.getProviderName()));
    }

    // ══════════════════════════════════════════════
    //  4. Multi-profile metro union (the removed 'break')
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("two profiles for one provider union their metro coverage instead of stopping at the first")
    void multipleProfilesForOneProviderUnionTheirMetros() throws Exception {
        // A provider legitimately publishes several profiles (seller, hosted, region variants).
        // The old buildProviderMetroMap broke out of the profile loop after the first match, so
        // AWS resolved to DC only and DA was filtered out of candidacy despite carrying AWS.
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-east", "AWS Direct Connect", serviceProfileMetro("DC", "us-east-1")),
                profile("sp-aws-west", "AWS Direct Connect (Hosted)", serviceProfileMetro("DA", "us-west-2")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(List.of("DA", "DC"), sortedCodes(result),
                "both profiles' metros must be candidates: " + sortedCodes(result));

        // Each metro's availability entry names the profile it actually came from.
        assertEquals("sp-aws-east", availability(result, "DC", "Amazon Web Services").getServiceProfileUuid());
        assertEquals(List.of("us-east-1"), availability(result, "DC", "Amazon Web Services").getSellerRegions());
        assertEquals("sp-aws-west", availability(result, "DA", "Amazon Web Services").getServiceProfileUuid());
        assertEquals(List.of("us-west-2"), availability(result, "DA", "Amazon Web Services").getSellerRegions());
    }

    @Test
    @DisplayName("for one metro covered twice, a profile publishing seller regions replaces one that does not")
    void richerEntryReplacesTheSellerRegionlessOne() throws Exception {
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-bare", "AWS Direct Connect", serviceProfileMetroNoRegions("DC")),
                profile("sp-aws-rich", "AWS Direct Connect (Hosted)", serviceProfileMetro("DC", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(List.of("DC"), sortedCodes(result));
        ProviderAvailability aws = availability(result, "DC", "Amazon Web Services");
        assertEquals(List.of("us-east-1"), aws.getSellerRegions(),
                "the richer entry wins so the preferred-seller-region bonus is evaluable");
        assertEquals("sp-aws-rich", aws.getServiceProfileUuid(),
                "uuid and seller regions must come from the SAME profile, never be spliced");
    }

    // ══════════════════════════════════════════════
    //  5. A requirement matching zero profiles is named, not silently empty
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a REQUIRED provider matching zero profiles raises a named CRITICAL lookup-miss finding")
    void requiredProviderMatchingNoProfileIsNamedExplicitly() throws Exception {
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-1", "AWS Direct Connect",
                        serviceProfileMetro("DC", "us-east-1"), serviceProfileMetro("DA", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.ORACLE_CLOUD).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        // The empty result is still correct here — but it must be EXPLAINED, because an empty
        // candidate set caused by a lookup miss is indistinguishable from a genuine coverage gap
        // from the metro count alone. That ambiguity is what made the live failure hard to read.
        assertTrue(result.getRecommendations().isEmpty(), "no metro carries Oracle in this account");

        List<RiskFinding> misses = findings(result, "PROVIDER_UNAVAILABLE");
        assertEquals(1, misses.size(), "exactly one lookup-miss finding: " + misses);
        RiskFinding miss = misses.get(0);
        assertEquals(RiskSeverity.CRITICAL, miss.getSeverity(), "a required provider that resolves to nothing is critical");
        assertTrue(miss.getDescription().contains("Oracle Cloud Infrastructure"),
                "the finding names the provider: " + miss.getDescription());
        assertTrue(miss.getDescription().contains("cloud provider ORACLE_CLOUD"),
                "the finding names the selector that was looked up: " + miss.getDescription());
        assertTrue(miss.getDescription().contains("provider-lookup miss"),
                "the finding distinguishes a lookup miss from a coverage gap: " + miss.getDescription());
        assertNotNull(miss.getRecommendation(), "the finding tells the caller how to pin the profile");
        assertEquals(RiskSeverity.CRITICAL, result.getRiskAssessment().getOverallSeverity());

        assertTrue(result.getExplanation().getHumanReadable().contains(
                        "No Fabric service profile matched required provider(s): Oracle Cloud Infrastructure"),
                "the human-readable explanation names it too: " + result.getExplanation().getHumanReadable());
        assertEquals("No viable metros found matching the given constraints.", result.toSummary());
    }

    @Test
    @DisplayName("an unresolved requirement pinned by profile NAME names that selector in the finding")
    void requiredProfileNameMatchingNoProfileNamesTheSelector() throws Exception {
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-1", "AWS Direct Connect", serviceProfileMetro("DC", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider("Equinix Internet Access").done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        RiskFinding miss = findings(result, "PROVIDER_UNAVAILABLE").stream().findFirst()
                .orElseThrow(() -> new AssertionError("expected a lookup-miss finding, got: "
                        + result.getRiskAssessment().getFindings()));
        assertTrue(miss.getDescription().contains("service profile name 'Equinix Internet Access'"),
                miss.getDescription());
    }

    @Test
    @DisplayName("a PREFERRED provider matching zero profiles is a LOW finding and never empties the set")
    void preferredProviderMatchingNoProfileIsLowAndNonFiltering() throws Exception {
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-1", "AWS Direct Connect",
                        serviceProfileMetro("DC", "us-east-1"), serviceProfileMetro("DA", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .preferProvider(CloudProviderType.ORACLE_CLOUD).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(4, result.getRecommendations().size(), "a preference never filters: " + sortedCodes(result));

        RiskFinding miss = findings(result, "PROVIDER_UNAVAILABLE").stream().findFirst()
                .orElseThrow(() -> new AssertionError("expected a lookup-miss finding, got: "
                        + result.getRiskAssessment().getFindings()));
        assertEquals(RiskSeverity.LOW, miss.getSeverity());
        assertTrue(miss.getDescription().contains("preferred provider 'Oracle Cloud Infrastructure'"),
                miss.getDescription());
        assertTrue(result.getExplanation().getHumanReadable().contains(
                        "No Fabric service profile matched preferred provider(s): Oracle Cloud Infrastructure"),
                result.getExplanation().getHumanReadable());
    }

    // ══════════════════════════════════════════════
    //  8. The catalog is paged, and page 1 is not the catalog
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a provider whose only profile sits on page 2 of the search still resolves")
    void providerOnPageTwoOfTheCatalogStillResolves() throws Exception {
        // search() asks for limit=100 and toList() snapshots only the pages loaded so far, so
        // reading the search result directly indexes page 1 alone. A real account has far more
        // than 100 service profiles: any provider whose profiles sort onto a later page then
        // resolved to NOTHING, which the required-provider filter read as "no metro has this
        // cloud" and turned into the same total blackout the alias fix was supposed to end.
        FabricGateway fabric = gatewayWithPagedProfiles(
                List.of(profile("sp-azr-1", "Azure ExpressRoute",
                        serviceProfileMetro("SV", "west-us"))),
                List.of(profile("sp-aws-1", "AWS Direct Connect",
                        serviceProfileMetro("DC", "us-east-1"), serviceProfileMetro("DA", "us-east-1"))));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(List.of("DA", "DC"), sortedCodes(result),
                "the page-2 profile must be part of the availability index: " + sortedCodes(result));
        assertTrue(findings(result, "PROVIDER_UNAVAILABLE").isEmpty(),
                "AWS resolved on page 2, so nothing is unresolved: " + findings(result, "PROVIDER_UNAVAILABLE"));

        // The diagnostics quote the size of what was actually searched, so a future "nothing
        // matched" verdict can be read against the catalog it was drawn from.
        assertTrue(result.getExplanation().getMethodology().contains("2 Fabric service profiles"),
                "the methodology states how much of the catalog was read: "
                        + result.getExplanation().getMethodology());
        assertTrue(result.getExplanation().getMethodology().contains("across every page"),
                result.getExplanation().getMethodology());
    }

    // ══════════════════════════════════════════════
    //  8b. A paging failure degrades the scan; it does not fail the run
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a failing service-profile page keeps the pages already read and marks the scan degraded")
    void profilePagingFailureDegradesTheScanInsteadOfFailingTheRun() throws Exception {
        // Traversing to the last page turned phase 1 from 2 HTTP requests into up to 200. Both
        // paginated list types roll back and RETHROW when a page fails, and neither the engine nor
        // its callers caught it — so after that change one transient blip anywhere in the traversal
        // failed the WHOLE optimization, which no paging blip could do before it. The pages already
        // read are enough to answer this request, so the run must answer it and say what it missed.
        FabricGateway fabric = gatewayWithFailingProfilePaging(
                List.of(profile("sp-aws-1", "AWS Direct Connect",
                        serviceProfileMetro("DC", "us-east-1"), serviceProfileMetro("DA", "us-east-1"))),
                new EquinixClientException("503 Service Unavailable from SearchServiceProfiles"));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(List.of("DA", "DC"), sortedCodes(result),
                "page 1 resolved AWS, so the run stays answerable: " + sortedCodes(result));

        String methodology = result.getExplanation().getMethodology();
        assertFalse(methodology.contains("across every page of each catalog"),
                "a degraded scan may not claim it read every page: " + methodology);
        assertTrue(methodology.contains("Fabric service profile catalog scan stopped at 1 item(s) "
                        + "after a paging request failed"),
                "the shortfall is attributed to the catalog it happened in: " + methodology);
        assertTrue(methodology.contains("503 Service Unavailable"),
                "and names the failure: " + methodology);
        assertTrue(methodology.contains("Equinix metro catalog was read to its last page"),
                "the catalog that DID complete is reported as complete: " + methodology);

        String humanReadable = result.getExplanation().getHumanReadable();
        assertTrue(humanReadable.contains("WARNING"), humanReadable);
        assertTrue(humanReadable.contains("Fabric service profile catalog scan stopped"), humanReadable);
        assertFalse(humanReadable.contains("Equinix metro catalog scan stopped"),
                "the intact catalog must not be blamed: " + humanReadable);
    }

    @Test
    @DisplayName("a failing metro page is attributed to the metro catalog, not the profile catalog")
    void metroPagingFailureIsAttributedToTheMetroCatalog() throws Exception {
        // The other half of the attribution: with two catalogs in play an unattributed note leaves
        // the reader unable to tell which one was cut short.
        FabricGateway fabric = gatewayWithFailingMetroPaging(
                new EquinixClientException("connection reset reading Metros page 2"),
                profile("sp-aws-1", "AWS Direct Connect", serviceProfileMetro("DC", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(4, result.getRecommendations().size(),
                "the metros already read are still ranked: " + sortedCodes(result));

        String methodology = result.getExplanation().getMethodology();
        assertTrue(methodology.contains("Equinix metro catalog scan stopped at 4 item(s) after a "
                        + "paging request failed"), methodology);
        assertTrue(methodology.contains("Fabric service profile catalog was read to its last page"),
                "the profile catalog completed and is reported as complete: " + methodology);
        assertFalse(methodology.contains("across every page of each catalog"), methodology);
    }

    // ══════════════════════════════════════════════
    //  9. The lookup-miss finding is honest and actionable
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("profiles that match the name but publish no metros are a coverage gap, not a lookup miss")
    void matchedProfilesWithoutMetroCoverageAreNotReportedAsALookupMiss() throws Exception {
        // buildProviderMetroMap contributes nothing for a profile that matched by NAME but published
        // no metros, so the requirement resolves to no metro and lands in exactly the same
        // "unresolved" bucket as a name that matched nothing. The old text then stated, flatly and
        // falsely, that "No Fabric service profile matched" it and that "nothing in the searched
        // catalog carried that provider" - sending the reader hunting a spelling mistake that is not
        // there, when the real fault is a profile with no published metro coverage.
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-empty", "AWS Direct Connect"),
                profile("sp-azr-1", "Azure ExpressRoute", serviceProfileMetro("DC", "east-us")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        RiskFinding miss = findings(result, "PROVIDER_UNAVAILABLE").stream().findFirst()
                .orElseThrow(() -> new AssertionError("expected a finding, got: "
                        + result.getRiskAssessment().getFindings()));
        String description = miss.getDescription();

        assertFalse(description.contains("No Fabric service profile matched required provider"),
                "one profile DID match by name: " + description);
        assertFalse(description.contains("nothing in the searched catalog carried that provider"),
                "the catalog does carry it - it publishes no metros: " + description);
        assertTrue(description.contains("1 Fabric service profile(s) matched required provider "
                + "'Amazon Web Services' but none published any metro coverage"), description);
        assertTrue(description.contains("published-coverage gap"), description);
        assertTrue(description.contains("removed 4 of 4 metros from candidacy"),
                "the cost of the requirement is still quantified: " + description);
        assertTrue(miss.getRecommendation().contains("published metros"),
                "and the fix offered matches the failure that happened: " + miss.getRecommendation());

        assertTrue(result.getExplanation().getHumanReadable().contains(
                        "Fabric service profiles matched required provider(s) but published no metro "
                                + "coverage: Amazon Web Services"),
                result.getExplanation().getHumanReadable());
        assertFalse(result.getExplanation().getHumanReadable().contains(
                        "No Fabric service profile matched required provider(s)"),
                result.getExplanation().getHumanReadable());
    }

    @Test
    @DisplayName("a workload dependency whose profiles publish no metros says which failure happened")
    void matchedWorkloadDependencyWithoutMetroCoverageIsDescribedAccurately() throws Exception {
        FabricGateway fabric = gatewayWith(
                profile("sp-ora-empty", "Oracle Cloud Infrastructure FastConnect"),
                profile("sp-aws-1", "AWS Direct Connect", serviceProfileMetro("DC", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Analytics").bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.ORACLE_CLOUD).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        RiskFinding miss = findings(result, "WORKLOAD_PROVIDER_UNAVAILABLE").stream().findFirst()
                .orElseThrow(() -> new AssertionError("expected a workload-dependency finding, got: "
                        + result.getRiskAssessment().getFindings()));
        assertTrue(miss.getDescription().contains("1 Fabric service profile(s) matched 'Oracle Cloud "
                + "Infrastructure', declared as a workload dependency, but none published any metro "
                + "coverage"), miss.getDescription());
        assertTrue(result.getExplanation().getHumanReadable().contains(
                        "Fabric service profiles matched workload dependency provider(s) but published "
                                + "no metro coverage: Oracle Cloud Infrastructure"),
                result.getExplanation().getHumanReadable());
    }

    @Test
    @DisplayName("the unresolved-provider finding states what was searched and contradicts nothing")
    void lookupMissFindingIsInternallyConsistent() throws Exception {
        // requireMetro(SV) puts a metro in the payload alongside the miss, which is exactly the
        // shape the old text lied about: it asserted "Every metro was therefore excluded" while
        // the same response recommended one.
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-1", "AWS Direct Connect", serviceProfileMetro("DC", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.ORACLE_CLOUD).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().requireMetro(MetroCode.SV).maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(List.of("SV"), sortedCodes(result), "the forced metro survives: " + sortedCodes(result));

        RiskFinding miss = findings(result, "PROVIDER_UNAVAILABLE").stream().findFirst()
                .orElseThrow(() -> new AssertionError("expected a lookup-miss finding, got: "
                        + result.getRiskAssessment().getFindings()));

        String description = miss.getDescription();
        assertFalse(description.contains("Every metro was therefore excluded"),
                "a metro IS recommended in this very payload: " + description);
        assertFalse(description.contains("(matched on"),
                "'no profile matched ... (matched on X)' contradicts itself in one sentence: " + description);
        assertTrue(description.contains("searched 1 Fabric service profile(s)"),
                "the finding says how much of the catalog was searched: " + description);
        assertTrue(description.contains("looking them up by cloud provider ORACLE_CLOUD"),
                "the finding says how the lookup was performed: " + description);
        assertTrue(description.contains("removed 4 of 4 metros from candidacy"),
                "the finding quantifies the real cost of the requirement: " + description);
        assertTrue(description.contains("provider-lookup miss"), description);

        // The recommendation must be actionable by whoever is reading it — an MCP agent cannot
        // call a Java builder method, which is what the previous wording told it to do.
        String recommendation = miss.getRecommendation();
        assertFalse(recommendation.contains("requireProvider("),
                "do not instruct the caller to invoke a Java API it may not have: " + recommendation);
        assertFalse(recommendation.contains("preferProvider("), recommendation);
    }

    // ══════════════════════════════════════════════
    //  10. Workload-level dependencies are never silent either
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a workload dependsOn provider that matches no profile is named, not silently dropped")
    void unresolvedWorkloadDependencyIsNamed() throws Exception {
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-1", "AWS Direct Connect",
                        serviceProfileMetro("DC", "us-east-1"), serviceProfileMetro("DA", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Analytics").bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.ORACLE_CLOUD).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        // A workload dependency never filters, so the run still recommends metros — which is
        // precisely why the miss has to be stated rather than inferred from the result.
        assertEquals(4, result.getRecommendations().size(), sortedCodes(result).toString());

        RiskFinding miss = findings(result, "WORKLOAD_PROVIDER_UNAVAILABLE").stream().findFirst()
                .orElseThrow(() -> new AssertionError("expected a workload-dependency finding, got: "
                        + result.getRiskAssessment().getFindings()));
        assertEquals(RiskSeverity.MEDIUM, miss.getSeverity());
        assertTrue(miss.getDescription().contains("Oracle Cloud Infrastructure"), miss.getDescription());
        assertTrue(miss.getDescription().contains("workload dependency"), miss.getDescription());
        assertTrue(result.getExplanation().getHumanReadable().contains(
                        "No Fabric service profile matched workload dependency provider(s): "
                                + "Oracle Cloud Infrastructure"),
                result.getExplanation().getHumanReadable());

        // The placement itself admits the dependency could not be honoured.
        assertTrue(result.getTopology().getPlacements().stream()
                        .allMatch(p -> p.getReasoning().contains("not all available in any recommended metro")),
                "the placement rationale must not read like an unconstrained one: "
                        + result.getTopology().getPlacements());
    }

    // ══════════════════════════════════════════════
    //  11. Same-metro merge is deterministic
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("two profiles covering one metro resolve to the same winner whatever order the catalog returns")
    void sameMetroMergeIsOrderIndependent() throws Exception {
        // search() passes no SortPropertyList, so profile order is whatever the server returns.
        // A first-seen rule therefore made the entry — and the seller region the deployment
        // wizard plans against — depend on that order.
        ServiceProfile east = profile("sp-aws-east", "AWS Direct Connect", serviceProfileMetro("DC", "us-east-1"));
        ServiceProfile west = profile("sp-aws-west", "AWS Direct Connect (Hosted)", serviceProfileMetro("DC", "us-west-2"));

        ProviderAvailability forward = awsAvailabilityForDc(gatewayWith(east, west));
        ProviderAvailability reversed = awsAvailabilityForDc(gatewayWith(west, east));

        assertEquals(forward.getServiceProfileUuid(), reversed.getServiceProfileUuid(),
                "the winning profile must not depend on catalog order");
        assertEquals(forward.getSellerRegions(), reversed.getSellerRegions());
        assertEquals("sp-aws-east", forward.getServiceProfileUuid(),
                "with nothing else to separate them the lowest uuid wins deterministically");
    }

    @Test
    @DisplayName("a preferred seller region decides the winner, which is the bonus the rule exists to serve")
    void preferredSellerRegionDecidesTheWinner() throws Exception {
        ServiceProfile east = profile("sp-aws-east", "AWS Direct Connect", serviceProfileMetro("DC", "us-east-1"));
        ServiceProfile west = profile("sp-aws-west", "AWS Direct Connect (Hosted)", serviceProfileMetro("DC", "us-west-2"));

        // us-west-2 is preferred and sp-aws-west carries it, even though sp-aws-east sorts first
        // and would win the uuid tie-break.
        OptimizationResult result = MetroOptimizer.builder(gatewayWith(east, west))
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).sellerRegions("us-west-2").done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        ProviderAvailability aws = availability(result, "DC", "Amazon Web Services");
        assertEquals("sp-aws-west", aws.getServiceProfileUuid(),
                "the profile matching the preferred seller region must win");
        assertEquals(List.of("us-west-2"), aws.getSellerRegions(),
                "uuid and seller regions still come from the SAME profile - never unioned across profiles");
    }

    // ══════════════════════════════════════════════
    //  12. No providers requested: the score and its explanation agree
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("with no providers requested the coverage explanation agrees with the perfect score")
    void noProvidersRequestedReadsConsistentlyWithTheScore() throws Exception {
        FabricGateway fabric = gatewayWith(
                profile("sp-aws-1", "AWS Direct Connect", serviceProfileMetro("DC", "us-east-1")));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        for (MetroRecommendation rec : result.getRecommendations()) {
            assertEquals(100.0, rec.getScore().providerScore(), 1e-9,
                    "nothing was asked for, so nothing is missing");
            String explanation = explanationFor(rec, ScoreCategory.PROVIDER_COVERAGE);
            assertFalse(explanation.contains("0/0"),
                    "'0/0 providers available' next to a score of 100 is a self-contradiction: " + explanation);
            assertTrue(explanation.contains("No providers were required or preferred"), explanation);
        }
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    /** The AWS availability entry this gateway resolves for DC. */
    private ProviderAvailability awsAvailabilityForDc(FabricGateway fabric) {
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();
        return availability(result, "DC", "Amazon Web Services");
    }

    private static List<String> sortedCodes(OptimizationResult result) {
        return result.getRecommendations().stream()
                .map(r -> r.getMetroId().code())
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<RiskFinding> findings(OptimizationResult result, String category) {
        return result.getRiskAssessment().getFindings().stream()
                .filter(f -> category.equals(f.getCategory()))
                .collect(Collectors.toList());
    }

    private static String explanationFor(MetroRecommendation rec, ScoreCategory category) {
        return rec.getScore().getComponents().stream()
                .filter(c -> c.getCategory() == category)
                .findFirst().orElseThrow()
                .getExplanation();
    }

    /** The availability entry a recommendation reports for one provider label. */
    private static ProviderAvailability availability(OptimizationResult result, String metroCode, String label) {
        MetroRecommendation rec = result.getRecommendations().stream()
                .filter(r -> r.getMetroId().code().equals(metroCode))
                .findFirst().orElseThrow(() -> new AssertionError(
                        metroCode + " is not among the recommendations: " + sortedCodes(result)));
        ProviderAvailability entry = rec.getAvailableProviders().stream()
                .filter(p -> label.equals(p.getProviderLabel()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no availability entry for " + label + " in " + metroCode));
        assertTrue(entry.isAvailable(), label + " must be available in " + metroCode);
        return entry;
    }

    // ── stub builders (same shapes as MetroOptimizerEngineRunTest / MetroOptimizerLeversTest) ──

    private FabricGateway gatewayWith(ServiceProfile... profiles) {
        return gatewayServing(new PaginatedFilteredList<>(List.of(profiles), null, null, null, null));
    }

    /**
     * A gateway whose service-profile search really pages: {@code pageTwo} is only reachable by
     * advancing past the first page, exactly as a >100-profile account behaves.
     */
    private FabricGateway gatewayWithPagedProfiles(List<ServiceProfile> pageOne,
                                                   List<ServiceProfile> pageTwo) throws Exception {
        Pagination notLast = Constants.mapper()
                .readValue("{\"offset\":0,\"limit\":100,\"total\":150}", Pagination.class);
        Pagination last = Constants.mapper()
                .readValue("{\"offset\":100,\"limit\":100,\"total\":150}", Pagination.class);

        PaginatedPostRequest<ServiceProfile> request = new PaginatedPostRequest<>();
        request.setServiceEndpoint("SearchServiceProfiles");
        request.setBody(RequestBody.json(new FilteredPaginatedPost<>("any")));

        PaginatedFilteredList<ServiceProfile> second =
                new PaginatedFilteredList<>(pageTwo, null, request, null, last);
        PageablePost<ServiceProfile> pageable = new PageablePost<>() {
            @Override
            public PaginatedFilteredList<ServiceProfile> nextPage(PaginatedPostRequest<ServiceProfile> req) {
                return second;
            }

            @Override
            public PaginatedList<ServiceProfile> nextPage(PaginatedRequest<ServiceProfile> req) {
                throw new UnsupportedOperationException("this stub pages via POST only");
            }
        };
        return gatewayServing(new PaginatedFilteredList<>(pageOne, pageable, request, null, notLast));
    }

    /**
     * A gateway whose service-profile search reports a second page and then fails when it is
     * requested — the transient-blip shape, not a permanent one.
     */
    private FabricGateway gatewayWithFailingProfilePaging(List<ServiceProfile> pageOne,
                                                          RuntimeException failure) throws Exception {
        Pagination notLast = Constants.mapper()
                .readValue("{\"offset\":0,\"limit\":100,\"total\":150}", Pagination.class);

        PaginatedPostRequest<ServiceProfile> request = new PaginatedPostRequest<>();
        request.setServiceEndpoint("SearchServiceProfiles");
        request.setBody(RequestBody.json(new FilteredPaginatedPost<>("any")));

        PageablePost<ServiceProfile> pageable = new PageablePost<>() {
            @Override
            public PaginatedFilteredList<ServiceProfile> nextPage(PaginatedPostRequest<ServiceProfile> req) {
                throw failure;
            }

            @Override
            public PaginatedList<ServiceProfile> nextPage(PaginatedRequest<ServiceProfile> req) {
                throw failure;
            }
        };
        return gatewayServing(new PaginatedFilteredList<>(pageOne, pageable, request, null, notLast));
    }

    /** A gateway whose METRO list reports a second page and then fails when it is requested. */
    private FabricGateway gatewayWithFailingMetroPaging(RuntimeException failure,
                                                        ServiceProfile... profiles) throws Exception {
        Pagination notLast = Constants.mapper()
                .readValue("{\"offset\":0,\"limit\":100,\"total\":150}", Pagination.class);

        PaginatedRequest<Metro> request = new PaginatedRequest<>();
        request.setServiceEndpoint("GetMetros");
        Pageable<Metro> pageable = req -> {
            throw failure;
        };

        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(allMetros, pageable, request, null, notLast));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.of(profiles), null, null, null, null));

        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
        return fabric;
    }

    private FabricGateway gatewayServing(PaginatedFilteredList<ServiceProfile> profiles) {
        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(allMetros, null, null, null, null));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(profiles);

        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
        return fabric;
    }

    private static ServiceProfile profile(String uuid, String name, ServiceProfileMetro... metros) {
        ServiceProfile profile = mock(ServiceProfile.class);
        when(profile.getUuid()).thenReturn(uuid);
        when(profile.getName()).thenReturn(name);
        when(profile.metros()).thenReturn(List.of(metros));
        return profile;
    }

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

    private static ServiceProfileMetro serviceProfileMetroNoRegions(String code) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code + "\"}",
                ServiceProfileMetro.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
