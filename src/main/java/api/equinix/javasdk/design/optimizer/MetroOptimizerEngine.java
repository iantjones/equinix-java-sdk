package api.equinix.javasdk.design.optimizer;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.AccessPointTypeConfig;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.design.optimizer.enums.*;
import api.equinix.javasdk.design.optimizer.model.*;
import api.equinix.javasdk.design.value.CurrencyReconciler;
import api.equinix.javasdk.design.value.ratecard.EquinixRateCard;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Core optimization engine that scores, ranks, and assigns Equinix metros
 * based on user-defined sites, provider requirements, workloads, and constraints.
 *
 * <p>This class is stateless; all state is contained in the {@link OptimizationRequest}
 * and the data fetched from the Fabric APIs.</p>
 */
final class MetroOptimizerEngine {

    private static final double FIBER_DISTANCE_MULTIPLIER = 1.4;
    private static final double FIBER_LATENCY_US_PER_KM = 4.9;
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Hard stop on how many pages a catalog traversal will pull, whatever the server reports.
     * A page is 100 items ({@code Constants.PAGE_LIMIT}), so this bounds one run to 100 requests
     * per catalog even if pagination metadata never reports a last page.
     */
    private static final int MAX_CATALOG_PAGES = 100;

    /** Hard stop on the number of service profiles one run will scan. */
    private static final int MAX_SERVICE_PROFILES_SCANNED = 10_000;

    /** Hard stop on the number of metros one run will scan. */
    private static final int MAX_METROS_SCANNED = 5_000;

    private MetroOptimizerEngine() {}

    /**
     * Executes the full optimization pipeline against live Fabric API data.
     *
     * <p>The pipeline consists of a validation step and 12 phases:</p>
     * <ol>
     *   <li><strong>Validation</strong> -- Rejects request values the engine cannot act on (a
     *       non-positive or non-finite latency bound or workload latency tolerance) with an
     *       {@link IllegalArgumentException}, rather than accepting them and silently doing nothing.</li>
     *   <li><strong>Data Collection</strong> -- Fetches all metros and service profiles via the Fabric
     *       client, traversing every page. A traversal that hits the scan bound, or that a paging
     *       failure cuts short, keeps what it read and is reported as incomplete per catalog.</li>
     *   <li><strong>Candidate Filtering</strong> -- Eliminates metros that violate hard constraints
     *       (excluded regions/metros, compliance zones, and — when {@code maxLatencyMs} is set — an
     *       estimated latency to any user site beyond the bound). The provider gate is a per-metro
     *       <em>eligibility</em> test, not a whole-set union: a metro qualifies when it either carries
     *       every request-level required cloud, or carries all the clouds of at least one workload that
     *       declares its own dependencies. Request-level required clouds are enforced as a coverage
     *       guarantee across the selected set (Risk Analysis), not as a "present in every metro" filter,
     *       so a single-cloud metro can qualify to host a single-cloud workload.</li>
     *   <li><strong>Scoring</strong> -- Scores each candidate across five dimensions: latency, provider
     *       coverage, cost, redundancy, and compliance, using weighted aggregation.</li>
     *   <li><strong>Selection</strong> -- Selects the top N metros by composite score. When a
     *       multi-region or multi-metro redundancy tier is requested the selection is
     *       region-diversity-aware: candidates are grouped by region and picked round-robin best-per-region
     *       (regions the user has sites in first), so a single region can never monopolise the set and
     *       genuine geographic spread is a hard outcome rather than a scoring nudge.</li>
     *   <li><strong>Redundancy Refinement</strong> -- Recalculates redundancy scores based on the
     *       actual geographic diversity of the selected set.</li>
     *   <li><strong>Latency Matrix</strong> -- Builds a metro-by-site latency grid.</li>
     *   <li><strong>Provider Connectivity</strong> -- Maps provider availability per metro.</li>
     *   <li><strong>Topology Assembly</strong> -- Assigns workloads to metros using a greedy algorithm.</li>
     *   <li><strong>Risk Analysis</strong> -- Identifies deployment risks (single points of failure,
     *       compliance gaps, latency violations).</li>
     *   <li><strong>Cost Estimation</strong> -- Produces per-metro and aggregate cost estimates.</li>
     *   <li><strong>Recommendation Building</strong> -- Assembles ranked {@link MetroRecommendation} objects.</li>
     *   <li><strong>Explanation</strong> -- Generates human-readable methodology documentation.</li>
     * </ol>
     *
     * @param request the fully-assembled optimization request
     * @param fabric  the authenticated Fabric client for API access
     * @return the complete optimization result
     * @throws IllegalArgumentException if the request carries a latency bound or a workload latency
     *                                  tolerance that is not a positive, finite number of milliseconds
     * @see OptimizationResult
     */
    static OptimizationResult execute(OptimizationRequest request, FabricGateway fabric) {
        long startTime = System.currentTimeMillis();

        // Phase 0: reject the inputs this engine cannot honour. A constraint that is accepted,
        // silently dropped, and then absent from every diagnostic is indistinguishable from one that
        // was satisfied, so it is refused at the door instead of being ignored.
        validateRequest(request);

        // Phase 1: Data Collection. Both catalogs are traversed to the LAST page: a single page is
        // 100 items, and an availability index built from page 1 alone silently blacks out every
        // provider whose profiles happen to sort onto page 2+. The traversal is bounded so a
        // pathological catalog (or pagination metadata that never reports a last page) cannot run
        // away, and a paging failure keeps the pages already read instead of failing the whole run.
        // Neither outcome is hidden — see CatalogScan#outcome.
        CatalogScan<Metro> metroScan =
                scanAll(METRO_CATALOG, fabric.metros().list(), MAX_METROS_SCANNED);
        CatalogScan<ServiceProfile> profileScan =
                scanAll(PROFILE_CATALOG, fabric.serviceProfiles().search(), MAX_SERVICE_PROFILES_SCANNED);
        List<Metro> allMetros = metroScan.items;
        List<ServiceProfile> serviceProfiles = profileScan.items;

        // Build lookup maps
        Map<MetroId, Metro> metroMap = new HashMap<>();
        for (Metro m : allMetros) {
            metroMap.put(m.metroId(), m);
        }

        Map<MetroId, Map<MetroId, Double>> latencyMap = buildLatencyMap(allMetros);

        // Build provider → metro availability map, covering both the request-level
        // requirements and the per-workload provider dependencies.
        ProviderIndex providerIndex =
                buildProviderIndex(collectProviderRequirements(request), serviceProfiles);
        Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap = providerIndex.availability;

        // Requirements that resolved to no metro at all — either because no service profile matched
        // the selector, or because the profiles that did match published no metro coverage. Both
        // would otherwise silently empty the candidate set (request-level) or silently mis-place a
        // workload (workload-level), and the two are reported as the different failures they are.
        List<UnresolvedProvider> unresolvedProviders = findUnresolvedProviders(request, providerIndex);
        List<UnresolvedProvider> unresolvedWorkloadProviders =
                findUnresolvedWorkloadProviders(request, providerIndex);

        // Phase 2: Candidate Filtering
        CandidateSet candidateSet = filterCandidates(allMetros, request, providerMetroMap, metroMap, latencyMap);
        List<Metro> candidates = candidateSet.candidates;

        // Phase 3: Scoring
        int totalHeadcount = request.getSites().stream().mapToInt(UserSite::getHeadcount).sum();
        SiteWeighting siteWeighting = SiteWeighting.of(request.getSites(), totalHeadcount);
        ScoringWeights weights = request.getScoringWeights();
        OptimizationStrategy strategy = request.getStrategy();

        double wLatency = weights.resolveLatencyWeight(strategy);
        double wProvider = weights.resolveProviderCoverageWeight(strategy);
        double wCost = weights.resolveCostWeight(strategy);
        double wRedundancy = weights.resolveRedundancyWeight(strategy);
        double wCompliance = weights.resolveComplianceWeight(strategy);

        // Normalize weights
        double totalWeight = wLatency + wProvider + wCost + wRedundancy + wCompliance;
        if (totalWeight > 0) {
            wLatency /= totalWeight;
            wProvider /= totalWeight;
            wCost /= totalWeight;
            wRedundancy /= totalWeight;
            wCompliance /= totalWeight;
        }

        // Score each candidate
        List<ScoredMetro> scoredMetros = new ArrayList<>();
        for (Metro candidate : candidates) {
            double latencyScore = scoreLatency(candidate, request, metroMap, latencyMap, siteWeighting, weights);
            double providerScore = scoreProviderCoverage(candidate, request, providerMetroMap, weights);
            double costScore = scoreCost(candidate, request);
            double redundancyScore = 50.0; // baseline, refined in Phase 5
            double complianceScore = scoreCompliance(candidate, request);

            // Latency and provider-coverage explanations are expensive to build
            // (describeLatencyScore re-runs estimateLatency per user site and formats
            // strings) and are only ever surfaced for the top maxMetros. They are left
            // null here and backfilled for the selected set below; the numeric score and
            // weight — the only inputs to ranking — are computed eagerly, so selection is
            // unaffected.
            List<ScoreComponent> components = Arrays.asList(
                    ScoreComponent.builder()
                            .category(ScoreCategory.LATENCY)
                            .score(latencyScore)
                            .weight(wLatency)
                            .explanation(null)
                            .build(),
                    ScoreComponent.builder()
                            .category(ScoreCategory.PROVIDER_COVERAGE)
                            .score(providerScore)
                            .weight(wProvider)
                            .explanation(null)
                            .build(),
                    ScoreComponent.builder()
                            .category(ScoreCategory.COST)
                            .score(costScore)
                            .weight(wCost)
                            .explanation("Cost score based on estimated connection pricing")
                            .build(),
                    ScoreComponent.builder()
                            .category(ScoreCategory.REDUNDANCY)
                            .score(redundancyScore)
                            .weight(wRedundancy)
                            .explanation("Baseline redundancy score; refined after topology assembly")
                            .build(),
                    ScoreComponent.builder()
                            .category(ScoreCategory.COMPLIANCE)
                            .score(complianceScore)
                            .weight(wCompliance)
                            .explanation(complianceScore >= 100 ? "Meets all compliance requirements"
                                    : "Some compliance zones not fully satisfied")
                            .build()
            );

            double composite = components.stream()
                    .mapToDouble(ScoreComponent::weightedScore)
                    .sum();

            scoredMetros.add(new ScoredMetro(candidate, new MetroScore(composite, components)));
        }

        // Sort by composite score descending
        scoredMetros.sort((a, b) -> Double.compare(b.score.getComposite(), a.score.getComposite()));

        // Phase 4: Selection. Greedy top-N by composite score, EXCEPT when a multi-region or
        // multi-metro redundancy tier is requested — then selection spreads across regions
        // (round-robin best-per-region) so a single region cannot fill the whole set and the
        // requested geographic diversity is honoured, not merely scored.
        int maxMetros = resolveMaxMetros(request);
        List<ScoredMetro> selected = selectMetros(scoredMetros, maxMetros, request, metroMap);

        // Phase 4b: Backfill the deferred latency/provider explanations for the selected
        // metros only. These descriptions do not influence the composite score, so the
        // surfaced output is identical to computing them eagerly for every candidate.
        selected = backfillScoreDescriptions(selected, request, metroMap, latencyMap,
                providerMetroMap, siteWeighting);

        // Phase 5: Refine redundancy scores for selected set
        selected = refineRedundancyScores(selected, request, wRedundancy);

        // Phase 6: Build latency matrix
        LatencyMatrix latencyMatrix = buildLatencyMatrix(selected, request, metroMap, latencyMap);

        // Phase 7: Provider connectivity map
        ProviderConnectivityMap providerConnMap = buildProviderConnectivityMap(selected, request, providerMetroMap);

        // Phase 8: Topology assembly (workload placement)
        DeploymentTopology topology = assembleTopology(selected, request, metroMap, latencyMap,
                providerMetroMap, siteWeighting);

        // Phase 9: Cost estimate. Computed before risk analysis so the budget check can raise a
        // finding: the budget is reported against, and an over-budget estimate is a risk finding
        // rather than a flag buried in the cost object.
        RateCard rateCard = request.getRateCard() != null ? request.getRateCard() : EquinixRateCard.of(fabric);
        CostEstimate costEstimate = estimateCosts(selected, request, rateCard);

        // Phase 10: Risk analysis
        RiskAssessment riskAssessment = analyzeRisks(selected, request, providerMetroMap, latencyMatrix,
                unresolvedProviders, unresolvedWorkloadProviders, candidateSet, metroScan, profileScan,
                costEstimate);

        // Phase 11: Build recommendations
        List<MetroRecommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            ScoredMetro sm = selected.get(i);
            Metro metro = sm.metro;
            MetroId code = metro.metroId();

            Map<String, Double> siteLatencies = new LinkedHashMap<>();
            for (UserSite site : request.getSites()) {
                double lat = estimateLatency(code, site, metroMap, latencyMap);
                siteLatencies.put(site.getLabel(), lat);
            }

            List<ProviderAvailability> provAvail = providerConnMap.forMetro(code);
            MetroCostBreakdown metroCost = costEstimate.getPerMetro().stream()
                    .filter(c -> java.util.Objects.equals(c.getMetroId(), code))
                    .findFirst().orElse(null);

            List<WorkloadPlacement> metroWorkloads = topology.forMetro(code);
            List<String> reasons = generateReasons(sm, request, providerMetroMap, siteLatencies);

            recommendations.add(MetroRecommendation.builder()
                    .rank(i + 1)
                    .metroId(code)
                    .metroName(metro.getName())
                    .region(metro.getRegion())
                    .coordinates(metro.geoCoordinates())
                    .score(sm.score)
                    .reasons(reasons)
                    .availableProviders(provAvail)
                    .siteLatencies(siteLatencies)
                    .estimatedCost(metroCost)
                    .assignedWorkloads(metroWorkloads)
                    .build());
        }

        // Phase 12: Explanation
        OptimizationExplanation explanation = buildExplanation(request, candidateSet,
                selected.size(), maxMetros, unresolvedProviders, unresolvedWorkloadProviders,
                metroScan, profileScan);

        long computeTime = System.currentTimeMillis() - startTime;

