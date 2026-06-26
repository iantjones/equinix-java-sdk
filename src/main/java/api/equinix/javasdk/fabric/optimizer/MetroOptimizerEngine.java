package api.equinix.javasdk.fabric.optimizer;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.fabric.mcp.bridge.McpBridge;
import api.equinix.javasdk.fabric.mcp.bridge.McpMetroBridge;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.fabric.optimizer.enums.*;
import api.equinix.javasdk.fabric.optimizer.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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

    private MetroOptimizerEngine() {}

    /**
     * Executes the full optimization pipeline against live Fabric API data.
     *
     * <p>The pipeline consists of 12 phases:</p>
     * <ol>
     *   <li><strong>Data Collection</strong> -- Fetches all metros and service profiles via the Fabric client.</li>
     *   <li><strong>Candidate Filtering</strong> -- Eliminates metros that violate hard constraints
     *       (excluded regions/metros, compliance zones, missing required providers).</li>
     *   <li><strong>Scoring</strong> -- Scores each candidate across five dimensions: latency, provider
     *       coverage, cost, redundancy, and compliance, using weighted aggregation.</li>
     *   <li><strong>Selection</strong> -- Selects the top N metros by composite score.</li>
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
     * @see OptimizationResult
     */
    static OptimizationResult execute(OptimizationRequest request, Fabric fabric) {
        long startTime = System.currentTimeMillis();

        // Phase 1: Data Collection
        List<Metro> allMetros = new ArrayList<>(fabric.metros().list());
        List<ServiceProfile> serviceProfiles = new ArrayList<>(fabric.serviceProfiles().search());

        // Build lookup maps
        Map<MetroCode, Metro> metroMap = new HashMap<>();
        for (Metro m : allMetros) {
            metroMap.put(m.getCode(), m);
        }

        Map<MetroCode, Map<MetroCode, Double>> latencyMap = buildLatencyMap(allMetros);

        // Phase 1b: MCP Enrichment (optional)
        Map<String, McpMetroBridge.McpMetro> mcpMetroData = enrichWithMcp(request.getMcpBridge(), allMetros);

        // Build provider → metro availability map
        Map<String, Map<MetroCode, ProviderAvailability>> providerMetroMap =
                buildProviderMetroMap(request.getProviders(), serviceProfiles);

        // Phase 2: Candidate Filtering
        List<Metro> candidates = filterCandidates(allMetros, request, providerMetroMap);

        // Phase 3: Scoring
        int totalHeadcount = request.getSites().stream().mapToInt(UserSite::getHeadcount).sum();
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
            double latencyScore = scoreLatency(candidate, request, metroMap, latencyMap, totalHeadcount, weights);
            double providerScore = scoreProviderCoverage(candidate, request, providerMetroMap, weights);
            double costScore = scoreCost(candidate, request);
            double redundancyScore = 50.0; // baseline, refined in Phase 5
            double complianceScore = scoreCompliance(candidate, request);

            List<ScoreComponent> components = Arrays.asList(
                    new ScoreComponent(ScoreCategory.LATENCY, latencyScore, wLatency,
                            describeLatencyScore(candidate, request, metroMap, latencyMap, totalHeadcount)),
                    new ScoreComponent(ScoreCategory.PROVIDER_COVERAGE, providerScore, wProvider,
                            describeProviderScore(candidate, request, providerMetroMap)),
                    new ScoreComponent(ScoreCategory.COST, costScore, wCost,
                            "Cost score based on estimated connection pricing"),
                    new ScoreComponent(ScoreCategory.REDUNDANCY, redundancyScore, wRedundancy,
                            "Baseline redundancy score; refined after topology assembly"),
                    new ScoreComponent(ScoreCategory.COMPLIANCE, complianceScore, wCompliance,
                            complianceScore >= 100 ? "Meets all compliance requirements"
                                    : "Some compliance zones not fully satisfied")
            );

            double composite = components.stream()
                    .mapToDouble(ScoreComponent::weightedScore)
                    .sum();

            scoredMetros.add(new ScoredMetro(candidate, new MetroScore(composite, components)));
        }

        // Sort by composite score descending
        scoredMetros.sort((a, b) -> Double.compare(b.score.getComposite(), a.score.getComposite()));

        // Phase 4: Select top N
        int maxMetros = resolveMaxMetros(request);
        List<ScoredMetro> selected = scoredMetros.stream()
                .limit(maxMetros)
                .collect(Collectors.toList());

        // Phase 5: Refine redundancy scores for selected set
        selected = refineRedundancyScores(selected, request, wRedundancy);

        // Phase 6: Build latency matrix
        LatencyMatrix latencyMatrix = buildLatencyMatrix(selected, request, metroMap, latencyMap);

        // Phase 7: Provider connectivity map
        ProviderConnectivityMap providerConnMap = buildProviderConnectivityMap(selected, request, providerMetroMap);

        // Phase 8: Topology assembly (workload placement)
        DeploymentTopology topology = assembleTopology(selected, request, metroMap, latencyMap, providerMetroMap, totalHeadcount);

        // Phase 9: Risk analysis
        RiskAssessment riskAssessment = analyzeRisks(selected, topology, request, providerMetroMap, latencyMatrix);

        // Phase 10: Cost estimate
        CostEstimate costEstimate = estimateCosts(selected, request);

        // Phase 11: Build recommendations
        List<MetroRecommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            ScoredMetro sm = selected.get(i);
            Metro metro = sm.metro;
            MetroCode code = metro.getCode();

            Map<String, Double> siteLatencies = new LinkedHashMap<>();
            for (UserSite site : request.getSites()) {
                double lat = estimateLatency(code, site, metroMap, latencyMap);
                siteLatencies.put(site.getLabel(), lat);
            }

            List<ProviderAvailability> provAvail = providerConnMap.forMetro(code);
            MetroCostBreakdown metroCost = costEstimate.getPerMetro().stream()
                    .filter(c -> c.getMetroCode() == code)
                    .findFirst().orElse(null);

            List<WorkloadPlacement> metroWorkloads = topology.forMetro(code);
            List<String> reasons = generateReasons(sm, request, providerMetroMap, siteLatencies);

            recommendations.add(MetroRecommendation.builder()
                    .rank(i + 1)
                    .metroCode(code)
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
        OptimizationExplanation explanation = buildExplanation(request, allMetros.size(), candidates.size());

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
    //  Latency Map
    // ══════════════════════════════════════════════

    /** Builds a bidirectional metro-to-metro latency lookup from Fabric connected metro data. */
    private static Map<MetroCode, Map<MetroCode, Double>> buildLatencyMap(List<Metro> metros) {
        Map<MetroCode, Map<MetroCode, Double>> map = new HashMap<>();
        for (Metro metro : metros) {
            Map<MetroCode, Double> connections = new HashMap<>();
            if (metro.getConnectedMetros() != null) {
                for (ConnectedMetro cm : metro.getConnectedMetros()) {
                    if (cm.getAvgLatency() != null) {
                        connections.put(cm.getCode(), cm.getAvgLatency());
                    }
                }
            }
            map.put(metro.getCode(), connections);
        }
        return map;
    }

    // ══════════════════════════════════════════════
    //  Provider Metro Map
    // ══════════════════════════════════════════════

    /** Maps each provider requirement to the metros where matching service profiles are available. */
    private static Map<String, Map<MetroCode, ProviderAvailability>> buildProviderMetroMap(
            List<ProviderRequirement> requirements, List<ServiceProfile> profiles) {

        Map<String, Map<MetroCode, ProviderAvailability>> result = new HashMap<>();

        for (ProviderRequirement req : requirements) {
            String key = req.displayLabel();
            Map<MetroCode, ProviderAvailability> metroAvail = new HashMap<>();

            for (ServiceProfile profile : profiles) {
                if (matchesProvider(profile, req)) {
                    List<ServiceProfileMetro> metros = profile.metros();
                    if (metros != null) {
                        for (ServiceProfileMetro spm : metros) {
                            List<String> regions = spm.getSellerRegions() != null
                                    ? new ArrayList<>(spm.getSellerRegions().keySet())
                                    : Collections.emptyList();
                            metroAvail.put(spm.getCode(),
                                    new ProviderAvailability(key, true, regions, profile.getUuid()));
                        }
                    }
                    break; // matched this requirement
                }
            }
            result.put(key, metroAvail);
        }
        return result;
    }

    private static boolean matchesProvider(ServiceProfile profile, ProviderRequirement req) {
        if (req.getServiceProfileUuid() != null) {
            return req.getServiceProfileUuid().equals(profile.getUuid());
        }
        if (req.getServiceProfileName() != null) {
            return profile.getName() != null
                    && profile.getName().toLowerCase().contains(req.getServiceProfileName().toLowerCase());
        }
        if (req.getCloudProvider() != null) {
            return profile.getName() != null
                    && profile.getName().toLowerCase().contains(
                    req.getCloudProvider().getProviderName().toLowerCase());
        }
        return false;
    }

    // ══════════════════════════════════════════════
    //  Candidate Filtering
    // ══════════════════════════════════════════════

    /** Filters metros against hard constraints: excluded metros/regions, compliance zones, required providers. */
    private static List<Metro> filterCandidates(List<Metro> allMetros, OptimizationRequest request,
                                                 Map<String, Map<MetroCode, ProviderAvailability>> providerMetroMap) {
        OptimizationConstraints c = request.getConstraints();
        List<Metro> filtered = new ArrayList<>();

        Set<MetroCode> excluded = c.getExcludedMetros() != null
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

        for (Metro metro : allMetros) {
            MetroCode code = metro.getCode();
            Region region = metro.getRegion();

            if (excluded.contains(code)) continue;
            if (excludedRegions.contains(region)) continue;
            if (!requiredRegions.isEmpty() && !requiredRegions.contains(region)) continue;
            if (complianceAllowed != null && !complianceAllowed.contains(region)) continue;

            // Check required providers
            boolean hasAllRequired = true;
            for (ProviderRequirement req : request.getProviders()) {
                if (req.isRequired()) {
                    String key = req.displayLabel();
                    Map<MetroCode, ProviderAvailability> avail = providerMetroMap.get(key);
                    if (avail == null || !avail.containsKey(code)) {
                        hasAllRequired = false;
                        break;
                    }
                }
            }
            if (!hasAllRequired) continue;

            filtered.add(metro);
        }

        // Ensure required metros are included
        if (c.getRequiredMetros() != null) {
            Set<MetroCode> filteredCodes = filtered.stream()
                    .map(Metro::getCode).collect(Collectors.toSet());
            for (MetroCode required : c.getRequiredMetros()) {
                if (!filteredCodes.contains(required)) {
                    allMetros.stream()
                            .filter(m -> m.getCode() == required)
                            .findFirst()
                            .ifPresent(filtered::add);
                }
            }
        }

        return filtered;
    }

    // ══════════════════════════════════════════════
    //  Latency Scoring
    // ══════════════════════════════════════════════

    /** Scores a candidate metro on latency using weighted average distance to all user sites. */
    private static double scoreLatency(Metro candidate, OptimizationRequest request,
                                       Map<MetroCode, Metro> metroMap,
                                       Map<MetroCode, Map<MetroCode, Double>> latencyMap,
                                       int totalHeadcount, ScoringWeights weights) {
        if (request.getSites().isEmpty()) return 100.0;

        double weightedLatency = 0;
        double totalSiteWeight = 0;

        for (UserSite site : request.getSites()) {
            double siteWeight = site.effectiveWeight(totalHeadcount)
                    * site.getRole().getImportanceMultiplier();
            double latency = estimateLatency(candidate.getCode(), site, metroMap, latencyMap);
            weightedLatency += latency * siteWeight;
            totalSiteWeight += siteWeight;
        }

        if (totalSiteWeight == 0) return 100.0;
        double avgWeightedLatency = weightedLatency / totalSiteWeight;

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

    private static String describeLatencyScore(Metro candidate, OptimizationRequest request,
                                               Map<MetroCode, Metro> metroMap,
                                               Map<MetroCode, Map<MetroCode, Double>> latencyMap,
                                               int totalHeadcount) {
        if (request.getSites().isEmpty()) return "No sites defined";

        StringBuilder sb = new StringBuilder();
        for (UserSite site : request.getSites()) {
            double lat = estimateLatency(candidate.getCode(), site, metroMap, latencyMap);
            sb.append(String.format("%s: %.1fms", site.getLabel(), lat));
            if (request.getSites().indexOf(site) < request.getSites().size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Estimates latency from a metro to a user site. Tries direct metro-to-metro lookups first,
     * then falls back to Haversine distance-based estimation using fiber optic constants.
     */
    static double estimateLatency(MetroCode from, UserSite site,
                                          Map<MetroCode, Metro> metroMap,
                                          Map<MetroCode, Map<MetroCode, Double>> latencyMap) {
        MetroCode siteMetro = site.getNearestMetro();

        // If the site has a metro code, try direct latency lookup
        if (siteMetro != null) {
            if (siteMetro == from) return 0.5; // same metro, sub-ms

            Map<MetroCode, Double> fromConnections = latencyMap.get(from);
            if (fromConnections != null) {
                Double directLatency = fromConnections.get(siteMetro);
                if (directLatency != null) return directLatency;
            }

            // Reverse lookup
            Map<MetroCode, Double> siteConnections = latencyMap.get(siteMetro);
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

    /** Computes great-circle distance in kilometers between two geographic coordinates. */
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

    /** Scores a candidate metro on provider coverage, weighting required providers more heavily. */
    private static double scoreProviderCoverage(Metro candidate, OptimizationRequest request,
                                                Map<String, Map<MetroCode, ProviderAvailability>> providerMetroMap,
                                                ScoringWeights weights) {
        if (request.getProviders().isEmpty()) return 100.0;

        double totalWeight = 0;
        double matchedWeight = 0;
        double reqWeight = weights.resolveRequiredProviderWeight();

        for (ProviderRequirement req : request.getProviders()) {
            double provWeight = req.isRequired() ? reqWeight : 1.0;
            totalWeight += provWeight;

            String key = req.displayLabel();
            Map<MetroCode, ProviderAvailability> avail = providerMetroMap.get(key);
            if (avail != null && avail.containsKey(candidate.getCode())) {
                matchedWeight += provWeight;

                // Bonus for matching preferred seller regions
                if (req.getPreferredSellerRegions() != null && !req.getPreferredSellerRegions().isEmpty()) {
                    ProviderAvailability pa = avail.get(candidate.getCode());
                    long regionMatches = req.getPreferredSellerRegions().stream()
                            .filter(r -> pa.getSellerRegions() != null && pa.getSellerRegions().contains(r))
                            .count();
                    double regionBonus = (double) regionMatches / req.getPreferredSellerRegions().size() * 10.0;
                    matchedWeight += Math.min(regionBonus * provWeight / reqWeight, provWeight * 0.1);
                }
            }
        }

        return totalWeight > 0 ? Math.min(100.0, (matchedWeight / totalWeight) * 100.0) : 100.0;
    }

    private static String describeProviderScore(Metro candidate, OptimizationRequest request,
                                                Map<String, Map<MetroCode, ProviderAvailability>> providerMetroMap) {
        long available = request.getProviders().stream()
                .filter(req -> {
                    Map<MetroCode, ProviderAvailability> avail = providerMetroMap.get(req.displayLabel());
                    return avail != null && avail.containsKey(candidate.getCode());
                })
                .count();
        return available + "/" + request.getProviders().size() + " providers available";
    }

    // ══════════════════════════════════════════════
    //  Cost Scoring
    // ══════════════════════════════════════════════

    /** Scores a candidate metro on cost using a simplified regional pricing model. */
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

    /** Scores a candidate metro on compliance zone alignment (0 if any zone is violated, 100 otherwise). */
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

    /** Refines redundancy scores for the selected metro set based on actual geographic diversity. */
    private static List<ScoredMetro> refineRedundancyScores(List<ScoredMetro> selected,
                                                             OptimizationRequest request,
                                                             double wRedundancy) {
        if (selected.size() <= 1) return selected;

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
                    newComponents.add(new ScoreComponent(ScoreCategory.REDUNDANCY, redundancyScore,
                            wRedundancy, describeRedundancy(selected, regions)));
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
        return selected.size() + " metros across " + regions.size() + " region(s): "
                + regions.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    // ══════════════════════════════════════════════
    //  Latency Matrix
    // ══════════════════════════════════════════════

    private static LatencyMatrix buildLatencyMatrix(List<ScoredMetro> selected, OptimizationRequest request,
                                                     Map<MetroCode, Metro> metroMap,
                                                     Map<MetroCode, Map<MetroCode, Double>> latencyMap) {
        List<MetroCode> metroCodes = selected.stream()
                .map(sm -> sm.metro.getCode())
                .collect(Collectors.toList());
        List<String> siteLabels = request.getSites().stream()
                .map(UserSite::getLabel)
                .collect(Collectors.toList());

        List<List<LatencyEntry>> matrix = new ArrayList<>();
        for (ScoredMetro sm : selected) {
            List<LatencyEntry> row = new ArrayList<>();
            for (UserSite site : request.getSites()) {
                double latency = estimateLatency(sm.metro.getCode(), site, metroMap, latencyMap);
                boolean estimated = isEstimatedLatency(sm.metro.getCode(), site, latencyMap);
                row.add(new LatencyEntry(sm.metro.getCode(), site.getLabel(), latency, estimated));
            }
            matrix.add(row);
        }

        return new LatencyMatrix(metroCodes, siteLabels, matrix);
    }

    private static boolean isEstimatedLatency(MetroCode from, UserSite site,
                                               Map<MetroCode, Map<MetroCode, Double>> latencyMap) {
        MetroCode siteMetro = site.getNearestMetro();
        if (siteMetro == null) return true;
        if (siteMetro == from) return false;
        Map<MetroCode, Double> connections = latencyMap.get(from);
        if (connections != null && connections.containsKey(siteMetro)) return false;
        Map<MetroCode, Double> reverse = latencyMap.get(siteMetro);
        return reverse == null || !reverse.containsKey(from);
    }

    // ══════════════════════════════════════════════
    //  Provider Connectivity Map
    // ══════════════════════════════════════════════

    private static ProviderConnectivityMap buildProviderConnectivityMap(
            List<ScoredMetro> selected, OptimizationRequest request,
            Map<String, Map<MetroCode, ProviderAvailability>> providerMetroMap) {

        Map<MetroCode, List<ProviderAvailability>> map = new LinkedHashMap<>();
        for (ScoredMetro sm : selected) {
            MetroCode code = sm.metro.getCode();
            List<ProviderAvailability> provList = new ArrayList<>();
            for (ProviderRequirement req : request.getProviders()) {
                String key = req.displayLabel();
                Map<MetroCode, ProviderAvailability> avail = providerMetroMap.get(key);
                if (avail != null && avail.containsKey(code)) {
                    provList.add(avail.get(code));
                } else {
                    provList.add(new ProviderAvailability(key, false, Collections.emptyList(), null));
                }
            }
            map.put(code, provList);
        }
        return new ProviderConnectivityMap(map);
    }

    // ══════════════════════════════════════════════
    //  Topology Assembly
    // ══════════════════════════════════════════════

    /** Assigns workloads to metros using a greedy strategy: DR to alternate regions, latency-critical to lowest-latency, provider-dependent to best match. */
    private static DeploymentTopology assembleTopology(List<ScoredMetro> selected, OptimizationRequest request,
                                                       Map<MetroCode, Metro> metroMap,
                                                       Map<MetroCode, Map<MetroCode, Double>> latencyMap,
                                                       Map<String, Map<MetroCode, ProviderAvailability>> providerMetroMap,
                                                       int totalHeadcount) {
        List<WorkloadPlacement> placements = new ArrayList<>();
        if (selected.isEmpty()) return new DeploymentTopology(placements);

        MetroCode primaryMetro = selected.get(0).metro.getCode();
        Region primaryRegion = selected.get(0).metro.getRegion();

        for (WorkloadSpec workload : request.getWorkloads()) {
            WorkloadProfile profile = workload.resolvedProfile();
            LatencySensitivity sensitivity = profile.getDefaultLatencySensitivity();

            // DR workloads: place in a different region if possible
            if (workload.getType() == WorkloadType.DISASTER_RECOVERY
                    || workload.getType() == WorkloadType.COLD_BACKUP) {
                ScoredMetro drMetro = selected.stream()
                        .filter(sm -> sm.metro.getRegion() != primaryRegion)
                        .findFirst()
                        .orElse(selected.size() > 1 ? selected.get(selected.size() - 1) : selected.get(0));
                placements.add(new WorkloadPlacement(workload.getLabel(), drMetro.metro.getCode(),
                        "Placed in " + drMetro.metro.getRegion() + " for geographic diversity from primary"));
                continue;
            }

            // Latency-critical or proximity-weighted: place closest to weighted site center
            if (sensitivity == LatencySensitivity.CRITICAL || profile.isProximityWeighted()) {
                ScoredMetro bestLatency = selected.get(0);
                double bestAvg = Double.MAX_VALUE;
                for (ScoredMetro sm : selected) {
                    double avg = 0;
                    double totalW = 0;
                    for (UserSite site : request.getSites()) {
                        double w = site.effectiveWeight(totalHeadcount) * site.getRole().getImportanceMultiplier();
                        avg += estimateLatency(sm.metro.getCode(), site, metroMap, latencyMap) * w;
                        totalW += w;
                    }
                    avg = totalW > 0 ? avg / totalW : avg;
                    if (avg < bestAvg) {
                        bestAvg = avg;
                        bestLatency = sm;
                    }
                }
                placements.add(new WorkloadPlacement(workload.getLabel(), bestLatency.metro.getCode(),
                        "Lowest weighted latency to user sites (" + String.format("%.1fms avg", bestAvg) + ")"));
                continue;
            }

            // Provider-dependent workloads: place where all dependencies are available
            if (workload.getDependsOnProviders() != null && !workload.getDependsOnProviders().isEmpty()) {
                ScoredMetro bestProvider = null;
                for (ScoredMetro sm : selected) {
                    boolean allAvailable = workload.getDependsOnProviders().stream().allMatch(dep -> {
                        String key = dep.displayLabel();
                        Map<MetroCode, ProviderAvailability> avail = providerMetroMap.get(key);
                        return avail != null && avail.containsKey(sm.metro.getCode());
                    });
                    if (allAvailable) {
                        bestProvider = sm;
                        break;
                    }
                }
                if (bestProvider != null) {
                    placements.add(new WorkloadPlacement(workload.getLabel(), bestProvider.metro.getCode(),
                            "All required providers available"));
                    continue;
                }
            }

            // Default: place in highest-scored metro
            placements.add(new WorkloadPlacement(workload.getLabel(), primaryMetro,
                    "Placed in highest-scored metro"));
        }

        return new DeploymentTopology(placements);
    }

    // ══════════════════════════════════════════════
    //  Risk Analysis
    // ══════════════════════════════════════════════

    /** Identifies deployment risks: single points of failure, single-region concentration, latency violations, provider concentration, redundancy gaps. */
    private static RiskAssessment analyzeRisks(List<ScoredMetro> selected, DeploymentTopology topology,
                                               OptimizationRequest request,
                                               Map<String, Map<MetroCode, ProviderAvailability>> providerMetroMap,
                                               LatencyMatrix latencyMatrix) {
        List<RiskFinding> findings = new ArrayList<>();
        RiskSeverity worstSeverity = RiskSeverity.INFO;
        double resiliencyScore = 100.0;

        // Single point of failure
        if (selected.size() == 1) {
            findings.add(new RiskFinding(RiskSeverity.HIGH, "SINGLE_POINT_OF_FAILURE",
                    "All workloads are assigned to a single metro (" + selected.get(0).metro.getCode() + ")",
                    "Add at least one additional metro for redundancy",
                    selected.get(0).metro.getCode()));
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
                findings.add(new RiskFinding(RiskSeverity.HIGH, "SINGLE_REGION",
                        "All " + selected.size() + " metros are in " + regions.iterator().next()
                                + " but MULTI_REGION redundancy was requested",
                        "Consider expanding to metros in other regions",
                        null));
                resiliencyScore -= 25;
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.HIGH);
            } else {
                findings.add(new RiskFinding(RiskSeverity.MEDIUM, "SINGLE_REGION",
                        "All metros are in the " + regions.iterator().next() + " region",
                        "Consider adding a metro in another region for disaster resilience",
                        null));
                resiliencyScore -= 10;
                worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
            }
        }

        // Latency threshold violations
        Double maxLatency = request.getConstraints().getMaxLatencyMs();
        if (maxLatency != null) {
            for (MetroCode metro : latencyMatrix.getMetros()) {
                double worst = latencyMatrix.worstCase(metro);
                if (worst > maxLatency) {
                    findings.add(new RiskFinding(RiskSeverity.MEDIUM, "LATENCY_THRESHOLD",
                            metro + " has worst-case latency of " + String.format("%.1fms", worst)
                                    + " which exceeds the " + String.format("%.0fms", maxLatency) + " threshold",
                            "Consider adding a metro closer to the affected site",
                            metro));
                    resiliencyScore -= 5;
                    worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
                }
            }
        }

        // Provider concentration
        for (ProviderRequirement req : request.getProviders()) {
            if (req.isRequired()) {
                String key = req.displayLabel();
                Map<MetroCode, ProviderAvailability> avail = providerMetroMap.get(key);
                long availableCount = selected.stream()
                        .filter(sm -> avail != null && avail.containsKey(sm.metro.getCode()))
                        .count();
                if (availableCount == 1) {
                    findings.add(new RiskFinding(RiskSeverity.MEDIUM, "PROVIDER_CONCENTRATION",
                            key + " is only available in 1 of " + selected.size() + " recommended metros",
                            "Consider selecting metros where " + key + " has broader presence",
                            null));
                    resiliencyScore -= 5;
                    worstSeverity = mostSevere(worstSeverity, RiskSeverity.MEDIUM);
                }
            }
        }

        // Redundancy gap
        RedundancyTier requested = request.getConstraints().getMinimumRedundancy();
        if (requested != null && selected.size() < requested.getMinimumMetros()) {
            findings.add(new RiskFinding(RiskSeverity.CRITICAL, "REDUNDANCY_GAP",
                    "Requested " + requested + " redundancy requires at least " + requested.getMinimumMetros()
                            + " metros but only " + selected.size() + " were selected",
                    "Relax constraints to allow more eligible metros",
                    null));
            resiliencyScore -= 30;
            worstSeverity = RiskSeverity.CRITICAL;
        }

        if (findings.isEmpty()) {
            findings.add(new RiskFinding(RiskSeverity.INFO, "HEALTHY",
                    "No significant risks identified in the recommended topology",
                    null, null));
        }

        return new RiskAssessment(findings, worstSeverity, Math.max(0, resiliencyScore));
    }

    private static RiskSeverity mostSevere(RiskSeverity a, RiskSeverity b) {
        return a.ordinal() <= b.ordinal() ? a : b; // CRITICAL=0 is "higher" severity
    }

    // ══════════════════════════════════════════════
    //  Cost Estimation
    // ══════════════════════════════════════════════

    /** Produces per-metro and aggregate cost estimates using a simplified regional pricing model. */
    private static CostEstimate estimateCosts(List<ScoredMetro> selected, OptimizationRequest request) {
        List<MetroCostBreakdown> perMetro = new ArrayList<>();
        BigDecimal totalMonthly = BigDecimal.ZERO;
        BigDecimal totalSetup = BigDecimal.ZERO;

        int totalBandwidth = request.getWorkloads().stream()
                .mapToInt(WorkloadSpec::getBandwidthMbps)
                .sum();

        for (ScoredMetro sm : selected) {
            // Simplified cost model: base port cost + per-Mbps connection cost
            // Actual costs would come from the Pricing API in production
            BigDecimal basePortCost = BigDecimal.valueOf(500);
            BigDecimal perMbpsCost = BigDecimal.valueOf(0.50);
            BigDecimal setupCost = BigDecimal.valueOf(1000);

            // Regional multiplier
            double regionMultiplier = 1.0;
            if (sm.metro.getRegion() == Region.EMEA) regionMultiplier = 1.2;
            else if (sm.metro.getRegion() == Region.APAC) regionMultiplier = 1.4;

            BigDecimal monthly = basePortCost.add(
                    perMbpsCost.multiply(BigDecimal.valueOf(totalBandwidth / Math.max(1, selected.size()))))
                    .multiply(BigDecimal.valueOf(regionMultiplier));
            BigDecimal setup = setupCost.multiply(BigDecimal.valueOf(regionMultiplier));

            Map<String, BigDecimal> lineItems = new LinkedHashMap<>();
            lineItems.put("Base port", basePortCost);
            lineItems.put("Bandwidth allocation", perMbpsCost.multiply(
                    BigDecimal.valueOf(totalBandwidth / Math.max(1, selected.size()))));
            lineItems.put("Regional adjustment", monthly.subtract(basePortCost));

            perMetro.add(new MetroCostBreakdown(sm.metro.getCode(), monthly, setup, lineItems));
            totalMonthly = totalMonthly.add(monthly);
            totalSetup = totalSetup.add(setup);
        }

        boolean withinBudget = true;
        BudgetRange budget = request.getConstraints().getBudget();
        if (budget != null && budget.getMaxMonthly() != null) {
            withinBudget = totalMonthly.compareTo(budget.getMaxMonthly()) <= 0;
        }

        return new CostEstimate(totalMonthly, totalSetup, "USD", perMetro, withinBudget,
                "Estimates based on simplified pricing model. Actual costs vary by connection type, "
                        + "bandwidth tier, and contract terms. Contact your Equinix account team for precise quotes.");
    }

    // ══════════════════════════════════════════════
    //  Explanation
    // ══════════════════════════════════════════════

    /** Builds a human-readable explanation of the optimization methodology and assumptions. */
    private static OptimizationExplanation buildExplanation(OptimizationRequest request,
                                                             int totalMetros, int candidateMetros) {
        String methodology = "The optimizer evaluates all " + totalMetros + " Equinix metros, filtering to "
                + candidateMetros + " candidates based on constraints (excluded metros/regions, compliance zones, "
                + "required provider availability). Each candidate is scored across 5 dimensions: "
                + "latency (weighted by workforce distribution and site importance), provider coverage "
                + "(availability of required and preferred providers), cost (regional pricing estimates), "
                + "redundancy (geographic diversity of the selected set), and compliance (data sovereignty). "
                + "Scores are combined using the " + request.getStrategy() + " strategy weights"
                + (request.getScoringWeights() != null ? " with user-customized overrides" : "")
                + ". Workloads are placed using a greedy algorithm: latency-critical workloads go to the "
                + "lowest-latency metro, DR workloads to a different region, and provider-dependent workloads "
                + "to metros where all dependencies are available.";

        List<String> assumptions = Arrays.asList(
                "Latency between metros uses Equinix Fabric avgLatency data where available; "
                        + "otherwise estimated via Haversine distance × 1.4 fiber multiplier × 4.9μs/km",
                "Cost estimates use a simplified model; actual pricing depends on contract terms",
                "Provider availability is based on current Fabric marketplace service profiles",
                "Workload placement uses greedy assignment, not global optimization"
        );

        return new OptimizationExplanation(methodology, assumptions,
                "Data fetched at optimization time from live Fabric APIs",
                "Analyzed " + totalMetros + " metros, " + candidateMetros + " met constraints, "
                        + "selected top " + request.getConstraints().getMaxMetroCount()
                        + " by " + request.getStrategy() + " strategy");
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private static int resolveMaxMetros(OptimizationRequest request) {
        Integer max = request.getConstraints().getMaxMetroCount();
        if (max != null && max > 0) return max;

        RedundancyTier redundancy = request.getConstraints().getMinimumRedundancy();
        if (redundancy != null) return Math.max(3, redundancy.getMinimumMetros() + 1);

        return 3; // default
    }

    private static List<String> generateReasons(ScoredMetro sm, OptimizationRequest request,
                                                Map<String, Map<MetroCode, ProviderAvailability>> providerMetroMap,
                                                Map<String, Double> siteLatencies) {
        List<String> reasons = new ArrayList<>();

        // Best latency?
        double avgLatency = siteLatencies.values().stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        if (avgLatency < 30) {
            reasons.add("Excellent average latency of " + String.format("%.1fms", avgLatency) + " to user sites");
        } else if (avgLatency < 80) {
            reasons.add("Good average latency of " + String.format("%.1fms", avgLatency) + " to user sites");
        }

        // Provider availability
        long available = request.getProviders().stream()
                .filter(req -> {
                    Map<MetroCode, ProviderAvailability> avail = providerMetroMap.get(req.displayLabel());
                    return avail != null && avail.containsKey(sm.metro.getCode());
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

    /**
     * Enriches metro data with real-time information from the MCP server.
     * Returns a map of metro code to MCP metro data for metros that were
     * successfully enriched. Failures are silently ignored to ensure the
     * optimizer continues to function without MCP.
     */
    private static Map<String, McpMetroBridge.McpMetro> enrichWithMcp(
            McpBridge mcpBridge, List<Metro> allMetros) {

        Map<String, McpMetroBridge.McpMetro> mcpData = new HashMap<>();

        if (mcpBridge == null) {
            return mcpData;
        }

        try {
            List<McpMetroBridge.McpMetro> mcpMetros = mcpBridge.metros().listMetros();
            for (McpMetroBridge.McpMetro mcpMetro : mcpMetros) {
                if (mcpMetro.getCode() != null) {
                    mcpData.put(mcpMetro.getCode(), mcpMetro);
                }
            }
        } catch (Exception e) {
            // MCP enrichment is optional; continue with standard data
        }

        return mcpData;
    }
}