        return OptimizationResult.builder()
                .request(request)
                .recommendations(recommendations)
                .topology(topology)
                .providerMap(providerConnMap)
                .latencyMatrix(latencyMatrix)
                .riskAssessment(riskAssessment)
                .costEstimate(costEstimate)
                .explanation(explanation)
                .computedAt(Instant.now())
                .computeTimeMs(computeTime)
                .build();
    }

    // ══════════════════════════════════════════════
    //  Input Validation
    // ══════════════════════════════════════════════

    /**
     * Rejects request values the engine cannot act on, instead of accepting them and quietly doing
     * nothing.
     *
     * <p>A latency ceiling of {@code 0} (or a negative, or {@code NaN}) is not a request to run
     * unconstrained &mdash; it is a caller error, and every downstream guard required a positive
     * bound, so such a value produced no filtering, no risk finding and no methodology clause: a
     * constraint accepted at the door and then indistinguishable from one that was satisfied. The
     * same applies to a workload's own latency tolerance. Both are refused here, naming the value and
     * the fix, so the caller learns their input was wrong rather than trusting a result that ignored
     * it.</p>
     *
     * @throws IllegalArgumentException if a latency bound or tolerance is non-positive or non-finite
     */
    private static void validateRequest(OptimizationRequest request) {
        Double maxLatencyMs = request.getConstraints() != null
                ? request.getConstraints().getMaxLatencyMs() : null;
        if (maxLatencyMs != null && !isUsableLatencyMs(maxLatencyMs)) {
            throw new IllegalArgumentException("constraints.maxLatencyMs must be a positive, finite "
                    + "number of milliseconds; got " + maxLatencyMs + ". No metro can satisfy a "
                    + "non-positive ceiling, and the optimizer will not accept a constraint it would "
                    + "then have to ignore: omit the bound to search unconstrained, or give the real "
                    + "site-to-metro ceiling (for example 20).");
        }
        for (WorkloadSpec workload : request.getWorkloads()) {
            Double tolerance = workload.resolvedProfile().getMaxLatencyToleranceMs();
            if (tolerance != null && !isUsableLatencyMs(tolerance)) {
                throw new IllegalArgumentException("maxLatencyToleranceMs on workload '"
                        + workload.getLabel() + "' must be a positive, finite number of milliseconds; "
                        + "got " + tolerance + ". Omit it to place the workload without a latency "
                        + "ceiling, or give the real ceiling (for example 20).");
            }
        }
    }

    /** A latency figure the engine can compare against: positive and finite. */
    private static boolean isUsableLatencyMs(double ms) {
        return Double.isFinite(ms) && ms > 0;
    }

    // ══════════════════════════════════════════════
    //  Catalog Traversal
    // ══════════════════════════════════════════════

    /** How completely one paginated catalog was read. */
    private enum ScanOutcome {
        /** Every page the server reported was read. */
        COMPLETE,
        /** The traversal stopped on this run's page/item bound with pages still unread. */
        BOUNDED,
        /** A paging request failed; the pages already read were kept and the run continued. */
        DEGRADED
    }

    /**
     * The outcome of traversing one paginated catalog: which catalog it was, everything that was
     * read, and whether the traversal finished, hit this run's bound, or was cut short by a paging
     * failure. The engine attributes the item count and any incompleteness <em>per catalog</em> in
     * its diagnostics, so a "nothing matched" verdict can always be read against how much of which
     * catalog was actually searched.
     */
    private static final class CatalogScan<T> {
        /** Human name of the catalog, e.g. {@code "Equinix metro"}; used to attribute the note. */
        final String catalog;
        final List<T> items;
        final ScanOutcome outcome;
        /** The paging failure that degraded the scan; {@code null} unless {@link ScanOutcome#DEGRADED}. */
        final String failure;

        CatalogScan(String catalog, List<T> items, ScanOutcome outcome, String failure) {
            this.catalog = catalog;
            this.items = items;
            this.outcome = outcome;
            this.failure = failure;
        }

        int size() {
            return items.size();
        }

        boolean isComplete() {
            return outcome == ScanOutcome.COMPLETE;
        }

        /**
         * A parenthetical to append to any diagnostic that reasons from this scan, or {@code ""}
         * when the catalog was read in full. Never claims exhaustiveness it does not have, and
         * distinguishes "stopped on the bound" from "a page failed".
         */
        String truncationNote() {
            switch (outcome) {
                case BOUNDED:
                    return " (the scan stopped at " + items.size() + " items on this run's scan bound "
                            + "and the catalog holds more, so the search was NOT exhaustive)";
                case DEGRADED:
                    return " (the scan stopped at " + items.size() + " items because a paging request "
                            + "failed - " + failure + " - so the search was NOT exhaustive; the pages "
                            + "already read were kept)";
                default:
                    return "";
            }
        }

        /** A clause naming this catalog and exactly how completely it was read. */
        String coverageClause() {
            switch (outcome) {
                case BOUNDED:
                    return "the " + catalog + " catalog scan stopped at " + items.size() + " item(s) on "
                            + "this run's scan bound with pages still unread, so it was NOT read in full";
                case DEGRADED:
                    return "the " + catalog + " catalog scan stopped at " + items.size() + " item(s) "
                            + "after a paging request failed (" + failure + "), so it was NOT read in "
                            + "full; the pages already read were kept and the run continued";
                default:
                    return "the " + catalog + " catalog was read to its last page";
            }
        }
    }

    /** Catalog names, shared by the scan and every diagnostic that attributes coverage to it. */
    private static final String METRO_CATALOG = "Equinix metro";
    private static final String PROFILE_CATALOG = "Fabric service profile";

    // PaginatedList and PaginatedFilteredList do not share a paging supertype (only Iterable), so
    // the traversal is spelled once per type rather than reflected over.

    private static <T> CatalogScan<T> scanAll(String catalog, PaginatedList<T> page, int maxItems) {
        int pages = 1;
        while (page.hasNextPage() && pages < MAX_CATALOG_PAGES && page.size() < maxItems) {
            try {
                page.next();
            }
            catch (RuntimeException e) {
                return degraded(catalog, page.toList(), e);
            }
            pages++;
        }
        return new CatalogScan<>(catalog, new ArrayList<>(page.toList()),
                page.hasNextPage() ? ScanOutcome.BOUNDED : ScanOutcome.COMPLETE, null);
    }

    private static <T> CatalogScan<T> scanAll(String catalog, PaginatedFilteredList<T> page, int maxItems) {
        int pages = 1;
        while (page.hasNextPage() && pages < MAX_CATALOG_PAGES && page.size() < maxItems) {
            try {
                page.next();
            }
            catch (RuntimeException e) {
                return degraded(catalog, page.toList(), e);
            }
            pages++;
        }
        return new CatalogScan<>(catalog, new ArrayList<>(page.toList()),
                page.hasNextPage() ? ScanOutcome.BOUNDED : ScanOutcome.COMPLETE, null);
    }

    /**
     * Keeps the pages a failed traversal had already read and marks the scan degraded.
     *
     * <p>Traversing to the last page turned one HTTP request per catalog into up to
     * {@link #MAX_CATALOG_PAGES}, and both {@code PaginatedList} and {@code PaginatedFilteredList}
     * roll their request back and rethrow when a page fails. Left uncaught, a single transient
     * paging blip anywhere in that traversal would fail the entire optimization — a run that was
     * perfectly answerable from the pages already in hand. So the failure is absorbed here: the
     * items collected so far are intact (the rollback leaves the shared request re-fetchable, and
     * nothing was appended for the failed page), the scan is marked {@link ScanOutcome#DEGRADED},
     * and every diagnostic built from it says so. A partial catalog must never masquerade as a
     * complete one, and a blip must never destroy an answerable run.</p>
     */
    private static <T> CatalogScan<T> degraded(String catalog, List<T> readSoFar, RuntimeException failure) {
        return new CatalogScan<>(catalog, new ArrayList<>(readSoFar), ScanOutcome.DEGRADED,
                describeFailure(failure));
    }

    /** A one-line, bounded rendering of a paging failure, safe to embed in a sentence. */
    private static String describeFailure(RuntimeException failure) {
        String message = failure.getMessage();
        String detail = failure.getClass().getSimpleName();
        if (message != null && !message.isBlank()) {
            String flattened = message.replaceAll("\\s+", " ").trim();
            if (flattened.length() > 200) {
                flattened = flattened.substring(0, 197) + "...";
            }
            detail = detail + ": " + flattened;
        }
        return detail;
    }

    // ══════════════════════════════════════════════
    //  Latency Map
    // ══════════════════════════════════════════════

    private static Map<MetroId, Map<MetroId, Double>> buildLatencyMap(List<Metro> metros) {
        Map<MetroId, Map<MetroId, Double>> map = new HashMap<>();
        for (Metro metro : metros) {
            Map<MetroId, Double> connections = new HashMap<>();
            if (metro.getConnectedMetros() != null) {
                for (ConnectedMetro cm : metro.getConnectedMetros()) {
                    if (cm.getAvgLatency() != null) {
                        connections.put(cm.metroId(), cm.getAvgLatency());
                    }
                }
            }
            map.put(metro.metroId(), connections);
        }
        return map;
    }

    // ══════════════════════════════════════════════
    //  Provider Metro Map
    // ══════════════════════════════════════════════

    /**
     * Builds the provider-to-metro availability index: for every requirement, the set of
     * metros in which <em>any</em> matching Fabric service profile is present.
     *
     * <p>A provider legitimately has several service profiles (different sellers, regions,
     * and products), so every matching profile contributes its metros to the union.</p>
     *
     * <p>When more than one profile covers the <em>same</em> metro exactly one of them must win,
     * because the entry's {@code serviceProfileUuid} and {@code sellerRegions} are consumed as a
     * pair: the deployment wizard plans a connection to that uuid with
     * {@code sellerRegions.get(0)} as the z-side region. Unioning the regions across profiles
     * would therefore splice a region belonging to profile B onto profile A's uuid and produce an
     * unprovisionable plan, so the entry is always taken wholesale from a single profile.</p>
     *
     * <p>The winner is chosen by a total order rather than by arrival, so the index does not
     * depend on the (unsorted) order the search endpoint happens to return profiles in:</p>
     * <ol>
     *   <li>most matches against the requirement's {@code preferredSellerRegions} &mdash; this is
     *       the rule that actually serves the preferred-seller-region bonus in
     *       {@link #scoreProviderCoverage};</li>
     *   <li>then a profile that publishes seller regions over one that publishes none;</li>
     *   <li>then the lexicographically smallest {@code serviceProfileUuid} (nulls last) purely as a
     *       deterministic tie-break, so two runs against the same catalog agree.</li>
     * </ol>
     *
     * <p>The single winner drives scoring and the default the wizard pins, but bandwidth-aware
     * profile selection needs more than the winner: a provider's hosted profile (capped at, say,
     * 500&nbsp;Mbps) can win a metro on region/uuid and then be paired with a 3000&nbsp;Mbps
     * connection it cannot build, because the winner is chosen before any connection bandwidth
     * exists. So EVERY matching profile's capability for the metro — its allowed tiers, custom-band
     * flag and per-metro ceiling — is also carried forward on the entry's
     * {@link ProviderAvailability#getProfileOptions() profileOptions}, letting the wizard choose a
     * covering profile once the bandwidth is known while {@code outranks} still decides the default.</p>
     */
    private static ProviderIndex buildProviderIndex(
            List<ProviderRequirement> requirements, List<ServiceProfile> profiles) {

        Map<String, Map<MetroId, ProviderAvailability>> result = new HashMap<>();
        Map<String, Integer> matchedCounts = new HashMap<>();

        for (ProviderRequirement req : requirements) {
            String key = req.displayLabel();
            Set<String> preferredRegions = req.getPreferredSellerRegions() != null
                    ? new HashSet<>(req.getPreferredSellerRegions()) : Collections.emptySet();
            Map<MetroId, ProviderAvailability> metroAvail = new HashMap<>();
            // Every matching profile's bandwidth capability per metro, in catalog order, so the wizard
            // can pick a covering profile by bandwidth rather than being handed only the outranks winner.
            Map<MetroId, List<ServiceProfileOption>> metroOptions = new HashMap<>();
            int matched = 0;

            for (ServiceProfile profile : profiles) {
                if (!matchesProvider(profile, req)) continue;
                // Counted before the metro check: a profile that matches the selector but publishes
                // no metros is a DIFFERENT failure from a selector that matches nothing, and the
                // diagnostics must be able to tell the caller which one actually happened.
                matched++;

                List<ServiceProfileMetro> metros = profile.metros();
                if (metros == null) continue;

                // Profile-level bandwidth capability (metro-independent): the discrete tier list
                // aggregated across every access-point-type config, plus the allow-custom escape
                // hatch. Read once per profile and mirrors PlanValidator.checkProfile's aggregation.
                List<Integer> supportedBandwidths = new ArrayList<>();
                boolean allowCustom = false;
                if (profile.getAccessPointTypeConfigs() != null) {
                    for (AccessPointTypeConfig cfg : profile.getAccessPointTypeConfigs()) {
                        if (cfg == null) continue;
                        if (Boolean.TRUE.equals(cfg.getAllowCustomBandwidth())) {
                            allowCustom = true;
                        }
                        if (cfg.getSupportedBandwidths() != null) {
                            supportedBandwidths.addAll(cfg.getSupportedBandwidths());
                        }
                    }
                }

                for (ServiceProfileMetro spm : metros) {
                    if (spm == null || spm.metroId() == null) continue;
                    List<String> regions = spm.getSellerRegions() != null
                            ? new ArrayList<>(spm.getSellerRegions().keySet())
                            : Collections.emptyList();
                    ProviderAvailability candidate = ProviderAvailability.builder()
                            .providerLabel(key)
                            .available(true)
                            .sellerRegions(regions)
                            .serviceProfileUuid(profile.getUuid())
                            .build();
                    ProviderAvailability incumbent = metroAvail.get(spm.metroId());
                    if (incumbent == null || outranks(candidate, incumbent, preferredRegions)) {
                        metroAvail.put(spm.metroId(), candidate);
                    }
                    // Carry this profile's capability (uuid + seller regions kept as a pair, its
                    // aggregated tiers, its custom-band flag, and THIS metro's ceiling) as a candidate
                    // the wizard can choose by bandwidth. The winner above is unaffected.
                    metroOptions.computeIfAbsent(spm.metroId(), k -> new ArrayList<>())
                            .add(ServiceProfileOption.builder()
                                    .serviceProfileUuid(profile.getUuid())
                                    .sellerRegions(regions)
                                    .supportedBandwidths(new ArrayList<>(supportedBandwidths))
                                    .allowCustomBandwidth(allowCustom)
                                    .vcBandwidthMax(spm.getVcBandwidthMax())
                                    .build());
                }
            }

            // Attach the accumulated candidate options onto each metro's winning availability entry,
            // so the single-uuid default and the full candidate list travel together.
            Map<MetroId, ProviderAvailability> withOptions = new HashMap<>();
            for (Map.Entry<MetroId, ProviderAvailability> entry : metroAvail.entrySet()) {
                List<ServiceProfileOption> options =
                        metroOptions.getOrDefault(entry.getKey(), Collections.emptyList());
                withOptions.put(entry.getKey(),
                        entry.getValue().toBuilder().profileOptions(options).build());
            }
            result.put(key, withOptions);
            matchedCounts.put(key, matched);
        }
        return new ProviderIndex(result, matchedCounts);
    }

    /**
     * The provider-to-metro availability index plus, per requirement label, how many Fabric service
     * profiles matched its selector at all.
     *
     * <p>Both numbers are needed to describe a requirement that resolved to no metro. An empty
     * availability entry has two quite different causes: <em>nothing in the catalog is named that</em>
     * (a lookup miss), or <em>profiles were found but none of them published any metro coverage</em>
     * (a published-coverage gap). Reporting the first when the second happened is a false statement
     * about the catalog, so the count of matched profiles travels alongside the index.</p>
     */
    private static final class ProviderIndex {
        final Map<String, Map<MetroId, ProviderAvailability>> availability;
        private final Map<String, Integer> profilesMatched;

        ProviderIndex(Map<String, Map<MetroId, ProviderAvailability>> availability,
                      Map<String, Integer> profilesMatched) {
            this.availability = availability;
            this.profilesMatched = profilesMatched;
        }

        /** {@code true} when this requirement resolved to at least one metro. */
        boolean resolvedAnyMetro(String label) {
            Map<MetroId, ProviderAvailability> avail = availability.get(label);
            return avail != null && !avail.isEmpty();
        }

        /** How many service profiles matched the requirement's selector, coverage aside. */
        int profilesMatched(String label) {
            return profilesMatched.getOrDefault(label, 0);
        }
    }

    /**
     * A requirement that resolved to no metro at all, carrying <em>why</em>: how many service
     * profiles matched its selector. Zero means nothing in the searched catalog is named that;
     * more than zero means the profiles were found but published no metro coverage.
     */
    private static final class UnresolvedProvider {
        final ProviderRequirement requirement;
        final int profilesMatched;

        UnresolvedProvider(ProviderRequirement requirement, int profilesMatched) {
            this.requirement = requirement;
            this.profilesMatched = profilesMatched;
        }

        String label() {
            return requirement.displayLabel();
        }

        /** {@code true} when profiles matched the selector but none published metro coverage. */
        boolean matchedWithoutCoverage() {
            return profilesMatched > 0;
        }
    }

    /**
     * Total order over two availability entries for the same metro and requirement. See
     * {@link #buildProviderIndex} for the ranking rationale.
     */
    private static boolean outranks(ProviderAvailability candidate, ProviderAvailability incumbent,
                                    Set<String> preferredRegions) {
        int byRegions = Integer.compare(regionFitness(candidate, preferredRegions),
                regionFitness(incumbent, preferredRegions));
        if (byRegions != 0) return byRegions > 0;
        return compareProfileUuid(candidate.getServiceProfileUuid(), incumbent.getServiceProfileUuid()) < 0;
    }

    /** Preferred-region matches dominate; publishing any seller region breaks the remaining tie. */
    private static int regionFitness(ProviderAvailability availability, Set<String> preferredRegions) {
        List<String> regions = availability.getSellerRegions();
        if (regions == null || regions.isEmpty()) return 0;
        int matches = (int) regions.stream().filter(preferredRegions::contains).count();
        return matches * 2 + 1;
    }

    /** Lexicographic uuid comparison with nulls last, so the tie-break is total. */
    private static int compareProfileUuid(String a, String b) {
        if (java.util.Objects.equals(a, b)) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }

    /**
     * Matches a Fabric service profile against a provider requirement.
     *
     * <p>An explicit {@code serviceProfileUuid} or {@code serviceProfileName} on the
     * requirement takes precedence, in that order. A {@link CloudProviderType} requirement
     * delegates to {@link CloudProviderType#matchesServiceProfileName(String)}, which
     * considers the provider's Fabric product name, its corporate name, the constant name,
     * and curated aliases. Matching on the corporate name alone does not work: marketplace
     * profiles are named after the product ("AWS Direct Connect"), not the corporation
     * ("Amazon Web Services").</p>
     */
    private static boolean matchesProvider(ServiceProfile profile, ProviderRequirement req) {
        if (req.getServiceProfileUuid() != null) {
            return req.getServiceProfileUuid().equals(profile.getUuid());
        }
        if (req.getServiceProfileName() != null) {
            return profile.getName() != null
                    && profile.getName().toLowerCase(Locale.ROOT)
                        .contains(req.getServiceProfileName().toLowerCase(Locale.ROOT));
        }
        if (req.getCloudProvider() != null) {
            return req.getCloudProvider().matchesServiceProfileName(profile.getName());
        }
        return false;
    }

    /**
     * Collects every provider requirement whose availability the run needs to know about:
     * the request-level requirements (which gate candidacy) plus the per-workload
     * {@code dependsOn} requirements (which steer placement), de-duplicated by display
     * label. Without the workload dependencies in the index, the provider-dependent
     * placement branch in {@link #assembleTopology} can never find a metro.
     */
    private static List<ProviderRequirement> collectProviderRequirements(OptimizationRequest request) {
        Map<String, ProviderRequirement> byLabel = new LinkedHashMap<>();
        for (ProviderRequirement req : request.getProviders()) {
            byLabel.putIfAbsent(req.displayLabel(), req);
        }
        for (WorkloadSpec workload : request.getWorkloads()) {
            if (workload.getDependsOnProviders() == null) continue;
            for (ProviderRequirement dep : workload.getDependsOnProviders()) {
                byLabel.putIfAbsent(dep.displayLabel(), dep);
            }
        }
        return new ArrayList<>(byLabel.values());
    }

    /**
     * Returns the request-level requirements that resolved to no metro at all, each tagged with
     * whether any service profile matched its selector. Both shapes are surfaced explicitly rather
     * than being allowed to silently empty the candidate set, and they are never conflated: "nothing
     * is named that" and "it is named but publishes no metros" call for different corrective action.
     */
    private static List<UnresolvedProvider> findUnresolvedProviders(
            OptimizationRequest request, ProviderIndex providerIndex) {
        List<UnresolvedProvider> unresolved = new ArrayList<>();
        for (ProviderRequirement req : request.getProviders()) {
            String key = req.displayLabel();
            if (!providerIndex.resolvedAnyMetro(key)) {
                unresolved.add(new UnresolvedProvider(req, providerIndex.profilesMatched(key)));
            }
        }
        return unresolved;
    }

    /**
     * Returns the <em>workload-level</em> {@code dependsOn} requirements that resolved to no metro
     * at all and are not already reported at request level. A workload dependency that resolves to
     * nothing does not empty the candidate set — it silently disarms the provider-dependent branch
     * of {@link #assembleTopology}, so the workload falls through to the highest-scored metro as if
     * it had declared no dependency. Same never-silent rule as the request-level misses.
     */
    private static List<UnresolvedProvider> findUnresolvedWorkloadProviders(
            OptimizationRequest request, ProviderIndex providerIndex) {
        Set<String> reportedAtRequestLevel = request.getProviders().stream()
                .map(ProviderRequirement::displayLabel)
                .collect(Collectors.toSet());
        Map<String, UnresolvedProvider> unresolved = new LinkedHashMap<>();
        for (WorkloadSpec workload : request.getWorkloads()) {
            if (workload.getDependsOnProviders() == null) continue;
            for (ProviderRequirement dep : workload.getDependsOnProviders()) {
                String key = dep.displayLabel();
                if (reportedAtRequestLevel.contains(key)) continue;
                if (!providerIndex.resolvedAnyMetro(key)) {
                    unresolved.putIfAbsent(key, new UnresolvedProvider(dep, providerIndex.profilesMatched(key)));
                }
            }
        }
        return new ArrayList<>(unresolved.values());
    }

    /** Describes which selector on a requirement was used to look profiles up. */
    private static String describeRequirementSelector(ProviderRequirement req) {
        if (req.getServiceProfileUuid() != null) {
            return "service profile UUID " + req.getServiceProfileUuid();
        }
        if (req.getServiceProfileName() != null) {
            return "service profile name '" + req.getServiceProfileName() + "'";
        }
        if (req.getCloudProvider() != null) {
            return "cloud provider " + req.getCloudProvider().name();
        }
        return "no provider selector";
    }

    // ══════════════════════════════════════════════
    //  Candidate Filtering
    // ══════════════════════════════════════════════

    /**
     * The result of candidate filtering: the surviving metros plus a tally of how many metros each
     * constraint eliminated. The tally exists so an empty result can name the constraint that
     * caused it instead of being reported as an unexplained absence (or, worse, as HEALTHY).
     */
    private static final class CandidateSet {
        final List<Metro> candidates;
        /** Constraint description → metros eliminated by it, in the order the checks run. */
        final Map<String, Integer> eliminations;
        final int evaluated;
        /**
         * Metros present in {@link #candidates} only because {@code requireMetro(...)} forced them
         * in. They bypassed every filter, so they met no constraint and are never counted as though
         * they had.
         */
        final List<MetroId> forceIncluded;
        /** Required metros that are not in the catalog this run read, so could not be included. */
        final List<MetroId> requiredNotFound;

        CandidateSet(List<Metro> candidates, Map<String, Integer> eliminations, int evaluated,
                     List<MetroId> forceIncluded, List<MetroId> requiredNotFound) {
            this.candidates = candidates;
            this.eliminations = eliminations;
            this.evaluated = evaluated;
            this.forceIncluded = forceIncluded;
            this.requiredNotFound = requiredNotFound;
        }

        /** How many metros actually passed every filter, force-includes excluded. */
        int passedFilters() {
            return candidates.size() - forceIncluded.size();
        }

        /** The constraint that eliminated the most metros, or empty when nothing was eliminated. */
        Optional<Map.Entry<String, Integer>> dominantConstraint() {
            return eliminations.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .max(Map.Entry.comparingByValue());
        }

        int eliminatedBy(String constraint) {
            return eliminations.getOrDefault(constraint, 0);
        }
    }

    /** Stable key for the per-provider elimination tally, shared by the filter and the risk text. */
    private static String requiredProviderConstraint(String providerLabel) {
        return "required provider '" + providerLabel + "'";
    }

    /** The request-level required providers: the classic candidacy gate, now one of two eligibility paths. */
    private static List<ProviderRequirement> requiredRequestProviders(OptimizationRequest request) {
        return request.getProviders().stream()
                .filter(ProviderRequirement::isRequired)
                .collect(Collectors.toList());
    }

    /**
     * The cloud dependencies of every workload that declares its own, one non-empty list per such
     * workload. A metro that carries all the clouds of any one of these lists can host that workload,
     * which is the second (widening) provider-eligibility path.
     */
    private static List<List<ProviderRequirement>> workloadCloudNeeds(OptimizationRequest request) {
        List<List<ProviderRequirement>> needs = new ArrayList<>();
        for (WorkloadSpec workload : request.getWorkloads()) {
            List<ProviderRequirement> deps = workload.getDependsOnProviders();
            if (deps != null && !deps.isEmpty()) {
                needs.add(deps);
            }
        }
        return needs;
    }

    /** {@code true} when the availability index places the requirement's provider in this metro. */
    private static boolean metroHasProvider(MetroId code, ProviderRequirement req,
                                            Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap) {
        Map<MetroId, ProviderAvailability> avail = providerMetroMap.get(req.displayLabel());
        return avail != null && avail.containsKey(code);
    }

    /**
     * Whether a metro qualifies on providers: it carries every request-level required cloud (vacuously
     * true when none are required), or it carries all the clouds of at least one workload that declares
     * its own dependencies. The request-level required clouds are thus <em>a</em> way to qualify, not a
     * "present in every metro" filter — their real enforcement is the across-the-set coverage check in
     * {@link #analyzeRisks}, so a single-cloud metro can still qualify to host a single-cloud workload.
     */
    private static boolean isProviderEligible(MetroId code, List<ProviderRequirement> requestRequired,
                                              List<List<ProviderRequirement>> workloadCloudNeeds,
                                              Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap) {
        boolean carriesAllRequestRequired = requestRequired.stream()
                .allMatch(req -> metroHasProvider(code, req, providerMetroMap));
        if (carriesAllRequestRequired) {
            return true;
        }
        return workloadCloudNeeds.stream()
                .anyMatch(deps -> deps.stream().allMatch(dep -> metroHasProvider(code, dep, providerMetroMap)));
    }

    private static CandidateSet filterCandidates(List<Metro> allMetros, OptimizationRequest request,
                                                 Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap,
                                                 Map<MetroId, Metro> metroMap,
                                                 Map<MetroId, Map<MetroId, Double>> latencyMap) {
        OptimizationConstraints c = request.getConstraints();
        List<Metro> filtered = new ArrayList<>();
        Map<String, Integer> eliminations = new LinkedHashMap<>();

        Double maxLatencyMs = c.getMaxLatencyMs();
        boolean latencyBounded = isLatencyBounded(maxLatencyMs, request.getSites());

        Set<MetroId> excluded = c.getExcludedMetros() != null
                ? new HashSet<>(c.getExcludedMetros()) : Collections.emptySet();
        Set<Region> excludedRegions = c.getExcludedRegions() != null
                ? new HashSet<>(c.getExcludedRegions()) : Collections.emptySet();
        Set<Region> requiredRegions = c.getRequiredRegions() != null
                ? new HashSet<>(c.getRequiredRegions()) : Collections.emptySet();

        // Compliance zone allowed regions
        Set<Region> complianceAllowed = null;
        if (c.getComplianceZones() != null && !c.getComplianceZones().isEmpty()) {
            complianceAllowed = new HashSet<>();
            for (ComplianceZone zone : c.getComplianceZones()) {
                complianceAllowed.addAll(zone.getAllowedRegions());
            }
        }

        // Provider eligibility inputs. A metro qualifies on providers when it EITHER carries every
        // request-level required cloud (the classic gate, and vacuously true when none are required)
        // OR carries all the clouds of at least one workload that declares its own dependencies. The
        // second path is what lets a single-cloud EMEA/APAC metro qualify to host a single-cloud
        // workload, instead of the old rule that forced EVERY metro to carry the union of all clouds.
        List<ProviderRequirement> requestRequired = requiredRequestProviders(request);
        List<List<ProviderRequirement>> workloadCloudNeeds = workloadCloudNeeds(request);

        for (Metro metro : allMetros) {
            MetroId code = metro.metroId();
            Region region = metro.getRegion();

            if (excluded.contains(code)) {
                count(eliminations, "excluded metros");
                continue;
            }
            if (excludedRegions.contains(region)) {
                count(eliminations, "excluded regions");
                continue;
            }
            if (!requiredRegions.isEmpty() && !requiredRegions.contains(region)) {
                count(eliminations, "required regions");
                continue;
            }
            if (complianceAllowed != null && !complianceAllowed.contains(region)) {
                count(eliminations, "compliance zones");
                continue;
            }

            // Provider eligibility: qualify a metro when it carries every request-level required
            // cloud, OR carries all the clouds of at least one workload that declares its own
            // dependencies. A metro that reaches the elimination branch below therefore carries
            // neither the full request-required set nor any single workload's clouds; every
            // request-level required cloud it lacks is tallied, so the per-provider counts the risk
            // text quotes are the real blast radius rather than a first-blamed approximation. When
            // there are no request-level required clouds the first path is vacuously true, so this
            // branch is unreachable and the tally is never silent.
            if (!isProviderEligible(code, requestRequired, workloadCloudNeeds, providerMetroMap)) {
                for (ProviderRequirement req : requestRequired) {
                    if (!metroHasProvider(code, req, providerMetroMap)) {
                        count(eliminations, requiredProviderConstraint(req.displayLabel()));
                    }
                }
                continue;
            }

            // Hard latency bound: exclude metros whose estimated latency to ANY user site
            // exceeds constraints.maxLatencyMs. Uses the same per-metro-to-site estimate
            // (estimateLatency: Fabric avgLatency, else Haversine fiber estimate) as the
            // latency score dimension and the LatencyMatrix, so filter and scoring agree.
            if (latencyBounded && exceedsLatencyBound(code, request.getSites(), maxLatencyMs, metroMap, latencyMap)) {
                count(eliminations, "the " + formatMs(maxLatencyMs) + " max-latency bound");
                continue;
            }

            filtered.add(metro);
        }

        // Ensure required metros are included. Required metros bypass every filter above,
        // including the latency bound — a required metro beyond maxLatencyMs is surfaced
        // as a LATENCY_THRESHOLD risk finding rather than excluded. Which metros were forced in
        // (and which could not be found at all) is recorded, because a force-included metro met no
        // constraint and must never be reported as one that did.
        List<MetroId> forceIncluded = new ArrayList<>();
        List<MetroId> requiredNotFound = new ArrayList<>();
        if (c.getRequiredMetros() != null) {
            Set<MetroId> filteredCodes = filtered.stream()
                    .map(Metro::metroId).collect(Collectors.toSet());
            for (MetroId required : c.getRequiredMetros()) {
                if (filteredCodes.contains(required)) continue;
                Optional<Metro> found = allMetros.stream()
                        .filter(m -> java.util.Objects.equals(m.metroId(), required))
                        .findFirst();
                if (found.isPresent()) {
                    filtered.add(found.get());
                    forceIncluded.add(required);
                }
                else if (!requiredNotFound.contains(required)) {
                    requiredNotFound.add(required);
                }
            }
        }

        return new CandidateSet(filtered, eliminations, allMetros.size(), forceIncluded, requiredNotFound);
    }

    private static void count(Map<String, Integer> tally, String key) {
        tally.merge(key, 1, Integer::sum);
    }

    /**
     * Formats a millisecond figure without inventing or destroying precision: whole values read as
     * "20ms", fractional ones keep a decimal ("0.1ms") instead of being rounded to a "0ms" bound
     * that no metro could ever satisfy.
     */
    private static String formatMs(double ms) {
        return ms == Math.rint(ms) ? String.format("%.0fms", ms) : String.format("%.1fms", ms);
    }

    /**
     * Renders a monetary amount with its currency code, e.g. {@code "12000 USD"}, without inventing
     * precision: a whole amount reads without decimals, a fractional one keeps two. A null currency is
     * omitted rather than printed as the word "null".
     */
    private static String formatMoney(BigDecimal amount, String currency) {
        BigDecimal scaled = amount.stripTrailingZeros();
        String number = scaled.scale() <= 0 ? scaled.toBigInteger().toString()
                : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        return currency != null && !currency.isBlank() ? number + " " + currency : number;
    }

    /**
     * Returns {@code true} when the max-latency bound is evaluable for this request: a positive
     * bound was set <em>and</em> there is at least one user site to measure the distance to.
     *
     * <p>This is the single predicate both the hard candidate filter and the
     * {@code LATENCY_THRESHOLD} risk surfacing consult, so the two can never disagree: with no
     * sites (or no usable bound) the filter excludes nothing and the risk analysis flags
     * nothing, because there is no evidence either way. A run in that state does not stay quiet
     * about it &mdash; {@code analyzeRisks} raises {@code LATENCY_BOUND_NOT_EVALUATED} and the
     * methodology says the bound was not applied.</p>
     *
     * <p>Site <em>weights</em> are deliberately not consulted here, unlike in
     * {@link SiteWeighting}: the bound is a per-site maximum, not a weighted mean, so a site with
     * zero weight still has a real distance that the bound is entitled to reject.</p>
     *
     * <p>A non-positive bound cannot reach this method: {@link #validateRequest} rejects it as an
     * input error rather than letting it read as "unbounded". The positivity check is retained as a
     * belt-and-braces invariant.</p>
     */
    private static boolean isLatencyBounded(Double maxLatencyMs, List<UserSite> sites) {
        return maxLatencyMs != null && isUsableLatencyMs(maxLatencyMs) && sites != null && !sites.isEmpty();
    }

    /**
     * The worst (highest) estimated latency from a metro to any user site, or empty when there are no
     * sites to measure to. Same estimate as {@link #exceedsLatencyBound} and the {@link LatencyMatrix},
     * so the global bound, the per-workload tolerance, and the reported matrix cannot disagree.
     */
    private static OptionalDouble worstCaseLatencyMs(MetroId metro, List<UserSite> sites,
                                                     Map<MetroId, Metro> metroMap,
                                                     Map<MetroId, Map<MetroId, Double>> latencyMap) {
        if (sites == null || sites.isEmpty()) return OptionalDouble.empty();
        double worst = Double.NEGATIVE_INFINITY;
        for (UserSite site : sites) {
            worst = Math.max(worst, estimateLatency(metro, site, metroMap, latencyMap));
        }
        return Double.isFinite(worst) ? OptionalDouble.of(worst) : OptionalDouble.empty();
    }

    /**
     * Returns {@code true} if the metro's estimated latency to <em>any</em> user site exceeds
     * the {@code maxLatencyMs} bound. Uses {@link #estimateLatency}, the same estimate the
     * latency score dimension and {@link LatencyMatrix} are built from (Fabric metro-to-metro
     * {@code avgLatency} where published, otherwise the Haversine fiber-distance estimate),
     * so the hard filter and the scoring path can never disagree about a metro's latency.
     */
    private static boolean exceedsLatencyBound(MetroId metro, List<UserSite> sites, double maxLatencyMs,
                                               Map<MetroId, Metro> metroMap,
                                               Map<MetroId, Map<MetroId, Double>> latencyMap) {
        for (UserSite site : sites) {
            if (estimateLatency(metro, site, metroMap, latencyMap) > maxLatencyMs) {
                return true;
            }
        }
        return false;
    }

    // ══════════════════════════════════════════════
    //  Site Weighting
    // ══════════════════════════════════════════════

    /**
     * The per-site weights every proximity-driven calculation shares: latency scoring, the
     * latency-critical placement rule, and the explanations for both.
     *
     * <p>{@link UserSite#effectiveWeight(int)} returns 0 for a site with neither an explicit weight
     * nor any headcount, and there are callers &mdash; the {@code design_optimize_placement} MCP
     * tool among them, whose site schema exposes {@code weight} and no headcount at all &mdash; that
     * routinely leave both unset. Weighting straight off that value gave such a site weight 0, which
     * silently deleted it from every proximity calculation while the report still listed its
     * latencies and promised proximity-driven placement.</p>
     *
     * <p>The fallback is therefore applied <strong>per site</strong>, not only when the whole request
     * is unweighted &mdash; a mixed request (one site weighted, one not) is the shape the MCP tool
     * produces most often, and it must not silently zero the second site. A site with no usable
     * weight is treated as an <em>average site for its {@link SiteRole}</em>: the stated weight per
     * unit of role importance, times its own role multiplier. When no site states a weight that unit
     * is 1.0, so an entirely unweighted request reduces exactly to role-importance weighting.</p>
     *
     * <p>{@link #isImplied()} is set when <em>any</em> site's weight was inferred, and
     * {@link #provenance()} names those sites, so the emitted text tells the truth in the mixed case
     * instead of implying every weight was given.</p>
     *
     * <p>Because every {@link SiteRole} multiplier is positive (and a null role reads as 1.0), a
     * non-empty site list always yields a positive total: {@link #isMeasurable()} is false only for
     * a request with no sites at all, which is the single condition the zero-site guards in this
     * engine test.</p>
     */
    private static final class SiteWeighting {

        private final List<UserSite> sites;
        private final double[] weights;
        private final double total;
        private final boolean[] implied;
        private final int statedCount;

        private SiteWeighting(List<UserSite> sites, double[] weights, double total,
                              boolean[] implied, int statedCount) {
            this.sites = sites;
            this.weights = weights;
            this.total = total;
            this.implied = implied;
            this.statedCount = statedCount;
        }

        static SiteWeighting of(List<UserSite> sites, int totalHeadcount) {
            List<UserSite> safe = sites != null ? sites : Collections.emptyList();
            double[] weights = new double[safe.size()];
            boolean[] implied = new boolean[safe.size()];
            double statedTotal = 0;
            double statedRoleTotal = 0;
            int statedCount = 0;

            for (int i = 0; i < safe.size(); i++) {
                double stated = safe.get(i).effectiveWeight(totalHeadcount);
                double role = roleMultiplier(safe.get(i));
                if (stated > 0 && Double.isFinite(stated)) {
                    weights[i] = stated * role;
                    statedTotal += weights[i];
                    statedRoleTotal += role;
                    statedCount++;
                }
                else {
                    implied[i] = true;
                }
            }

            // Stated weight per unit of role importance: an unweighted site counts as an average
            // site for its role rather than as nothing. With no stated weights at all this is 1.0,
            // which reduces to weighting by role importance alone.
            double statedPerRoleUnit = statedRoleTotal > 0 ? statedTotal / statedRoleTotal : 1.0;
            if (!Double.isFinite(statedPerRoleUnit) || statedPerRoleUnit <= 0) {
                statedPerRoleUnit = 1.0;
            }
            double total = 0;
            for (int i = 0; i < safe.size(); i++) {
                if (implied[i]) {
                    weights[i] = statedPerRoleUnit * roleMultiplier(safe.get(i));
                }
                total += weights[i];
            }
            return new SiteWeighting(safe, weights, total, implied, statedCount);
        }

        private static double roleMultiplier(UserSite site) {
            return site.getRole() != null ? site.getRole().getImportanceMultiplier() : 1.0;
        }

        /**
         * {@code true} when the sites carry a positive total weight, i.e. whenever there is at least
         * one site: the per-site fallback guarantees every site a positive weight.
         */
        boolean isMeasurable() {
            return total > 0;
        }

        /** {@code true} when at least one site's weight was inferred rather than supplied. */
        boolean isImplied() {
            return statedCount < sites.size();
        }

        /**
         * The weighted mean latency from a metro to the sites, or {@link Double#NaN} when there is
         * nothing measurable to average — never a sentinel that could be mistaken for a reading.
         */
        double weightedMeanLatencyMs(MetroId metro, Map<MetroId, Metro> metroMap,
                                     Map<MetroId, Map<MetroId, Double>> latencyMap) {
            if (!isMeasurable()) return Double.NaN;
            double weighted = 0;
            for (int i = 0; i < sites.size(); i++) {
                weighted += estimateLatency(metro, sites.get(i), metroMap, latencyMap) * weights[i];
            }
            double mean = weighted / total;
            return Double.isFinite(mean) ? mean : Double.NaN;
        }

        /**
         * A clause naming where the weights came from, for the emitted explanations. The mixed case
         * names the sites that were inferred: "weighted by headcount/explicit weight" would be a
         * false statement about them, and silence about them was the original defect.
         */
        String provenance() {
            if (statedCount == sites.size()) {
                return "sites weighted by headcount/explicit weight and role";
            }
            if (statedCount == 0) {
                return "sites weighted by role only (no headcount or explicit weight was given)";
            }
            return "sites weighted by headcount/explicit weight and role, except "
                    + impliedLabels() + ", which carry neither a headcount nor an explicit weight and "
                    + "were weighted as an average site for their role";
        }

        /** The labels of the sites whose weight was inferred, in declaration order. */
        private String impliedLabels() {
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < sites.size(); i++) {
                if (implied[i]) {
                    labels.add("'" + sites.get(i).getLabel() + "'");
                }
            }
            return String.join(", ", labels);
        }
    }

    // ══════════════════════════════════════════════
    //  Latency Scoring
    // ══════════════════════════════════════════════

    private static double scoreLatency(Metro candidate, OptimizationRequest request,
                                       Map<MetroId, Metro> metroMap,
                                       Map<MetroId, Map<MetroId, Double>> latencyMap,
                                       SiteWeighting siteWeighting, ScoringWeights weights) {
        double avgWeightedLatency = siteWeighting.weightedMeanLatencyMs(
                candidate.metroId(), metroMap, latencyMap);
        // Nothing to measure against (no sites at all): latency cannot rank metros, so it is
        // deliberately neutral. describeLatencyScore says so in the same breath.
        if (Double.isNaN(avgWeightedLatency)) return 100.0;

        // Score using configurable thresholds
        double excellent = weights.resolveLatencyExcellentMs();
        double good = weights.resolveLatencyGoodMs();
        double acceptable = weights.resolveLatencyAcceptableMs();
        double poor = weights.resolveLatencyPoorMs();

        if (avgWeightedLatency <= excellent) return 95.0 + 5.0 * (1.0 - avgWeightedLatency / excellent);
        if (avgWeightedLatency <= good) return 75.0 + 20.0 * (1.0 - (avgWeightedLatency - excellent) / (good - excellent));
        if (avgWeightedLatency <= acceptable) return 50.0 + 25.0 * (1.0 - (avgWeightedLatency - good) / (acceptable - good));
        if (avgWeightedLatency <= poor) return 25.0 * (1.0 - (avgWeightedLatency - acceptable) / (poor - acceptable));
        return 0.0;
    }

    /**
     * Renders the per-site latencies behind a metro's latency score, and says where the weighting
     * that turned them into that score came from. The provenance clause is not decoration: when the
     * caller supplies neither headcount nor weight the weighting is inferred, and a reader
     * comparing this line against the score is entitled to know that.
     */
    private static String describeLatencyScore(Metro candidate, OptimizationRequest request,
                                               Map<MetroId, Metro> metroMap,
                                               Map<MetroId, Map<MetroId, Double>> latencyMap,
                                               SiteWeighting siteWeighting) {
        List<UserSite> sites = request.getSites();
        // The only state in which latency cannot rank metros. There is deliberately no second
        // "sites defined but unweighted" branch: SiteWeighting now gives every site a positive
        // weight, so such a branch was unreachable — and its text ("latency could not rank metros")
        // was contradicted by the zero-weight sites that demonstrably do rank them.
        if (sites.isEmpty()) return "No sites defined";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sites.size(); i++) {
            if (i > 0) sb.append(", ");
            double lat = estimateLatency(candidate.metroId(), sites.get(i), metroMap, latencyMap);
            sb.append(String.format("%s: %.1fms", sites.get(i).getLabel(), lat));
        }
        if (siteWeighting.isImplied()) {
            sb.append(" (").append(siteWeighting.provenance()).append(")");
        }
        return sb.toString();
    }

    /**
     * Estimates latency from a metro to a user site. Tries direct metro-to-metro lookups first,
     * then falls back to Haversine distance-based estimation using fiber optic constants.
     */
    static double estimateLatency(MetroId from, UserSite site,
                                          Map<MetroId, Metro> metroMap,
                                          Map<MetroId, Map<MetroId, Double>> latencyMap) {
        MetroId siteMetro = site.getNearestMetro();

        // If the site has a metro code, try direct latency lookup
        if (siteMetro != null) {
            if (siteMetro.equals(from)) return 0.5; // same metro, sub-ms

            Map<MetroId, Double> fromConnections = latencyMap.get(from);
            if (fromConnections != null) {
                Double directLatency = fromConnections.get(siteMetro);
                if (directLatency != null) return directLatency;
            }

            // Reverse lookup
            Map<MetroId, Double> siteConnections = latencyMap.get(siteMetro);
            if (siteConnections != null) {
                Double reverseLatency = siteConnections.get(from);
                if (reverseLatency != null) return reverseLatency;
            }
        }

        // Fall back to geo-distance estimation
        GeoCoordinate fromCoord = metroMap.containsKey(from) ? metroMap.get(from).geoCoordinates() : null;
        Double siteLat = site.getLatitude();
        Double siteLng = site.getLongitude();

        // If site has no coords but has a metro, use metro coords
        if ((siteLat == null || siteLng == null) && siteMetro != null && metroMap.containsKey(siteMetro)) {
            GeoCoordinate siteCoord = metroMap.get(siteMetro).geoCoordinates();
            if (siteCoord != null) {
                siteLat = siteCoord.getLatitude();
                siteLng = siteCoord.getLongitude();
            }
        }

        if (fromCoord != null && siteLat != null && siteLng != null
                && fromCoord.getLatitude() != null && fromCoord.getLongitude() != null) {
            double distKm = haversine(fromCoord.getLatitude(), fromCoord.getLongitude(), siteLat, siteLng);
            double fiberDistKm = distKm * FIBER_DISTANCE_MULTIPLIER;
            return (fiberDistKm * FIBER_LATENCY_US_PER_KM) / 1000.0; // convert μs to ms
        }

        // Absolute fallback: assume high latency
        return 150.0;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    // ══════════════════════════════════════════════
    //  Provider Coverage Scoring
    // ══════════════════════════════════════════════

    private static double scoreProviderCoverage(Metro candidate, OptimizationRequest request,
                                                Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap,
                                                ScoringWeights weights) {
        if (request.getProviders().isEmpty()) return 100.0;

        double totalWeight = 0;
        double matchedWeight = 0;
        double reqWeight = weights.resolveRequiredProviderWeight();

        for (ProviderRequirement req : request.getProviders()) {
            double provWeight = req.isRequired() ? reqWeight : 1.0;
            totalWeight += provWeight;

            String key = req.displayLabel();
            Map<MetroId, ProviderAvailability> avail = providerMetroMap.get(key);
            if (avail != null && avail.containsKey(candidate.metroId())) {
                matchedWeight += provWeight;

                // Bonus for matching preferred seller regions
                if (req.getPreferredSellerRegions() != null && !req.getPreferredSellerRegions().isEmpty()) {
                    ProviderAvailability pa = avail.get(candidate.metroId());
                    long regionMatches = req.getPreferredSellerRegions().stream()
                            .filter(r -> pa.getSellerRegions() != null && pa.getSellerRegions().contains(r))
                            .count();
                    double regionBonus = (double) regionMatches / req.getPreferredSellerRegions().size() * 10.0;
                    // reqWeight can legitimately be 0 (a caller may zero the required-provider weight),
                    // which would make regionBonus/reqWeight a division by zero — NaN/Infinity that then
                    // poisons matchedWeight and every composite score derived from it. The bonus is
                    // capped at provWeight*0.1 anyway, so fall back to that cap when reqWeight is 0
                    // rather than propagating a non-finite score.
                    double cappedBonus = reqWeight > 0
                            ? Math.min(regionBonus * provWeight / reqWeight, provWeight * 0.1)
                            : provWeight * 0.1;
                    matchedWeight += cappedBonus;
                }
            }
        }

        return totalWeight > 0 ? Math.min(100.0, (matchedWeight / totalWeight) * 100.0) : 100.0;
    }

    /**
     * Describes the provider-coverage score. With no providers requested the score is a full 100
     * (nothing was asked for, so nothing is missing); saying "0/0 providers available" alongside
     * that read as a total coverage failure sitting next to a perfect score.
     */
    private static String describeProviderScore(Metro candidate, OptimizationRequest request,
                                                Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap) {
        if (request.getProviders().isEmpty()) {
            return "No providers were required or preferred, so provider coverage does not "
                    + "differentiate metros (scored 100)";
        }
        long available = request.getProviders().stream()
                .filter(req -> {
                    Map<MetroId, ProviderAvailability> avail = providerMetroMap.get(req.displayLabel());
                    return avail != null && avail.containsKey(candidate.metroId());
                })
                .count();
        return available + "/" + request.getProviders().size() + " providers available";
    }

    // ══════════════════════════════════════════════
    //  Cost Scoring
    // ══════════════════════════════════════════════

    private static double scoreCost(Metro candidate, OptimizationRequest request) {
        // Cost scoring uses a simplified model based on region.
        // AMER metros are baseline, EMEA and APAC carry a regional premium.
        Region region = candidate.getRegion();
        if (region == null) return 70.0;

        switch (region) {
            case AMER: return 80.0;
            case EMEA: return 65.0;
            case APAC: return 55.0;
            default: return 70.0;
        }
    }

    // ══════════════════════════════════════════════
    //  Compliance Scoring
    // ══════════════════════════════════════════════

    private static double scoreCompliance(Metro candidate, OptimizationRequest request) {
        OptimizationConstraints c = request.getConstraints();
        if (c.getComplianceZones() == null || c.getComplianceZones().isEmpty()) return 100.0;

        Region metroRegion = candidate.getRegion();
        for (ComplianceZone zone : c.getComplianceZones()) {
            if (!zone.getAllowedRegions().contains(metroRegion)) {
                return 0.0;
            }
        }
        return 100.0;
    }

    // ══════════════════════════════════════════════
    //  Redundancy Refinement
    // ══════════════════════════════════════════════

    /**
     * Recomputes the redundancy dimension from the geographic diversity of the metros actually
     * selected, replacing the placeholder score and explanation stamped during scoring.
     *
     * <p>A single-metro selection is refined too. Returning early for it left the placeholder
     * explanation "Baseline redundancy score; refined after topology assembly" in the shipped
     * payload &mdash; an assertion that a refinement had happened when none had, on the one result
     * shape where redundancy is most consequential. The ladder below already has a branch for a
     * single metro; it is now used, and the explanation states the absence of redundancy plainly.</p>
     */
    private static List<ScoredMetro> refineRedundancyScores(List<ScoredMetro> selected,
                                                             OptimizationRequest request,
                                                             double wRedundancy) {
        if (selected.isEmpty()) return selected;

        Set<Region> regions = selected.stream()
                .map(sm -> sm.metro.getRegion())
                .collect(Collectors.toSet());

        RedundancyTier requested = request.getConstraints().getMinimumRedundancy();
        if (requested == null) requested = RedundancyTier.NONE;

        double redundancyScore;
        if (regions.size() >= 3 && selected.size() >= 3) {
            redundancyScore = 100.0;
        } else if (regions.size() >= 2 && selected.size() >= 2) {
            redundancyScore = requested == RedundancyTier.MULTI_REGION ? 70.0 : 90.0;
        } else if (selected.size() >= 2) {
            redundancyScore = requested.ordinal() <= RedundancyTier.N_PLUS_1.ordinal() ? 80.0 : 50.0;
        } else {
            redundancyScore = requested == RedundancyTier.NONE ? 60.0 : 20.0;
        }

        // Rebuild scores with refined redundancy
        List<ScoredMetro> refined = new ArrayList<>();
        for (ScoredMetro sm : selected) {
            List<ScoreComponent> newComponents = new ArrayList<>();
            for (ScoreComponent comp : sm.score.getComponents()) {
                if (comp.getCategory() == ScoreCategory.REDUNDANCY) {
                    newComponents.add(ScoreComponent.builder()
                            .category(ScoreCategory.REDUNDANCY)
                            .score(redundancyScore)
                            .weight(wRedundancy)
                            .explanation(describeRedundancy(selected, regions))
                            .build());
                } else {
                    newComponents.add(comp);
                }
            }
            double newComposite = newComponents.stream().mapToDouble(ScoreComponent::weightedScore).sum();
            refined.add(new ScoredMetro(sm.metro, new MetroScore(newComposite, newComponents)));
        }

        refined.sort((a, b) -> Double.compare(b.score.getComposite(), a.score.getComposite()));
        return refined;
    }

    private static String describeRedundancy(List<ScoredMetro> selected, Set<Region> regions) {
        if (selected.size() == 1) {
            Metro only = selected.get(0).metro;
            return "Single metro (" + only.metroId() + " in " + only.getRegion() + "): no geographic "
                    + "redundancy, so a metro-level outage takes the whole deployment with it";
        }
        return selected.size() + " metros across " + regions.size() + " region(s): "
                + regions.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    /**
     * Fills in the latency and provider-coverage score explanations that were deferred
     * during the scoring phase, but only for the metros that were actually selected.
     *
     * <p>The explanations are descriptive only — they have no bearing on the composite
     * score — so the per-metro composite and overall ranking are preserved exactly. This
     * avoids running {@link #describeLatencyScore} / {@link #describeProviderScore} (each
     * of which re-estimates latency per user site) for every candidate when only the top
     * {@code maxMetros} are surfaced.</p>
     */
    private static List<ScoredMetro> backfillScoreDescriptions(
            List<ScoredMetro> selected, OptimizationRequest request,
            Map<MetroId, Metro> metroMap, Map<MetroId, Map<MetroId, Double>> latencyMap,
            Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap, SiteWeighting siteWeighting) {

        List<ScoredMetro> result = new ArrayList<>(selected.size());
        for (ScoredMetro sm : selected) {
            List<ScoreComponent> newComponents = new ArrayList<>(sm.score.getComponents().size());
            for (ScoreComponent comp : sm.score.getComponents()) {
                if (comp.getCategory() == ScoreCategory.LATENCY) {
                    newComponents.add(ScoreComponent.builder()
                            .category(ScoreCategory.LATENCY)
                            .score(comp.getScore())
                            .weight(comp.getWeight())
                            .explanation(describeLatencyScore(sm.metro, request, metroMap, latencyMap, siteWeighting))
                            .build());
                } else if (comp.getCategory() == ScoreCategory.PROVIDER_COVERAGE) {
                    newComponents.add(ScoreComponent.builder()
                            .category(ScoreCategory.PROVIDER_COVERAGE)
                            .score(comp.getScore())
                            .weight(comp.getWeight())
                            .explanation(describeProviderScore(sm.metro, request, providerMetroMap))
                            .build());
                } else {
                    newComponents.add(comp);
                }
            }
            // Composite is unchanged: descriptions do not contribute to weightedScore().
            result.add(new ScoredMetro(sm.metro, new MetroScore(sm.score.getComposite(), newComponents)));
        }
        return result;
    }

    // ══════════════════════════════════════════════
    //  Latency Matrix
    // ══════════════════════════════════════════════

    private static LatencyMatrix buildLatencyMatrix(List<ScoredMetro> selected, OptimizationRequest request,
                                                     Map<MetroId, Metro> metroMap,
                                                     Map<MetroId, Map<MetroId, Double>> latencyMap) {
        List<MetroId> metroCodes = selected.stream()
                .map(sm -> sm.metro.metroId())
                .collect(Collectors.toList());
        List<String> siteLabels = request.getSites().stream()
                .map(UserSite::getLabel)
                .collect(Collectors.toList());

        List<List<LatencyEntry>> matrix = new ArrayList<>();
        for (ScoredMetro sm : selected) {
            List<LatencyEntry> row = new ArrayList<>();
            for (UserSite site : request.getSites()) {
                double latency = estimateLatency(sm.metro.metroId(), site, metroMap, latencyMap);
                boolean estimated = isEstimatedLatency(sm.metro.metroId(), site, latencyMap);
                row.add(new LatencyEntry(sm.metro.metroId(), site.getLabel(), latency, estimated));
            }
            matrix.add(row);
        }

        return new LatencyMatrix(metroCodes, siteLabels, matrix);
    }

    private static boolean isEstimatedLatency(MetroId from, UserSite site,
                                               Map<MetroId, Map<MetroId, Double>> latencyMap) {
        MetroId siteMetro = site.getNearestMetro();
        if (siteMetro == null) return true;
        if (siteMetro.equals(from)) return false;
        Map<MetroId, Double> connections = latencyMap.get(from);
        if (connections != null && connections.containsKey(siteMetro)) return false;
        Map<MetroId, Double> reverse = latencyMap.get(siteMetro);
        return reverse == null || !reverse.containsKey(from);
    }

    // ══════════════════════════════════════════════
    //  Provider Connectivity Map
    // ══════════════════════════════════════════════

    private static ProviderConnectivityMap buildProviderConnectivityMap(
            List<ScoredMetro> selected, OptimizationRequest request,
            Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap) {

        Map<MetroId, List<ProviderAvailability>> map = new LinkedHashMap<>();
        for (ScoredMetro sm : selected) {
            MetroId code = sm.metro.metroId();
            List<ProviderAvailability> provList = new ArrayList<>();
            for (ProviderRequirement req : request.getProviders()) {
                String key = req.displayLabel();
                Map<MetroId, ProviderAvailability> avail = providerMetroMap.get(key);
                if (avail != null && avail.containsKey(code)) {
                    provList.add(avail.get(code));
                } else {
                    provList.add(ProviderAvailability.builder()
                            .providerLabel(key)
                            .available(false)
                            .sellerRegions(Collections.emptyList())
                            .serviceProfileUuid(null)
                            .profileOptions(Collections.emptyList())
                            .build());
                }
            }
            map.put(code, provList);
        }
        return new ProviderConnectivityMap(map);
    }

    // ══════════════════════════════════════════════
    //  Topology Assembly
    // ══════════════════════════════════════════════

    /**
     * Assigns each workload to one of the selected metros.
     *
     * <p>Per workload the candidate set is first narrowed to the metros that honour that workload's
     * own {@code maxLatencyToleranceMs} ceiling — the documented contract of the lever — and the
     * placement rules (DR diversity, proximity, provider dependency, highest score) then run over
     * that narrowed set. A tolerance no metro can honour is not silently dropped: the placement says
     * which the closest metro was and by how much it misses, and {@link #analyzeRisks} raises the
     * matching finding. Recorded facility requirements (high power density, liquid cooling) are
     * carried onto the rationale for the same reason: they are accepted from the caller, so they must
     * appear in the output rather than vanish.</p>
     */
    private static DeploymentTopology assembleTopology(List<ScoredMetro> selected, OptimizationRequest request,
                                                       Map<MetroId, Metro> metroMap,
                                                       Map<MetroId, Map<MetroId, Double>> latencyMap,
                                                       Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap,
                                                       SiteWeighting siteWeighting) {
        List<WorkloadPlacement> placements = new ArrayList<>();
        if (selected.isEmpty()) return new DeploymentTopology(placements);

        for (WorkloadSpec workload : request.getWorkloads()) {
            WorkloadProfile profile = workload.resolvedProfile();
            LatencySensitivity sensitivity = profile.getDefaultLatencySensitivity();

            // The workload's own hard latency ceiling, applied to the metros it may be placed in.
            ToleranceScope scope = ToleranceScope.of(workload, profile, selected, request.getSites(),
                    metroMap, latencyMap);
            List<ScoredMetro> eligible = scope.eligible;
            String suffix = scope.clause + facilityRequirementsClause(profile);

            MetroId fallbackMetro = eligible.get(0).metro.metroId();
            Region primaryRegion = eligible.get(0).metro.getRegion();

            // DR / cold-backup workloads: place in a different region for geographic diversity — and,
            // when the workload declares its own cloud dependencies, in a different-region metro that
            // actually carries them, so the recovery site can reach its data instead of being placed
            // for diversity alone and silently stranded from its clouds. The rationale states which of
            // the two goals had to give when they cannot both be met.
            if (workload.getType() == WorkloadType.DISASTER_RECOVERY
                    || workload.getType() == WorkloadType.COLD_BACKUP) {
                List<ProviderRequirement> drDeps = workload.getDependsOnProviders();
                boolean hasDeps = drDeps != null && !drDeps.isEmpty();
                java.util.function.Predicate<ScoredMetro> carriesDeps = sm -> !hasDeps
                        || drDeps.stream().allMatch(dep -> metroHasProvider(sm.metro.metroId(), dep, providerMetroMap));

                ScoredMetro diffRegionWithDeps = eligible.stream()
                        .filter(sm -> sm.metro.getRegion() != primaryRegion).filter(carriesDeps)
                        .findFirst().orElse(null);
                ScoredMetro anyWithDeps = eligible.stream().filter(carriesDeps).findFirst().orElse(null);
                ScoredMetro diffRegion = eligible.stream()
                        .filter(sm -> sm.metro.getRegion() != primaryRegion).findFirst().orElse(null);

                ScoredMetro drMetro;
                String drReason;
                if (diffRegionWithDeps != null) {
                    drMetro = diffRegionWithDeps;
                    drReason = "Placed in " + drMetro.metro.getRegion() + " for geographic diversity from "
                            + "primary" + (hasDeps ? ", where its required providers are available" : "");
                }
                else if (hasDeps && anyWithDeps != null) {
                    drMetro = anyWithDeps;
                    drReason = "Placed in " + drMetro.metro.getRegion() + ", which carries this "
                            + "workload's providers (" + depLabels(drDeps) + "); no different-region "
                            + "metro carries them, so cross-region diversity from the primary region "
                            + "was not achievable for it";
                }
                else if (diffRegion != null) {
                    drMetro = diffRegion;
                    drReason = "Placed in " + drMetro.metro.getRegion() + " for geographic diversity from "
                            + "primary" + (hasDeps ? "; NOTE its declared providers (" + depLabels(drDeps)
                                + ") are not available in any recommended metro, so the recovery site "
                                + "cannot reach them here" : "");
                }
                else {
                    drMetro = eligible.size() > 1 ? eligible.get(eligible.size() - 1) : eligible.get(0);
                    drReason = "Placed in " + drMetro.metro.getRegion() + " (no other region is available "
                            + "for geographic diversity)" + (hasDeps && !carriesDeps.test(drMetro)
                                ? "; NOTE its declared providers (" + depLabels(drDeps)
                                    + ") are not available there" : "");
                }
                placements.add(WorkloadPlacement.builder()
                        .workloadLabel(workload.getLabel())
                        .assignedMetro(drMetro.metro.metroId())
                        .reasoning(drReason + suffix)
                        .build());
                continue;
            }

            // Latency-critical or proximity-weighted: place closest to weighted site center.
            // Gated on there being a measurable proximity signal — at least one site — not on the
            // weights being non-zero: every site now carries a positive weight, so a request with
            // sites always produces a real weighted mean rather than a division that never happened.
            // A request with no sites falls through to the provider-dependent and highest-score rules.
            if ((sensitivity == LatencySensitivity.CRITICAL || profile.isProximityWeighted())
                    && siteWeighting.isMeasurable()) {
                ScoredMetro bestLatency = null;
                double bestAvg = Double.NaN;
                for (ScoredMetro sm : eligible) {
                    double avg = siteWeighting.weightedMeanLatencyMs(sm.metro.metroId(), metroMap, latencyMap);
                    // NaN never wins a comparison, so an unmeasurable metro is skipped explicitly
                    // rather than being carried by a sentinel seed into the rationale text.
                    if (Double.isNaN(avg)) continue;
                    if (bestLatency == null || avg < bestAvg) {
                        bestAvg = avg;
                        bestLatency = sm;
                    }
                }
                if (bestLatency != null) {
                    placements.add(WorkloadPlacement.builder()
                            .workloadLabel(workload.getLabel())
                            .assignedMetro(bestLatency.metro.metroId())
                            .reasoning("Lowest weighted latency to user sites ("
                                    + String.format("%.1fms avg", bestAvg) + "; "
                                    + siteWeighting.provenance() + ")" + suffix)
                            .build());
                    continue;
                }
                // No metro produced a measurable average: fall through rather than invent one.
            }

            // Provider-dependent workloads: place where all dependencies are available
            List<ProviderRequirement> dependencies = workload.getDependsOnProviders();
            if (dependencies != null && !dependencies.isEmpty()) {
                ScoredMetro bestProvider = null;
                for (ScoredMetro sm : eligible) {
                    boolean allAvailable = dependencies.stream().allMatch(dep -> {
                        String key = dep.displayLabel();
                        Map<MetroId, ProviderAvailability> avail = providerMetroMap.get(key);
                        return avail != null && avail.containsKey(sm.metro.metroId());
                    });
                    if (allAvailable) {
                        bestProvider = sm;
                        break;
                    }
                }
                if (bestProvider != null) {
                    placements.add(WorkloadPlacement.builder()
                            .workloadLabel(workload.getLabel())
                            .assignedMetro(bestProvider.metro.metroId())
                            .reasoning("All required providers available" + suffix)
                            .build());
                    continue;
                }
                // The dependency rule could not be honoured anywhere. Say so on the placement
                // itself: silently degrading to the highest-scored metro reads identically to a
                // workload that declared no dependency at all.
                placements.add(WorkloadPlacement.builder()
                        .workloadLabel(workload.getLabel())
                        .assignedMetro(fallbackMetro)
                        .reasoning(scope.highestScoredLead + "; its declared provider "
                                + "dependencies (" + dependencies.stream()
                                        .map(ProviderRequirement::displayLabel)
                                        .collect(Collectors.joining(", "))
                                + ") are not all available in any recommended metro" + suffix)
                        .build());
                continue;
            }

            // Default: place in highest-scored metro
            placements.add(WorkloadPlacement.builder()
                    .workloadLabel(workload.getLabel())
                    .assignedMetro(fallbackMetro)
                    .reasoning(scope.highestScoredLead + suffix)
                    .build());
        }

        return new DeploymentTopology(placements);
    }

    /**
     * The metros one workload may be placed in once its own {@code maxLatencyToleranceMs} ceiling is
     * applied, plus the clause that states what the ceiling did.
     *
     * <p>{@code WorkloadProfile.maxLatencyToleranceMs} is documented — including in the
     * {@code design_optimize_placement} tool schema, as "hard latency ceiling from user sites to the
     * workload" — and was read by nothing. It is a per-workload ceiling, not a request-wide one, so
     * it narrows placement rather than candidacy: the metros recommended for the deployment as a whole
     * are unchanged, but a workload with a 20ms ceiling is not dropped into a 60ms metro while the
     * report calls it "the highest-scored metro" and says nothing about the ceiling.</p>
     */
    private static final class ToleranceScope {

        /** The metros this workload may be placed in once its own ceiling is applied. */
        final List<ScoredMetro> eligible;
        /** A clause stating what the ceiling did, appended to the placement rationale. */
        final String clause;
        /**
         * How to describe "place it in the best metro" for this workload. Once a tolerance has
         * narrowed the set, "the highest-scored metro" would name a metro the workload was NOT
         * placed in, so the phrase says which set the winner was drawn from.
         */
        final String highestScoredLead;

        private ToleranceScope(List<ScoredMetro> eligible, String clause, String highestScoredLead) {
            this.eligible = eligible;
            this.clause = clause;
            this.highestScoredLead = highestScoredLead;
        }

        private static final String SCORE_LEAD = "Placed in highest-scored metro";

        static ToleranceScope of(WorkloadSpec workload, WorkloadProfile profile,
                                 List<ScoredMetro> selected, List<UserSite> sites,
                                 Map<MetroId, Metro> metroMap,
                                 Map<MetroId, Map<MetroId, Double>> latencyMap) {
            Double tolerance = profile.getMaxLatencyToleranceMs();
            if (tolerance == null) {
                return new ToleranceScope(selected, "", SCORE_LEAD);
            }
            if (sites == null || sites.isEmpty()) {
                // Same rule as the request-level bound: a ceiling measured to user sites cannot be
                // evaluated without any, and the run says so instead of looking constrained.
                return new ToleranceScope(selected, "; its " + formatMs(tolerance) + " latency "
                        + "tolerance was NOT applied, because it measures latency from user sites and "
                        + "this request defines none", SCORE_LEAD);
            }

            List<ScoredMetro> within = new ArrayList<>();
            ScoredMetro closest = null;
            double closestWorst = Double.NaN;
            for (ScoredMetro sm : selected) {
                OptionalDouble worst = worstCaseLatencyMs(sm.metro.metroId(), sites, metroMap, latencyMap);
                if (worst.isEmpty()) continue;
                double value = worst.getAsDouble();
                if (value <= tolerance) {
                    within.add(sm);
                }
                if (closest == null || value < closestWorst) {
                    closest = sm;
                    closestWorst = value;
                }
            }
            if (within.size() == selected.size()) {
                return new ToleranceScope(selected, "; within the workload's " + formatMs(tolerance)
                        + " latency tolerance to every user site", SCORE_LEAD);
            }
            if (!within.isEmpty()) {
                return new ToleranceScope(within, "; the workload's " + formatMs(tolerance)
                        + " latency tolerance to every user site ruled out "
                        + (selected.size() - within.size()) + " of the " + selected.size()
                        + " recommended metros",
                        "Placed in the highest-scored metro within the workload's latency tolerance");
            }
            if (closest == null) {
                return new ToleranceScope(selected, "; its " + formatMs(tolerance) + " latency "
                        + "tolerance could not be evaluated: no recommended metro has a measurable "
                        + "latency to any user site", SCORE_LEAD);
            }
            return new ToleranceScope(selected, "; NOTE: no recommended metro is within the workload's "
                    + formatMs(tolerance) + " latency tolerance, so the tolerance was NOT honoured - "
                    + "the closest is " + closest.metro.metroId() + " at "
                    + formatMs(closestWorst) + " to its worst-case site", SCORE_LEAD);
        }
    }

    /**
     * States a workload's recorded facility requirements on its placement, or {@code ""} when it has
     * none.
     *
     * <p>{@code requiresHighPowerDensity} and {@code requiresLiquidCooling} are accepted by the
     * builder and merged into the resolved profile, and their documented contract is that they are
     * "captured on the workload profile and carried through to the result for downstream facility
     * selection" while explicitly <em>not</em> being metro-scoring inputs (Fabric publishes no
     * per-metro power-density or cooling capability to score against). Nothing carried them through,
     * so the promise was unkept and the levers were inert. They now travel on the placement
     * rationale, which is where a reader picking a cabinet looks — and the clause says plainly that
     * they did not influence the ranking, so their presence cannot be misread as scoring.</p>
     */
    private static String facilityRequirementsClause(WorkloadProfile profile) {
        List<String> needs = new ArrayList<>();
        if (profile.isRequiresHighPowerDensity()) needs.add("high power density");
        if (profile.isRequiresLiquidCooling()) needs.add("liquid cooling");
        if (needs.isEmpty()) return "";
        return ". Facility requirements recorded for cabinet/cage selection: " + String.join(" and ", needs)
                + " - Fabric publishes no per-metro power or cooling capability, so this did NOT "
                + "influence the metro ranking; confirm availability with your Equinix account team";
    }

    /** Comma-joined display labels of a workload's provider dependencies. */
    private static String depLabels(List<ProviderRequirement> deps) {
        return deps.stream().map(ProviderRequirement::displayLabel).collect(Collectors.joining(", "));
    }

    // ══════════════════════════════════════════════
    //  Risk Analysis
    // ══════════════════════════════════════════════

    private static RiskAssessment analyzeRisks(List<ScoredMetro> selected,
                                               OptimizationRequest request,
                                               Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap,
                                               LatencyMatrix latencyMatrix,
                                               List<UnresolvedProvider> unresolvedProviders,
                                               List<UnresolvedProvider> unresolvedWorkloadProviders,
                                               CandidateSet candidateSet,
                                               CatalogScan<Metro> metroScan,
                                               CatalogScan<ServiceProfile> profileScan,
                                               CostEstimate costEstimate) {
        List<RiskFinding> findings = new ArrayList<>();
        RiskSeverity worstSeverity = RiskSeverity.INFO;
        double resiliencyScore = 100.0;

        // Nothing was selected. This is the headline fact about the run and it is stated first,
        // whatever the cause: the old code only named a provider-lookup miss, so a blackout from
        // any other constraint fell through to "No significant risks identified" — a HEALTHY
        // verdict on a result with zero metros and zero recommendations.
        if (selected.isEmpty()) {
            findings.add(RiskFinding.builder()
                    .severity(RiskSeverity.CRITICAL)
                    .category("NO_VIABLE_METRO")
                    .description(describeEmptyResult(candidateSet, metroScan))
                    .recommendation("Relax or remove the constraint above and re-run; each constraint "
                            + "can be dropped independently to find which one is binding")
                    .affectedMetro(null)
                    .build());
            resiliencyScore = 0;
            worstSeverity = RiskSeverity.CRITICAL;
        }

        // Single point of failure
        if (selected.size() == 1) {
            findings.add(RiskFinding.builder()
                    .severity(RiskSeverity.HIGH)
                    .category("SINGLE_POINT_OF_FAILURE")
                    .description("All workloads are assigned to a single metro (" + selected.get(0).metro.metroId() + ")")
                    .recommendation("Add at least one additional metro for redundancy")
                    .affectedMetro(selected.get(0).metro.metroId())
                    .build());
            resiliencyScore -= 30;
            worstSeverity = RiskSeverity.HIGH;
        }

        // All metros in same region
        Set<Region> regions = selected.stream()
                .map(sm -> sm.metro.getRegion())
                .collect(Collectors.toSet());
        if (selected.size() > 1 && regions.size() == 1) {
            RedundancyTier requested = request.getConstraints().getMinimumRedundancy();
            if (requested != null && requested.ordinal() >= RedundancyTier.MULTI_REGION.ordinal()) {
                // Region-aware selection would have spread across regions had any other region
                // carried a qualifying metro, so a single-region result here means geographic
                // spread was IMPOSSIBLE with the given constraints, not merely out-scored. Name the
                // hard blocker and what to relax rather than returning a single-region set quietly.
                Region only = regions.iterator().next();
                // Region-aware round-robin takes one metro from every candidate region before it
                // takes a second from any, so a selected set of 2+ metros confined to ONE region can
                // only happen when just one region had a qualifying candidate (a single-region set of
                // size 1 is caught above as SINGLE_POINT_OF_FAILURE, not here). The former "the
                // qualifying metros span N regions but the set collapsed to one — widen max_metros"
                // branch was therefore unreachable and has been removed; only the genuine
                // single-candidate-region case remains, and it states what can actually happen.
                String blocker = "; no other region has a metro that meets the constraints (only " + only
                        + " metros qualified), so multi-region spread is not achievable here";
                findings.add(RiskFinding.builder()
                        .severity(RiskSeverity.HIGH)
                        .category("SINGLE_REGION")
                        .description("All " + selected.size() + " metros are in " + only
                                + " but MULTI_REGION redundancy was requested" + blocker)
                        .recommendation("Relax whichever constraint confines the candidates to one region "
                                + "(required clouds, required/excluded regions, compliance zone, or the "
                                + "max-latency bound), or add a site in another region so a metro there "
                                + "qualifies; multi-region redundancy needs metros in 2+ regions")
                        .affectedMetro(null)
                        .build());
                resiliencyScore -= 25;
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.HIGH);
            } else {
                findings.add(RiskFinding.builder()
                        .severity(RiskSeverity.MEDIUM)
                        .category("SINGLE_REGION")
                        .description("All metros are in the " + regions.iterator().next() + " region")
                        .recommendation("Consider adding a metro in another region for disaster resilience")
                        .affectedMetro(null)
                        .build());
                resiliencyScore -= 10;
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
            }
        }

        // Latency threshold violations. Guarded by the same predicate as the hard candidate
        // filter: with no user sites the bound is not evaluable, nothing was excluded on it,
        // and nothing may be flagged on it either. Candidate filtering
        // already excludes metros beyond an evaluable bound, so in practice only metros
        // force-included via requireMetro(...) can trip this finding. worstCaseMs() is absent
        // for a metro with no measurable latency, so no sentinel can reach the message text.
        // A non-positive bound cannot reach this point — validateRequest rejects it outright — so a
        // non-null bound is always a real one, and the only reason it can be unevaluable here is
        // that the request defines no user sites.
        Double maxLatency = request.getConstraints().getMaxLatencyMs();
        if (maxLatency != null && !isLatencyBounded(maxLatency, request.getSites())) {
            // The caller asked for a hard bound that cannot be evaluated. Silently dropping it is
            // no better than the nonsense message it replaced: the request was not honoured, so
            // say which input is missing rather than let the run look unconstrained.
            findings.add(RiskFinding.builder()
                    .severity(RiskSeverity.MEDIUM)
                    .category("LATENCY_BOUND_NOT_EVALUATED")
                    .description("The requested " + formatMs(maxLatency) + " max-latency "
                            + "bound was NOT applied: it measures metro-to-site latency and this request "
                            + "defines no user sites, so no metro was excluded or flagged on it")
                    .recommendation("Add at least one site (a nearest metro code, or latitude/longitude) "
                            + "for the bound to be enforced")
                    .affectedMetro(null)
                    .build());
            worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
        }
        if (isLatencyBounded(maxLatency, request.getSites())) {
            for (MetroId metro : latencyMatrix.getMetros()) {
                OptionalDouble worstCase = latencyMatrix.worstCaseMs(metro);
                if (worstCase.isEmpty()) continue;
                double worst = worstCase.getAsDouble();
                if (Double.isFinite(worst) && worst > maxLatency) {
                    findings.add(RiskFinding.builder()
                            .severity(RiskSeverity.MEDIUM)
                            .category("LATENCY_THRESHOLD")
                            .description(metro + " has worst-case latency of " + String.format("%.1fms", worst)
                                    + " which exceeds the " + formatMs(maxLatency) + " threshold")
                            .recommendation("Consider adding a metro closer to the affected site")
                            .affectedMetro(metro)
                            .build());
                    resiliencyScore -= 5;
                    worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
                }
            }
        }

        // Per-workload latency ceilings (WorkloadProfile.maxLatencyToleranceMs). The lever is
        // documented as a hard ceiling from the user sites to the workload, so a ceiling no
        // recommended metro can honour — or one that cannot be evaluated at all — is stated, not
        // left to be inferred from a placement that reads exactly like an unconstrained one.
        for (WorkloadSpec workload : request.getWorkloads()) {
            Double tolerance = workload.resolvedProfile().getMaxLatencyToleranceMs();
            if (tolerance == null || selected.isEmpty()) continue;
            if (!latencyMatrix.hasSites()) {
                findings.add(RiskFinding.builder()
                        .severity(RiskSeverity.MEDIUM)
                        .category("WORKLOAD_LATENCY_TOLERANCE_NOT_EVALUATED")
                        .description("Workload '" + workload.getLabel() + "' declares a "
                                + formatMs(tolerance) + " latency tolerance, which was NOT applied: it "
                                + "measures latency from user sites and this request defines none, so the "
                                + "workload was placed without it")
                        .recommendation("Add at least one site (a nearest metro code, or latitude/longitude) "
                                + "for the workload's latency tolerance to be enforced")
                        .affectedMetro(null)
                        .build());
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
                continue;
            }
            boolean honoured = selected.stream().anyMatch(sm -> {
                OptionalDouble worst = latencyMatrix.worstCaseMs(sm.metro.metroId());
                return worst.isPresent() && worst.getAsDouble() <= tolerance;
            });
            if (!honoured) {
                OptionalDouble closest = selected.stream()
                        .map(sm -> latencyMatrix.worstCaseMs(sm.metro.metroId()))
                        .filter(OptionalDouble::isPresent)
                        .mapToDouble(OptionalDouble::getAsDouble)
                        .min();
                findings.add(RiskFinding.builder()
                        .severity(RiskSeverity.HIGH)
                        .category("WORKLOAD_LATENCY_TOLERANCE_UNMET")
                        .description("No recommended metro is within the " + formatMs(tolerance)
                                + " latency tolerance declared by workload '" + workload.getLabel() + "'"
                                + (closest.isPresent()
                                        ? "; the closest is " + formatMs(closest.getAsDouble())
                                            + " to its worst-case site"
                                        : "")
                                + ". It was still placed, so the deployment does not honour that ceiling")
                        .recommendation("Relax the workload's latency tolerance, add a site closer to the "
                                + "recommended metros, or relax the constraints so a nearer metro can be "
                                + "recommended")
                        .affectedMetro(null)
                        .build());
                resiliencyScore -= 10;
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.HIGH);
            }
        }

        // Required metros the catalog does not contain. requireMetro(...) forces a metro past every
        // filter, but a code that is not in the catalog this run read cannot be forced in at all —
        // and dropping it silently makes the result indistinguishable from one that honoured it.
        if (!candidateSet.requiredNotFound.isEmpty()) {
            findings.add(RiskFinding.builder()
                    .severity(RiskSeverity.HIGH)
                    .category("REQUIRED_METRO_NOT_FOUND")
                    .description("Required metro(s) " + candidateSet.requiredNotFound.stream()
                            .map(String::valueOf).collect(Collectors.joining(", "))
                            + " are not in the " + candidateSet.evaluated + " metros this run read from "
                            + "the Fabric metro catalog, so they could not be included in the deployment")
                    .recommendation("Check the metro code against the Fabric metro catalog (codes are "
                            + "two letters, e.g. 'DC'), and confirm the metro is visible to this Equinix "
                            + "account")
                    .affectedMetro(null)
                    .build());
            resiliencyScore -= 10;
            worstSeverity = mostSevere(worstSeverity, RiskSeverity.HIGH);
        }

        // Requirements that resolved to no metro. Two distinct failures hide behind an empty
        // availability entry and they are never conflated here: nothing in the catalog is NAMED that
        // (a lookup miss), or profiles were found but PUBLISHED no metro coverage. Claiming the
        // first when the second happened is a false statement about the catalog — the caller would
        // go hunting for a spelling mistake that does not exist. The text otherwise states only what
        // the run established: how much of the catalog was searched, how the lookup was performed,
        // and how many metros it cost.
        String searched = "searched " + profileScan.size() + " Fabric service profile(s) visible to "
                + "this account" + profileScan.truncationNote();
        for (UnresolvedProvider unresolved : unresolvedProviders) {
            ProviderRequirement req = unresolved.requirement;
            String key = unresolved.label();
            String selector = describeRequirementSelector(req);
            String kind = req.isRequired() ? "required" : "preferred";
            String cause = unresolved.matchedWithoutCoverage()
                    ? unresolved.profilesMatched + " Fabric service profile(s) matched " + kind
                        + " provider '" + key + "' but none published any metro coverage. The optimizer "
                        + searched + ", looking them up by " + selector + ". "
                    : "No Fabric service profile matched " + kind + " provider '" + key + "'. The "
                        + "optimizer " + searched + ", looking them up by " + selector + ", and none of "
                        + "them matched with published metro coverage. ";
            String classification = unresolved.matchedWithoutCoverage()
                    ? "That is a published-coverage gap in the matched profile(s) - the provider is in "
                        + "the searched catalog but no metro is listed against it - rather than a "
                        + "provider-lookup miss"
                    : "That is a provider-lookup miss - nothing in the searched catalog carried that "
                        + "provider - rather than a measured coverage gap";
            String remedy = unresolved.matchedWithoutCoverage()
                    ? "Check the matched service profiles in the Fabric marketplace: a profile with no "
                        + "published metros cannot be connected to anywhere, so pick a profile that "
                        + "lists metros (name it explicitly), or ask your Equinix account team whether "
                        + "its metro coverage is visible to this account"
                    : "Check the provider name against the Fabric marketplace and confirm its "
                        + "service profiles are visible to this Equinix account (visibility is "
                        + "per-account), or name the exact Fabric service profile to match";

            if (req.isRequired()) {
                int cost = candidateSet.eliminatedBy(requiredProviderConstraint(key));
                findings.add(RiskFinding.builder()
                        .severity(RiskSeverity.CRITICAL)
                        .category("PROVIDER_UNAVAILABLE")
                        .description(cause + "Because it resolved to no metro at all, this requirement "
                                + "removed " + cost + " of " + candidateSet.evaluated
                                + " metros from candidacy. " + classification)
                        .recommendation(remedy + ". To see which metros qualify on everything else, "
                                + "re-run with this provider as a preference instead of a requirement")
                        .affectedMetro(null)
                        .build());
                resiliencyScore -= 20;
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.CRITICAL);
            }
            else {
                findings.add(RiskFinding.builder()
                        .severity(RiskSeverity.LOW)
                        .category("PROVIDER_UNAVAILABLE")
                        .description(cause + "Being a preference it excluded nothing, but it scored as "
                                + "unavailable in every metro. " + classification)
                        .recommendation(remedy)
                        .affectedMetro(null)
                        .build());
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.LOW);
            }
        }

        // Workload-level dependency misses. These never touch candidacy, so they cannot show up as
        // an empty result — they just disarm the provider-dependent placement rule, and the
        // workload lands in the highest-scored metro as though it had declared nothing.
        for (UnresolvedProvider unresolved : unresolvedWorkloadProviders) {
            String key = unresolved.label();
            String cause = unresolved.matchedWithoutCoverage()
                    ? unresolved.profilesMatched + " Fabric service profile(s) matched '" + key
                        + "', declared as a workload dependency, but none published any metro coverage."
                    : "No Fabric service profile matched '" + key + "', declared as a workload "
                        + "dependency.";
            findings.add(RiskFinding.builder()
                    .severity(RiskSeverity.MEDIUM)
                    .category("WORKLOAD_PROVIDER_UNAVAILABLE")
                    .description(cause + " The optimizer " + searched + ", looking them up by "
                            + describeRequirementSelector(unresolved.requirement) + ". Workloads depending "
                            + "on it were placed by score alone, without that dependency being satisfied "
                            + "anywhere")
                    .recommendation("Check the provider name against the Fabric marketplace and confirm its "
                            + "service profiles are visible to this Equinix account, or drop the dependency "
                            + "so the placement rationale reflects what was actually optimized for")
                    .affectedMetro(null)
                    .build());
            resiliencyScore -= 10;
            worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
        }

        // Provider concentration
        for (ProviderRequirement req : request.getProviders()) {
            if (req.isRequired()) {
                String key = req.displayLabel();
                Map<MetroId, ProviderAvailability> avail = providerMetroMap.get(key);
                long availableCount = selected.stream()
                        .filter(sm -> avail != null && avail.containsKey(sm.metro.metroId()))
                        .count();
                if (availableCount == 1) {
                    findings.add(RiskFinding.builder()
                            .severity(RiskSeverity.MEDIUM)
                            .category("PROVIDER_CONCENTRATION")
                            .description(key + " is only available in 1 of " + selected.size() + " recommended metros")
                            .recommendation("Consider selecting metros where " + key + " has broader presence")
                            .affectedMetro(null)
                            .build());
                    resiliencyScore -= 5;
                    worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
                }
            }
        }

        // Required-cloud coverage across the SELECTED SET. Request-level required clouds no longer
        // force every metro to carry all of them; they are a coverage guarantee — each must be
        // reachable somewhere in the recommendation. A cloud that resolves to no metro at all is
        // already the CRITICAL PROVIDER_UNAVAILABLE finding above, so this fires only for a cloud that
        // IS available in the account but that the selected set happens not to include a metro for.
        for (ProviderRequirement req : request.getProviders()) {
            if (!req.isRequired()) continue;
            String key = req.displayLabel();
            Map<MetroId, ProviderAvailability> avail = providerMetroMap.get(key);
            boolean resolvedSomewhere = avail != null && !avail.isEmpty();
            if (!resolvedSomewhere) continue; // reported as PROVIDER_UNAVAILABLE, not a coverage gap
            boolean coveredInSet = !selected.isEmpty() && selected.stream()
                    .anyMatch(sm -> avail.containsKey(sm.metro.metroId()));
            if (!coveredInSet) {
                findings.add(RiskFinding.builder()
                        .severity(RiskSeverity.HIGH)
                        .category("REQUIRED_CLOUD_NOT_COVERED")
                        .description("Required cloud '" + key + "' is available in the account but no "
                                + "recommended metro carries it, so the deployment does not reach it. "
                                + "Required clouds are a coverage guarantee across the set, not a "
                                + "per-metro filter, so a set can be recommended that leaves one uncovered")
                        .recommendation("Add a metro that carries '" + key + "' (raise max_metros, or "
                                + "require a metro that has it), or drop it from require_clouds if it need "
                                + "not be reachable everywhere in this deployment")
                        .affectedMetro(null)
                        .build());
                resiliencyScore -= 15;
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.HIGH);
            }
        }

        // Budget: reported against, never enforced. When a ceiling is set and the estimated monthly
        // total exceeds it, surface it as a finding (the within_budget flag is already false) so an
        // over-budget deployment is stated rather than presented as acceptable. A null budget — the
        // default — is a no-cap: nothing is checked and nothing is flagged.
        BudgetRange budget = request.getConstraints().getBudget();
        if (budget != null && budget.getMaxMonthly() != null && costEstimate != null
                && !costEstimate.isWithinBudget()) {
            findings.add(RiskFinding.builder()
                    .severity(RiskSeverity.MEDIUM)
                    .category("BUDGET_EXCEEDED")
                    .description("Estimated monthly cost " + formatMoney(costEstimate.getMonthlyTotal(),
                            costEstimate.getCurrency()) + " exceeds the "
                            + formatMoney(budget.getMaxMonthly(), budget.getCurrency()) + " monthly budget "
                            + "ceiling. The budget is a reporting check, not a filter: no metro was "
                            + "excluded or scored on it")
                    .recommendation("Raise the budget ceiling, reduce bandwidth or the metro count, or "
                            + "choose a lower-cost region or a longer term; the ceiling only sets the "
                            + "within_budget flag and this finding")
                    .affectedMetro(null)
                    .build());
            worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
        }

        // Redundancy gap
        RedundancyTier requested = request.getConstraints().getMinimumRedundancy();
        if (requested != null && selected.size() < requested.getMinimumMetros()) {
            findings.add(RiskFinding.builder()
                    .severity(RiskSeverity.CRITICAL)
                    .category("REDUNDANCY_GAP")
                    .description("Requested " + requested + " redundancy requires at least " + requested.getMinimumMetros()
                            + " metros but only " + selected.size() + " were selected")
                    .recommendation("Relax constraints to allow more eligible metros")
                    .affectedMetro(null)
                    .build());
            resiliencyScore -= 30;
            worstSeverity = RiskSeverity.CRITICAL;
        }

        // A HEALTHY verdict is only ever reachable when something was actually recommended: the
        // empty-result finding above guarantees a non-empty findings list otherwise.
        if (findings.isEmpty()) {
            findings.add(RiskFinding.builder()
                    .severity(RiskSeverity.INFO)
                    .category("HEALTHY")
                    .description("No significant risks identified in the recommended topology")
                    .recommendation(null)
                    .affectedMetro(null)
                    .build());
        }

        return new RiskAssessment(findings, worstSeverity, Math.max(0, resiliencyScore));
    }

    /**
     * Describes an empty recommendation set, naming — best effort — the constraint that eliminated
     * the most metros. The tally comes from the candidate filter itself, so the attribution is the
     * real count of metros each constraint rejected rather than a guess made after the fact.
     */
    private static String describeEmptyResult(CandidateSet candidateSet, CatalogScan<Metro> metroScan) {
        StringBuilder sb = new StringBuilder("No metro was recommended: ");
        if (candidateSet.evaluated == 0) {
            // The scan note matters most here: "the catalog returned no metros" and "the scan of the
            // catalog failed before it read any" are different facts about the account.
            return sb.append("the Fabric metro catalog returned no metros at all, so there was "
                    + "nothing to evaluate").append(metroScan.truncationNote()).toString();
        }
        if (candidateSet.candidates.isEmpty()) {
            sb.append("all ").append(candidateSet.evaluated)
                    .append(" metros were eliminated during candidate filtering");
            Optional<Map.Entry<String, Integer>> dominant = candidateSet.dominantConstraint();
            if (dominant.isPresent()) {
                sb.append(". The constraint that eliminated the most was ")
                        .append(dominant.get().getKey()).append(" (")
                        .append(dominant.get().getValue()).append(" metros)");
                if (candidateSet.eliminations.size() > 1) {
                    sb.append("; full tally: ").append(candidateSet.eliminations.entrySet().stream()
                            .map(e -> e.getKey() + " = " + e.getValue())
                            .collect(Collectors.joining(", ")));
                }
            }
            return sb.toString();
        }
        // Candidates survived filtering but none were selected. resolveMaxMetros never returns a
        // non-positive cap, so this is defensive; it still names the selection step rather than
        // blaming a filter that did not fire.
        return sb.append(candidateSet.candidates.size()).append(" of ").append(candidateSet.evaluated)
                .append(" metros passed candidate filtering, but the resolved metro count limit "
                        + "selected none of them").toString();
    }

    private static RiskSeverity mostSevere(RiskSeverity a, RiskSeverity b) {
        return a.ordinal() <= b.ordinal() ? a : b; // CRITICAL=0 is "higher" severity
    }

    // ══════════════════════════════════════════════
    //  Cost Estimation
    // ══════════════════════════════════════════════

    /**
     * Produces per-metro and aggregate cost estimates. Each metro is priced via the
     * resolved {@link RateCard} (live Equinix pricing by default) for a representative
     * Fabric connection at the metro's allocated bandwidth, falling back to a regional
     * heuristic (tagged {@link PriceSource#ESTIMATE}) when the rate card cannot price it.
     */
    private static CostEstimate estimateCosts(List<ScoredMetro> selected, OptimizationRequest request,
                                              RateCard rateCard) {
        List<MetroCostBreakdown> perMetro = new ArrayList<>();

        int totalBandwidth = request.getWorkloads().stream()
                .mapToInt(MetroOptimizerEngine::effectiveBandwidthMbps)
                .sum();
        // Split the total bandwidth across the selected metros. Plain integer division silently
        // dropped up to (n-1) Mbps to truncation (e.g. 100 Mbps over 3 metros priced 33+33+33=99);
        // the remainder is distributed across the first metros so the per-metro sizing sums to the
        // total instead of losing bandwidth that a caller declared.
        int metroCount = Math.max(1, selected.size());
        int baseBandwidth = totalBandwidth / metroCount;
        int bandwidthRemainder = totalBandwidth % metroCount;

        Term term = request.getTerm() != null ? request.getTerm() : Term.MONTH_12;
        boolean anyLive = false;
        PriceSource aggregateSource = null;
        boolean mixedSources = false;
        // Per-metro figures are summed into one aggregate, so they must reconcile to one currency.
        // Live Fabric pricing genuinely quotes different currencies per region (EUR for Frankfurt,
        // USD for Ashburn, ...), so a multi-region set can legitimately span currencies — in which
        // case a single total would be a fabricated cross-currency sum.
        CurrencyReconciler recon = CurrencyReconciler.create();

        for (int i = 0; i < selected.size(); i++) {
            ScoredMetro sm = selected.get(i);
            int metroBandwidth = baseBandwidth + (i < bandwidthRemainder ? 1 : 0);

            BigDecimal monthly;
            BigDecimal setup;
            PriceSource metroSource;
            String metroCurrency;
            Map<String, BigDecimal> lineItems = new LinkedHashMap<>();

            Optional<PriceQuote> live = rateCard != null
                    ? rateCard.connection(ConnectionType.EVPL_VC, metroBandwidth, sm.metro.getCode(), term)
                    : Optional.empty();

            if (live.isPresent()) {
                PriceQuote quote = live.get();
                monthly = quote.getMonthlyRecurring();
                setup = quote.getNonRecurring();
                metroCurrency = quote.getCurrency() != null ? quote.getCurrency().getCurrencyCode() : null;
                metroSource = quote.getSource();
                anyLive = true;
                lineItems.put("Fabric connection (EVPL_VC, " + metroBandwidth + " Mbps)", monthly);
            } else {
                // Heuristic fallback: base port + per-Mbps connection cost with a regional multiplier.
                // These figures are USD by construction.
                BigDecimal basePortCost = BigDecimal.valueOf(500);
                BigDecimal perMbpsCost = BigDecimal.valueOf(0.50);
                BigDecimal setupCost = BigDecimal.valueOf(1000);

                double regionMultiplier = 1.0;
                if (sm.metro.getRegion() == Region.EMEA) regionMultiplier = 1.2;
                else if (sm.metro.getRegion() == Region.APAC) regionMultiplier = 1.4;

                BigDecimal bandwidthAllocation = perMbpsCost.multiply(BigDecimal.valueOf(metroBandwidth));
                monthly = basePortCost.add(bandwidthAllocation).multiply(BigDecimal.valueOf(regionMultiplier));
                setup = setupCost.multiply(BigDecimal.valueOf(regionMultiplier));
                metroSource = PriceSource.ESTIMATE;
                metroCurrency = "USD";

                lineItems.put("Base port", basePortCost);
                lineItems.put("Bandwidth allocation", bandwidthAllocation);
                lineItems.put("Regional adjustment", monthly.subtract(basePortCost));
            }

            if (aggregateSource == null) {
                aggregateSource = metroSource;
            } else if (aggregateSource != metroSource) {
                mixedSources = true;
            }

            recon.add(metroCurrency, monthly, setup);
            perMetro.add(new MetroCostBreakdown(sm.metro.metroId(), monthly, setup, lineItems, metroSource));
        }

        String baseDisclaimer = anyLive
                ? "Per-metro costs use live Equinix Fabric pricing where available, otherwise a regional "
                    + "estimate. Actual costs vary by connection type, bandwidth tier, and contract terms. "
                    + "Contact your Equinix account team for precise quotes."
                : "Estimates based on a regional pricing heuristic (live Fabric pricing was unavailable). "
                    + "Actual costs vary by connection type, bandwidth tier, and contract terms. "
                    + "Contact your Equinix account team for precise quotes.";

        PriceSource source = mixedSources ? PriceSource.COMPOSITE
                : (aggregateSource != null ? aggregateSource : PriceSource.ESTIMATE);

        BudgetRange budget = request.getConstraints().getBudget();

        if (recon.isMixed()) {
            // Metros span currencies: surface the per-currency subtotals and omit a single aggregate
            // rather than report a false cross-currency total. Budget cannot be evaluated against a
            // total that does not exist, so withinBudget stays true (no false over-budget alarm) and
            // the disclaimer says so.
            String mixDisclaimer = baseDisclaimer + " The selected metros are priced in multiple currencies ("
                    + recon.describeCurrencies() + "): " + recon.describeMonthlySubtotals() + " per month. A single "
                    + "aggregate total is not shown because summing across currencies without an FX rate would be a "
                    + "fabricated figure; the per-metro figures above are each in their own currency."
                    + (budget != null && budget.getMaxMonthly() != null
                        ? " The monthly budget could not be evaluated against a mixed-currency estimate." : "");
            return CostEstimate.builder()
                    .monthlyTotal(null)
                    .setupTotal(null)
                    .currency(null)
                    .monthlyByCurrency(recon.monthlySubtotals())
                    .perMetro(perMetro)
                    .withinBudget(true)
                    .costDisclaimer(mixDisclaimer)
                    .source(source)
                    .build();
        }

        BigDecimal totalMonthly = recon.monthlyTotal().orElse(BigDecimal.ZERO);
        BigDecimal totalSetup = recon.setupTotal().orElse(BigDecimal.ZERO);
        String currency = recon.soleCurrencyOr("USD");

        boolean withinBudget = true;
        if (budget != null && budget.getMaxMonthly() != null) {
            withinBudget = totalMonthly.compareTo(budget.getMaxMonthly()) <= 0;
        }

        return CostEstimate.builder()
                .monthlyTotal(totalMonthly)
                .setupTotal(totalSetup)
                .currency(currency)
                .monthlyByCurrency(recon.monthlySubtotals())
                .perMetro(perMetro)
                .withinBudget(withinBudget)
                .costDisclaimer(baseDisclaimer)
                .source(source)
                .build();
    }

    // ══════════════════════════════════════════════
    //  Explanation
    // ══════════════════════════════════════════════

    private static OptimizationExplanation buildExplanation(OptimizationRequest request,
                                                             CandidateSet candidateSet,
                                                             int selectedMetros, int resolvedMaxMetros,
                                                             List<UnresolvedProvider> unresolvedProviders,
                                                             List<UnresolvedProvider> unresolvedWorkloadProviders,
                                                             CatalogScan<Metro> metroScan,
                                                             CatalogScan<ServiceProfile> profileScan) {

        int totalMetros = candidateSet.evaluated;
        int candidateMetros = candidateSet.candidates.size();
        int passedFilters = candidateSet.passedFilters();

        // A non-positive bound never reaches here (validateRequest rejects it), so "requested"
        // means exactly that: a real bound the caller asked for.
        Double maxLatencyMs = request.getConstraints().getMaxLatencyMs();
        boolean latencyBoundRequested = maxLatencyMs != null;
        boolean latencyBoundApplied = isLatencyBounded(maxLatencyMs, request.getSites());

        // The methodology only claims a bound was applied when one actually was — the filter and
        // the risk pass both no-op when there are no sites to measure to.
        String latencyClause;
        if (latencyBoundApplied) {
            latencyClause = ", and the " + formatMs(maxLatencyMs) + " max-latency bound";
        }
        else if (latencyBoundRequested) {
            latencyClause = "; the requested " + formatMs(maxLatencyMs) + " max-latency bound "
                    + "was NOT applied, because it measures metro-to-site latency and this request "
                    + "defines no user sites";
        }
        else {
            latencyClause = "";
        }

        // The described selection method must match the one #selectMetros actually runs (keyed on the
        // same #usesRegionDiversitySelection predicate): greedy top-N for NONE/N_PLUS_1, region
        // round-robin for the geographic-diversity tiers. Calling a spread run "top N by score" would
        // misreport a set that deliberately dropped higher-scored metros to reach other regions.
        String selectionClause;
        if (usesRegionDiversitySelection(request)) {
            RedundancyTier tier = request.getConstraints().getMinimumRedundancy();
            selectionClause = "The set is then spread across regions rather than taken as the top "
                    + "scores: candidates are grouped by region and picked round-robin best-per-region "
                    + "(regions where the caller has sites first) up to the metro cap, so " + tier
                    + " geographic diversity is a guaranteed outcome — this can override raw score and "
                    + "drop a higher-scored metro whose region is already represented.";
        }
        else {
            selectionClause = "The highest-scoring metros are then selected, up to the metro cap.";
        }

        String methodology = "The optimizer read " + metroScan.size() + " Equinix metros and "
                + profileScan.size() + " Fabric service profiles" + describeScanCoverage(metroScan, profileScan)
                + ", then filtered to " + passedFilters + " candidates based on constraints "
                + "(excluded metros/regions, compliance zones, required provider availability"
                + latencyClause + ")"
                + describeForceIncluded(candidateSet) + ". "
                + "Each candidate is scored across 5 dimensions: "
                + "latency (weighted by workforce distribution and site importance), provider coverage "
                + "(availability of required and preferred providers), cost (regional pricing estimates), "
                + "redundancy (geographic diversity of the selected set), and compliance (data sovereignty). "
                + "Scores are combined using the " + request.getStrategy() + " strategy weights"
                + describeWeightSource(request.getScoringWeights())
                + ". " + selectionClause
                + " Workloads are placed using a greedy algorithm: latency-critical workloads go to the "
                + "lowest-latency metro, DR workloads to a different region, and provider-dependent workloads "
                + "to metros where all dependencies are available.";

        List<String> assumptions = new ArrayList<>(Arrays.asList(
                "Latency between metros uses Equinix Fabric avgLatency data where available; "
                        + "otherwise estimated via Haversine distance × 1.4 fiber multiplier × 4.9μs/km",
                "Cost estimates use a simplified model; actual pricing depends on contract terms",
                "Provider availability is based on the " + profileScan.size() + " Fabric marketplace "
                        + "service profiles visible to this account at optimization time"
                        + profileScan.truncationNote(),
                "Sites with neither a headcount nor an explicit weight are weighted as an average site "
                        + "for their role, per site - never as weightless",
                "Workload placement uses greedy assignment, not global optimization"));
        String bandwidthFloors = describeBandwidthFloors(request);
        if (bandwidthFloors != null) {
            assumptions.add(bandwidthFloors);
        }
        String facilityRequirements = describeFacilityRequirements(request);
        if (facilityRequirements != null) {
            assumptions.add(facilityRequirements);
        }
        assumptions = Collections.unmodifiableList(assumptions);

        // The metro count is reported as what was actually selected and (when it bound) the
        // resolved cap. constraints.maxMetroCount is nullable and defaults through
        // resolveMaxMetros, so printing it raw rendered "selected top null" on the default request.
        StringBuilder humanReadable = new StringBuilder()
                .append("Analyzed ").append(totalMetros).append(" metros, ")
                .append(passedFilters).append(" met constraints");
        // Force-included metros met no constraint — they bypassed every filter — so they are
        // counted separately. Folding them into "N met constraints" credited them with passing
        // checks they never ran, while a PROVIDER_UNAVAILABLE finding in the same payload said the
        // opposite.
        if (!candidateSet.forceIncluded.isEmpty()) {
            humanReadable.append(" (plus ").append(candidateSet.forceIncluded.size())
                    .append(" force-included by the required-metro constraint, which bypassed every "
                            + "filter and met none of them: ")
                    .append(candidateSet.forceIncluded.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")))
                    .append(")");
        }
        humanReadable.append(", ");
        if (selectedMetros == 0) {
            humanReadable.append("none selected by ").append(request.getStrategy()).append(" strategy");
        }
        else if (usesRegionDiversitySelection(request)) {
            // A spread run deliberately drops higher-scored metros to reach other regions, so it is
            // NOT a plain top-N-by-score ranking and must not describe itself as one.
            RedundancyTier tier = request.getConstraints().getMinimumRedundancy();
            humanReadable.append("selected ").append(selectedMetros)
                    .append(" metro(s) spread across regions (round-robin best-per-region, prioritising "
                            + "regions where you have sites) for ").append(tier).append(" redundancy");
            if (candidateMetros > resolvedMaxMetros) {
                humanReadable.append(" (capped at ").append(resolvedMaxMetros).append(")");
            }
            humanReadable.append(" — a geographic spread that can override raw score and drop "
                            + "higher-scored metros, not a plain top-").append(selectedMetros)
                    .append("-by-").append(request.getStrategy()).append("-score ranking");
        }
        else {
            humanReadable.append("selected the top ").append(selectedMetros);
            if (candidateMetros > resolvedMaxMetros) {
                humanReadable.append(" (capped at ").append(resolvedMaxMetros).append(")");
            }
            humanReadable.append(" by ").append(request.getStrategy()).append(" strategy");
        }

        if (latencyBoundRequested && !latencyBoundApplied) {
            humanReadable.append(". The requested ").append(formatMs(maxLatencyMs))
                    .append(" max-latency bound could not be evaluated because the request defines no "
                            + "user sites, so it excluded nothing");
        }
        if (!candidateSet.requiredNotFound.isEmpty()) {
            humanReadable.append(". Required metro(s) ").append(candidateSet.requiredNotFound.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")))
                    .append(" are not in the metro catalog this run read, so they could not be included");
        }
        // Attributed per catalog: with two catalogs an unattributed warning leaves the reader
        // unable to tell which one was cut short, and two incomplete scans emitted two
        // indistinguishable parentheticals.
        if (!metroScan.isComplete() || !profileScan.isComplete()) {
            humanReadable.append(". WARNING: ")
                    .append(Stream.of(metroScan, profileScan)
                            .filter(scan -> !scan.isComplete())
                            .map(CatalogScan::coverageClause)
                            .collect(Collectors.joining("; ")))
                    .append(" - coverage conclusions may be incomplete");
        }

        // Name a provider lookup miss instead of letting it read as "no metro has this provider",
        // and keep it distinct from profiles that matched but published no metro coverage: the two
        // are indistinguishable from the candidate count alone and call for different fixes.
        appendUnresolved(humanReadable, unresolvedProviders,
                u -> u.requirement.isRequired() && !u.matchedWithoutCoverage(),
                "No Fabric service profile matched required provider(s): ",
                " - of the " + profileScan.size() + " profiles searched, none published metro coverage "
                        + "for them, so metros were ruled out on that lookup miss rather than on "
                        + "measured coverage");
        appendUnresolved(humanReadable, unresolvedProviders,
                u -> u.requirement.isRequired() && u.matchedWithoutCoverage(),
                "Fabric service profiles matched required provider(s) but published no metro coverage: ",
                " - metros were ruled out on that missing published coverage, not on a naming miss");
        appendUnresolved(humanReadable, unresolvedProviders,
                u -> !u.requirement.isRequired() && !u.matchedWithoutCoverage(),
                "No Fabric service profile matched preferred provider(s): ",
                " - they scored as unavailable everywhere");
        appendUnresolved(humanReadable, unresolvedProviders,
                u -> !u.requirement.isRequired() && u.matchedWithoutCoverage(),
                "Fabric service profiles matched preferred provider(s) but published no metro coverage: ",
                " - they scored as unavailable everywhere");
        appendUnresolved(humanReadable, unresolvedWorkloadProviders,
                u -> !u.matchedWithoutCoverage(),
                "No Fabric service profile matched workload dependency provider(s): ",
                " - workloads depending on them were placed by score alone");
        appendUnresolved(humanReadable, unresolvedWorkloadProviders,
                UnresolvedProvider::matchedWithoutCoverage,
                "Fabric service profiles matched workload dependency provider(s) but published no metro "
                        + "coverage: ",
                " - workloads depending on them were placed by score alone");

        return OptimizationExplanation.builder()
                .methodology(methodology)
                .assumptions(assumptions)
                .dataFreshness("Data fetched at optimization time from live Fabric APIs")
                .humanReadable(humanReadable.toString())
                .build();
    }

    /** Appends one "these providers did not resolve" sentence, or nothing when the group is empty. */
    private static void appendUnresolved(StringBuilder text, List<UnresolvedProvider> unresolved,
                                         java.util.function.Predicate<UnresolvedProvider> group,
                                         String lead, String tail) {
        String labels = unresolved.stream()
                .filter(group)
                .map(UnresolvedProvider::label)
                .collect(Collectors.joining(", "));
        if (labels.isEmpty()) return;
        text.append(". ").append(lead).append(labels).append(tail);
    }

    /**
     * States how completely the two catalogs were read.
     *
     * <p>The claim "across every page of each catalog" used to be unconditional, with a note tacked
     * on afterwards saying the scan had in fact stopped early — a sentence that contradicted itself,
     * and with two catalogs in play the note carried no attribution, so a reader could not tell which
     * catalog was cut short. The claim is now made only when it is true, and any shortfall is
     * attributed to the catalog it happened in.</p>
     */
    private static String describeScanCoverage(CatalogScan<Metro> metroScan,
                                               CatalogScan<ServiceProfile> profileScan) {
        if (metroScan.isComplete() && profileScan.isComplete()) {
            return " across every page of each catalog";
        }
        return " (" + Stream.of(metroScan, profileScan)
                .map(CatalogScan::coverageClause)
                .collect(Collectors.joining("; ")) + ")";
    }

    /** Names the metros that were forced past the filters, so the candidate count is not read as theirs. */
    private static String describeForceIncluded(CandidateSet candidateSet) {
        if (candidateSet.forceIncluded.isEmpty()) return "";
        return ", plus " + candidateSet.forceIncluded.size() + " metro(s) force-included by the "
                + "required-metro constraint (" + candidateSet.forceIncluded.stream()
                        .map(String::valueOf).collect(Collectors.joining(", "))
                + "), which bypass every filter above";
    }

    /**
     * Says whether the scoring weights were the strategy's defaults or carried user overrides, by
     * comparing them against {@link ScoringWeights#defaults()}.
     *
     * <p>The old test was {@code getScoringWeights() != null}, and the builder substitutes
     * {@code ScoringWeights.defaults()} whenever the caller supplies none — so the field is never
     * null and every single run claimed "user-customized overrides", including the default MCP call
     * shape that customizes nothing.</p>
     */
    private static String describeWeightSource(ScoringWeights weights) {
        ScoringWeights defaults = ScoringWeights.defaults();
        if (weights == null || weights.equals(defaults)) {
            return " with no user overrides (the strategy's default weights and latency thresholds)";
        }
        List<String> overrides = new ArrayList<>();
        addOverride(overrides, "latency weight", weights.getLatencyWeight(), defaults.getLatencyWeight());
        addOverride(overrides, "provider-coverage weight",
                weights.getProviderCoverageWeight(), defaults.getProviderCoverageWeight());
        addOverride(overrides, "cost weight", weights.getCostWeight(), defaults.getCostWeight());
        addOverride(overrides, "redundancy weight",
                weights.getRedundancyWeight(), defaults.getRedundancyWeight());
        addOverride(overrides, "compliance weight",
                weights.getComplianceWeight(), defaults.getComplianceWeight());
        addOverride(overrides, "excellent-latency threshold",
                weights.getLatencyExcellentMs(), defaults.getLatencyExcellentMs());
        addOverride(overrides, "good-latency threshold",
                weights.getLatencyGoodMs(), defaults.getLatencyGoodMs());
        addOverride(overrides, "acceptable-latency threshold",
                weights.getLatencyAcceptableMs(), defaults.getLatencyAcceptableMs());
        addOverride(overrides, "poor-latency threshold",
                weights.getLatencyPoorMs(), defaults.getLatencyPoorMs());
        addOverride(overrides, "required-provider weight",
                weights.getRequiredProviderWeight(), defaults.getRequiredProviderWeight());
        addOverride(overrides, "cost tolerance",
                weights.getCostTolerancePercent(), defaults.getCostTolerancePercent());
        return overrides.isEmpty()
                ? " with user-customized overrides"
                : " with user-customized overrides (" + String.join(", ", overrides) + ")";
    }

    private static void addOverride(List<String> overrides, String name, Double value, Double defaultValue) {
        if (!java.util.Objects.equals(value, defaultValue)) {
            overrides.add(name);
        }
    }

    /**
     * An assumptions line naming every workload carrying recorded facility requirements, or
     * {@code null} when none do. These are accepted by the builder and are deliberately not scoring
     * inputs — Fabric publishes no per-metro power-density or cooling capability — so they are stated
     * here (and on the placement rationale) rather than being silently discarded.
     */
    private static String describeFacilityRequirements(OptimizationRequest request) {
        List<String> described = new ArrayList<>();
        for (WorkloadSpec workload : request.getWorkloads()) {
            WorkloadProfile profile = workload.resolvedProfile();
            List<String> needs = new ArrayList<>();
            if (profile.isRequiresHighPowerDensity()) needs.add("high power density");
            if (profile.isRequiresLiquidCooling()) needs.add("liquid cooling");
            if (!needs.isEmpty()) {
                described.add("'" + workload.getLabel() + "' (" + String.join(" and ", needs) + ")");
            }
        }
        return described.isEmpty() ? null
                : "Facility requirements are recorded for cabinet/cage selection and did NOT influence "
                    + "metro ranking, because Fabric publishes no per-metro power or cooling capability: "
                    + String.join(", ", described);
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    /**
     * The bandwidth a workload is actually sized at: its declared {@code bandwidthMbps}, raised to
     * the resolved profile's {@code minBandwidthMbps} when the declaration falls below that floor.
     *
     * <p>{@code WorkloadProfile.minBandwidthMbps} is a documented part of the profile and was read
     * by nothing, so a workload could declare 10 Mbps against a profile whose floor is 1000 and be
     * priced at 10 — a silently under-sized deployment. A minimum that is not enforced anywhere is
     * not a minimum, so it now sets the sizing floor for cost estimation, and
     * {@link #describeBandwidthFloors} states in the assumptions wherever it actually bound.</p>
     */
    private static int effectiveBandwidthMbps(WorkloadSpec workload) {
        int declared = workload.getBandwidthMbps();
        Double floor = workload.resolvedProfile().getMinBandwidthMbps();
        if (floor == null || !Double.isFinite(floor) || floor <= 0) return declared;
        return (int) Math.max(declared, Math.ceil(floor));
    }

    /**
     * An assumptions line naming every workload whose declared bandwidth was raised to its profile's
     * floor, or {@code null} when no floor bound anything.
     */
    private static String describeBandwidthFloors(OptimizationRequest request) {
        List<String> raised = new ArrayList<>();
        for (WorkloadSpec workload : request.getWorkloads()) {
            int effective = effectiveBandwidthMbps(workload);
            if (effective > workload.getBandwidthMbps()) {
                raised.add("'" + workload.getLabel() + "' " + workload.getBandwidthMbps()
                        + " -> " + effective + " Mbps");
            }
        }
        return raised.isEmpty() ? null
                : "Costs are sized at each workload's profile minimum where its declared bandwidth was "
                    + "below it (" + String.join(", ", raised) + ")";
    }

    private static int resolveMaxMetros(OptimizationRequest request) {
        Integer max = request.getConstraints().getMaxMetroCount();
        if (max != null && max > 0) return max;

        RedundancyTier redundancy = request.getConstraints().getMinimumRedundancy();
        if (redundancy != null) return Math.max(3, redundancy.getMinimumMetros() + 1);

        return 3; // default
    }

    // ══════════════════════════════════════════════
    //  Selection
    // ══════════════════════════════════════════════

    /**
     * Chooses up to {@code maxMetros} from the score-descending candidate list.
     *
     * <p>For redundancy {@code NONE} and {@code N_PLUS_1} this is the plain greedy top-N: cost,
     * latency and balanced runs are unchanged. For {@code MULTI_METRO} and {@code MULTI_REGION} —
     * the tiers whose whole point is geographic diversity — selection is region-aware instead, so
     * multi-region can never return an all-one-region set and only warn about it.</p>
     */
    private static List<ScoredMetro> selectMetros(List<ScoredMetro> scoredDesc, int maxMetros,
                                                  OptimizationRequest request,
                                                  Map<MetroId, Metro> metroMap) {
        if (!usesRegionDiversitySelection(request)) {
            return scoredDesc.stream().limit(maxMetros).collect(Collectors.toList());
        }
        return selectWithRegionDiversity(scoredDesc, maxMetros, request, metroMap);
    }

    /**
     * Whether selection spreads across regions (round-robin best-per-region) instead of taking the
     * plain greedy top-N. True exactly for the redundancy tiers whose purpose is geographic diversity
     * ({@link RedundancyTier#MULTI_REGION} and {@link RedundancyTier#MULTI_METRO}). The explanation
     * strings key their selection wording off this same predicate, so the method they describe and the
     * method {@link #selectMetros} actually runs can never drift apart.
     */
    private static boolean usesRegionDiversitySelection(OptimizationRequest request) {
        RedundancyTier tier = request.getConstraints().getMinimumRedundancy();
        return tier == RedundancyTier.MULTI_REGION || tier == RedundancyTier.MULTI_METRO;
    }

    /**
     * Region-diversity-aware selection: group the (score-descending) candidates by
     * {@link Metro#getRegion()} and pick round-robin best-per-region until {@code maxMetros} is
     * reached — best of the highest-priority region, best of the next, …, then the second-best of the
     * highest-priority region, and so on. Because the first pass takes one metro from every region in
     * turn, the selected set always spans {@code min(regionsAvailable, maxMetros)} distinct regions:
     * no single region can monopolise it, which is exactly the multi-region guarantee. When only one
     * region has candidates this degrades to top-N within that region, and the single-region outcome
     * is surfaced as a risk finding rather than hidden.
     *
     * <p>Region priority follows demand: regions the user has sites in rank first (weighted by those
     * sites' presence/weight), so the spread lands near the workforce — London pulls in EMEA,
     * Singapore pulls in APAC — with the best-in-region score and then the region name as
     * deterministic tie-breaks.</p>
     */
    private static List<ScoredMetro> selectWithRegionDiversity(List<ScoredMetro> scoredDesc, int maxMetros,
                                                               OptimizationRequest request,
                                                               Map<MetroId, Metro> metroMap) {
        // Preserve score-descending order within each region group.
        LinkedHashMap<Region, List<ScoredMetro>> byRegion = new LinkedHashMap<>();
        for (ScoredMetro sm : scoredDesc) {
            byRegion.computeIfAbsent(sm.metro.getRegion(), k -> new ArrayList<>()).add(sm);
        }

        Map<Region, Double> demand = regionSiteDemand(request, metroMap);
        List<Region> order = new ArrayList<>(byRegion.keySet());
        order.sort((a, b) -> {
            int byDemand = Double.compare(demand.getOrDefault(b, 0.0), demand.getOrDefault(a, 0.0));
            if (byDemand != 0) return byDemand;
            int byBest = Double.compare(byRegion.get(b).get(0).score.getComposite(),
                    byRegion.get(a).get(0).score.getComposite());
            if (byBest != 0) return byBest;
            return regionName(a).compareTo(regionName(b));
        });

        Map<Region, Integer> cursor = new HashMap<>();
        List<ScoredMetro> selected = new ArrayList<>();
        boolean progressed = true;
        while (selected.size() < maxMetros && progressed) {
            progressed = false;
            for (Region region : order) {
                if (selected.size() >= maxMetros) break;
                int idx = cursor.getOrDefault(region, 0);
                List<ScoredMetro> group = byRegion.get(region);
                if (idx < group.size()) {
                    selected.add(group.get(idx));
                    cursor.put(region, idx + 1);
                    progressed = true;
                }
            }
        }
        return selected;
    }

    /**
     * Per-region demand from the user's sites, used only to order regions for round-robin selection.
     * A site contributes to the region of its {@code nearestMetro} (sites given only by coordinates,
     * or by an unknown metro, cannot be attributed to a region and are skipped). The contribution is
     * the site's explicit weight, else its headcount, else 1.0 for bare presence, so the region a
     * user actually staffs is preferred without needing a fully-weighted request.
     */
    private static Map<Region, Double> regionSiteDemand(OptimizationRequest request,
                                                        Map<MetroId, Metro> metroMap) {
        Map<Region, Double> demand = new HashMap<>();
        for (UserSite site : request.getSites()) {
            MetroId near = site.getNearestMetro();
            if (near == null) continue;
            Metro metro = metroMap.get(near);
            if (metro == null || metro.getRegion() == null) continue;
            double contribution = site.getWeight() > 0 ? site.getWeight()
                    : (site.getHeadcount() > 0 ? site.getHeadcount() : 1.0);
            demand.merge(metro.getRegion(), contribution, Double::sum);
        }
        return demand;
    }

    /** A stable name for a region, treating a null region as an empty string for ordering. */
    private static String regionName(Region region) {
        return region != null ? region.name() : "";
    }

    private static List<String> generateReasons(ScoredMetro sm, OptimizationRequest request,
                                                Map<String, Map<MetroId, ProviderAvailability>> providerMetroMap,
                                                Map<String, Double> siteLatencies) {
        List<String> reasons = new ArrayList<>();

        // Best latency? Only claimed when there are sites to measure to: an empty average
        // is 0.0ms, which would otherwise be surfaced as "excellent latency to user sites"
        // for a request that defined none.
        OptionalDouble avg = siteLatencies.values().stream()
                .mapToDouble(Double::doubleValue).average();
        if (avg.isPresent()) {
            double avgLatency = avg.getAsDouble();
            if (avgLatency < 30) {
                reasons.add("Excellent average latency of " + String.format("%.1fms", avgLatency) + " to user sites");
            } else if (avgLatency < 80) {
                reasons.add("Good average latency of " + String.format("%.1fms", avgLatency) + " to user sites");
            }
        }

        // Provider availability
        long available = request.getProviders().stream()
                .filter(req -> {
                    Map<MetroId, ProviderAvailability> avail = providerMetroMap.get(req.displayLabel());
                    return avail != null && avail.containsKey(sm.metro.metroId());
                }).count();
        if (available == request.getProviders().size() && available > 0) {
            reasons.add("All " + available + " required/preferred providers available");
        } else if (available > 0) {
            reasons.add(available + " of " + request.getProviders().size() + " providers available");
        }

        // Region
        reasons.add("Located in " + sm.metro.getRegion() + " region");

        return reasons;
    }

    /**
     * Internal holder for a metro and its computed score during ranking.
     */
    static final class ScoredMetro {
        final Metro metro;
        final MetroScore score;

        ScoredMetro(Metro metro, MetroScore score) {
            this.metro = metro;
            this.score = score;
        }
    }

}
