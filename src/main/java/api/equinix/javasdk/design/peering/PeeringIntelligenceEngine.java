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

package api.equinix.javasdk.design.peering;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.peering.client.*;
import api.equinix.javasdk.design.peering.enums.*;
import api.equinix.javasdk.design.peering.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Core analysis engine for Peering Intelligence.
 *
 * <p>Executes a multi-phase pipeline:</p>
 * <ol>
 *   <li>Load Equinix catalog from PeeringDB (org/2 with depth=2)</li>
 *   <li>Build IX ID → MetroCode mapping</li>
 *   <li>Query PeeringDB for each target ASN (netixlan + netfac + net)</li>
 *   <li>Filter to Equinix IXes and facilities</li>
 *   <li>Build NetworkPresence per ASN</li>
 *   <li>Build PresenceMatrix (ASN × Metro grid)</li>
 *   <li>Build MetroPresenceReports (per metro)</li>
 *   <li>Optionally: cross-reference Fabric service profiles</li>
 *   <li>Optionally: perform resiliency analysis (blast radius, correlated failures)</li>
 *   <li>Optionally: discover mutual peering opportunities</li>
 *   <li>Build unified connectivity views</li>
 *   <li>Assemble final result</li>
 * </ol>
 *
 * @author ianjones
 */
class PeeringIntelligenceEngine {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final FabricGateway fabric;
    private final PeeringDbClient peeringDb;
    private final PeeringRequest request;

    private EquinixIXMapping ixMapping;
    private final Map<Long, PeeringDbNetwork> networkMetadata = new LinkedHashMap<>();
    private final Map<Long, List<PeeringDbNetIxlan>> ixPresenceByAsn = new LinkedHashMap<>();
    private final Map<Long, List<PeeringDbNetFac>> facPresenceByAsn = new LinkedHashMap<>();
    private final Map<Long, NetworkPresence> networkPresences = new LinkedHashMap<>();

    /**
     * Live Fabric metro geo data, loaded once per analysis from {@code fabric.metros()}: metro →
     * [latitude, longitude] (used to bind PeeringDB facilities/IXes to metros and to compute
     * geographic diversity) and metro → region (used for the same-region diversity descriptor).
     * Well-known metros only — {@link MetroCode#UNKNOWN} is skipped.
     */
    private final Map<MetroCode, double[]> metroCoordinates = new LinkedHashMap<>();
    private final Map<MetroCode, String> metroRegion = new LinkedHashMap<>();

    PeeringIntelligenceEngine(FabricGateway fabric, PeeringDbClient peeringDb, PeeringRequest request) {
        this.fabric = fabric;
        this.peeringDb = peeringDb;
        this.request = request;
    }

    /**
     * Executes the full analysis pipeline and returns the result.
     */
    PeeringIntelligenceResult execute() {
        long startTime = System.currentTimeMillis();
        List<String> dataSources = new ArrayList<>();
        dataSources.add("PeeringDB");

        try {
            // Phase 1: Load the Equinix catalog (PeeringDB) and live metro geo (Fabric), then build
            // the IX/facility -> metro bridge from live coordinates — facilities first (they carry
            // lat/lng and seed the city bridge), then IXes (city-only, resolved via that bridge).
            peeringDb.loadEquinixCatalog();
            loadMetroGeo();
            if (!metroCoordinates.isEmpty()) {
                dataSources.add("Equinix Fabric");
            }
            ixMapping = new EquinixIXMapping(metroCoordinates);
            ixMapping.mapFacilities(peeringDb.getEquinixFacMap());
            ixMapping.mapIxes(peeringDb.getEquinixIxMap());

            // Phase 2: Query each target ASN
            for (Map.Entry<Long, String> entry : request.getTargetAsns().entrySet()) {
                long asn = entry.getKey();
                queryAsn(asn);
            }

            // Phase 3: Build NetworkPresence per ASN
            buildNetworkPresences();

            // Phase 4: Build PresenceMatrix
            PresenceMatrix matrix = buildPresenceMatrix();

            // Phase 5: Build MetroPresenceReports
            Map<MetroCode, MetroPresenceReport> metroReports = buildMetroReports(matrix);

            // Phase 6: Resiliency analysis (if requested)
            ResiliencyAssessment resiliency = null;
            if (request.isIncludeResiliency() && !request.getCustomerMetros().isEmpty()) {
                resiliency = buildResiliencyAssessment(matrix);
            }

            // Phase 7: Peering opportunity discovery (if customer ASN provided)
            List<PeeringOpportunity> opportunities = Collections.emptyList();
            if (request.getCustomerAsn() > 0) {
                opportunities = discoverPeeringOpportunities();
            }

            // Phase 8: Unified connectivity views
            Map<Long, UnifiedConnectivityView> unifiedViews = buildUnifiedViews(matrix);

            long computeTime = System.currentTimeMillis() - startTime;

            return PeeringIntelligenceResult.builder()
                    .request(request)
                    .presenceMatrix(matrix)
                    .networkPresences(networkPresences)
                    .metroReports(metroReports)
                    .resiliency(resiliency)
                    .unifiedViews(unifiedViews)
                    .peeringOpportunities(opportunities)
                    .computedAt(Instant.now())
                    .computeTimeMs(computeTime)
                    .dataSources(dataSources)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("PeeringDB API call failed: " + e.getMessage(), e);
        }
    }

    // ---- Phase 2: Per-ASN data collection ----

    private void queryAsn(long asn) throws IOException {
        // The three PeeringDB GETs for a single ASN (net, netixlan, netfac) are
        // independent of one another, so they are fanned out onto a virtual-thread
        // executor to overlap their blocking I/O. The ASN loop itself stays
        // sequential (see execute()) to avoid bursting PeeringDB's anonymous
        // ~20 req/min rate limit. Results and exception behaviour are identical to
        // running the three calls in series: a failed sub-call still surfaces as an
        // IOException out of this method.
        PeeringDbNetwork net;
        List<PeeringDbNetIxlan> ixPresence;
        List<PeeringDbNetFac> facPresence;

        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<PeeringDbNetwork> netFuture = exec.submit(() -> peeringDb.getNetwork(asn));
            Future<List<PeeringDbNetIxlan>> ixFuture = exec.submit(() -> peeringDb.getEquinixIxPresence(asn));
            Future<List<PeeringDbNetFac>> facFuture = exec.submit(() -> peeringDb.getEquinixFacPresence(asn));

            net = awaitResult(netFuture);
            ixPresence = awaitResult(ixFuture);
            facPresence = awaitResult(facFuture);
        }

        if (net != null) {
            networkMetadata.put(asn, net);
        }
        ixPresenceByAsn.put(asn, ixPresence);
        facPresenceByAsn.put(asn, facPresence);
    }

    /**
     * Joins a per-ASN PeeringDB sub-call, unwrapping its result while preserving the
     * exact exception behaviour of the original sequential calls: an {@link IOException}
     * thrown by the underlying GET is re-thrown as-is, and any other failure is wrapped
     * in an {@link IOException} so it still surfaces from {@link #queryAsn(long)}.
     */
    private <T> T awaitResult(Future<T> future) throws IOException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IOException("PeeringDB query failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while querying PeeringDB", e);
        }
    }

    // ---- Phase 3: Build NetworkPresence ----

    private void buildNetworkPresences() {
        for (Map.Entry<Long, String> entry : request.getTargetAsns().entrySet()) {
            long asn = entry.getKey();
            String userLabel = entry.getValue();

            PeeringDbNetwork net = networkMetadata.get(asn);
            List<PeeringDbNetIxlan> ixEntries = ixPresenceByAsn.getOrDefault(asn, Collections.emptyList());
            List<PeeringDbNetFac> facEntries = facPresenceByAsn.getOrDefault(asn, Collections.emptyList());

            // Resolve label
            String label = userLabel != null ? userLabel :
                    (net != null ? net.getName() : "AS" + asn);

            // Build IX details and metro sets
            Set<MetroCode> ixMetros = new LinkedHashSet<>();
            List<IxPresenceDetail> ixDetails = new ArrayList<>();
            boolean anyRouteServer = false;
            boolean anyBfd = false;
            boolean anyIpv6 = false;
            long totalCapacity = 0;

            for (PeeringDbNetIxlan nix : ixEntries) {
                MetroCode metro = ixMapping.metroForIx(nix.getIxId());
                if (metro == null) continue;

                ixMetros.add(metro);

                PeeringDbIx ix = peeringDb.getEquinixIx(nix.getIxId());
                String ixName = ix != null ? ix.getName() : "IX-" + nix.getIxId();

                IxPresenceDetail detail = IxPresenceDetail.builder()
                        .metro(metro)
                        .ixId(nix.getIxId())
                        .ixName(ixName)
                        .speedMbps(nix.getSpeed())
                        .ipv4Address(nix.getIpaddr4())
                        .ipv6Address(nix.getIpaddr6())
                        .routeServerPeer(nix.isRsPeer())
                        .bfdSupport(nix.isBfdSupport())
                        .operational(nix.isOperational())
                        .build();
                ixDetails.add(detail);

                if (nix.isRsPeer()) anyRouteServer = true;
                if (nix.isBfdSupport()) anyBfd = true;
                if (nix.getIpaddr6() != null && !nix.getIpaddr6().isEmpty()) anyIpv6 = true;
                totalCapacity += nix.getSpeed();
            }

            // Facility metros
            Set<MetroCode> facMetros = new LinkedHashSet<>();
            for (PeeringDbNetFac nf : facEntries) {
                MetroCode metro = ixMapping.metroForFacility(nf.getFacId());
                if (metro != null) facMetros.add(metro);
            }

            // All metros
            Set<MetroCode> allMetros = new LinkedHashSet<>();
            allMetros.addAll(ixMetros);
            allMetros.addAll(facMetros);

            NetworkPresence presence = NetworkPresence.builder()
                    .asn(asn)
                    .label(label)
                    .peeringDbName(net != null ? net.getName() : null)
                    .peeringPolicy(net != null ? PeeringPolicy.fromPeeringDb(net.getPolicyGeneral()) : PeeringPolicy.UNKNOWN)
                    .networkType(net != null ? NetworkType.fromPeeringDb(net.getInfoType()) : NetworkType.UNKNOWN)
                    .trafficVolume(net != null ? net.getInfoTraffic() : null)
                    .trafficRatio(net != null ? net.getInfoRatio() : null)
                    .routeServerParticipant(anyRouteServer)
                    .bfdSupported(anyBfd)
                    .ipv6Capable(anyIpv6 || (net != null && net.isInfoIpv6()))
                    .ixPeeringMetros(ixMetros)
                    .facilityMetros(facMetros)
                    .allMetros(allMetros)
                    .ixDetails(ixDetails)
                    .totalIxCapacityMbps(totalCapacity)
                    .build();

            networkPresences.put(asn, presence);
        }
    }

    // ---- Phase 4: Build PresenceMatrix ----

    private PresenceMatrix buildPresenceMatrix() {
        // Determine columns: all metros that have any presence
        Set<MetroCode> allMetros = new TreeSet<>(Comparator.comparing(Enum::name));
        for (NetworkPresence np : networkPresences.values()) {
            allMetros.addAll(np.getAllMetros());
        }

        // If customer metros specified, ensure they're included
        allMetros.addAll(request.getCustomerMetros());

        List<Long> asns = new ArrayList<>(request.getTargetAsns().keySet());
        List<MetroCode> metros = new ArrayList<>(allMetros);

        Map<Long, Map<MetroCode, PresenceCell>> cells = new LinkedHashMap<>();

        for (Long asn : asns) {
            Map<MetroCode, PresenceCell> row = new LinkedHashMap<>();
            NetworkPresence np = networkPresences.get(asn);

            for (MetroCode metro : metros) {
                boolean ixPresent = np != null && np.hasIxPeeringAt(metro);
                boolean facPresent = np != null && np.hasFacilityAt(metro);

                // Collect IX details for this metro
                List<IxPresenceDetail> metroIxSessions = np != null
                        ? np.getIxDetails().stream()
                            .filter(d -> d.getMetro() == metro)
                            .collect(Collectors.toList())
                        : Collections.emptyList();

                int totalCapacity = metroIxSessions.stream().mapToInt(IxPresenceDetail::getSpeedMbps).sum();
                boolean anyRs = metroIxSessions.stream().anyMatch(IxPresenceDetail::isRouteServerPeer);
                boolean anyBfd = metroIxSessions.stream().anyMatch(IxPresenceDetail::isBfdSupport);

                ConnectivityType connType = ConnectivityType.resolve(ixPresent, false, facPresent);

                PresenceCell cell = PresenceCell.builder()
                        .asn(asn)
                        .metro(metro)
                        .connectivityType(connType)
                        .ixPresent(ixPresent)
                        .facilityPresent(facPresent)
                        .fabricAvailable(false)
                        .ixSessionCount(metroIxSessions.size())
                        .totalIxCapacityMbps(totalCapacity)
                        .routeServerPeer(anyRs)
                        .bfdSupported(anyBfd)
                        .ixSessions(metroIxSessions)
                        .build();

                row.put(metro, cell);
            }
            cells.put(asn, row);
        }

        Map<Long, String> labels = new LinkedHashMap<>();
        for (Map.Entry<Long, String> entry : request.getTargetAsns().entrySet()) {
            String label = entry.getValue();
            if (label == null) {
                NetworkPresence np = networkPresences.get(entry.getKey());
                label = np != null ? np.getLabel() : "AS" + entry.getKey();
            }
            labels.put(entry.getKey(), label);
        }

        return PresenceMatrix.builder()
                .asns(asns)
                .asnLabels(labels)
                .metros(metros)
                .cells(cells)
                .build();
    }

    // ---- Phase 5: Build MetroPresenceReports ----

    private Map<MetroCode, MetroPresenceReport> buildMetroReports(PresenceMatrix matrix) {
        Map<MetroCode, MetroPresenceReport> reports = new LinkedHashMap<>();

        for (MetroCode metro : matrix.getMetros()) {
            List<PresenceCell> cells = new ArrayList<>();
            for (Long asn : matrix.getAsns()) {
                PresenceCell cell = matrix.get(asn, metro);
                if (cell != null && cell.getConnectivityType() != ConnectivityType.NONE) {
                    cells.add(cell);
                }
            }

            PeeringDbIx sampleIx = null;
            List<Integer> ixIds = ixMapping.ixIdsForMetro(metro);
            if (!ixIds.isEmpty()) {
                sampleIx = peeringDb.getEquinixIx(ixIds.get(0));
            }

            String metroName = sampleIx != null ? sampleIx.getCity() : metro.name();

            reports.put(metro, MetroPresenceReport.builder()
                    .metro(metro)
                    .metroName(metroName)
                    .ixCount(ixIds.size())
                    .facilityCount(ixMapping.facIdsForMetro(metro).size())
                    .asnPresence(cells)
                    .build());
        }

        return reports;
    }

    // ---- Phase 6: Resiliency Analysis ----

    private ResiliencyAssessment buildResiliencyAssessment(PresenceMatrix matrix) {
        int totalAsns = request.getTargetAsns().size();
        List<BlastRadiusReport> blastReports = new ArrayList<>();
        Map<MetroCode, List<FailoverPath>> failoverPaths = new LinkedHashMap<>();
        List<CorrelatedFailure> correlatedFailures = new ArrayList<>();
        List<DiversityScore> diversityScores = new ArrayList<>();
        List<String> findings = new ArrayList<>();

        // Blast radius per customer metro
        for (MetroCode customerMetro : request.getCustomerMetros()) {
            List<Long> lostIxAsns = new ArrayList<>();
            List<String> lostIxLabels = new ArrayList<>();
            long lostCapacity = 0;

            for (Long asn : matrix.getAsns()) {
                PresenceCell cell = matrix.get(asn, customerMetro);
                if (cell != null && cell.isIxPresent()) {
                    lostIxAsns.add(asn);
                    lostIxLabels.add(request.getTargetAsns().getOrDefault(asn, "AS" + asn));
                    lostCapacity += cell.getTotalIxCapacityMbps();
                }
            }

            double impactRatio = totalAsns > 0 ? (double) lostIxAsns.size() / totalAsns : 0;
            String severity = impactRatio > 0.8 ? "CRITICAL" : impactRatio > 0.5 ? "HIGH" :
                    impactRatio > 0.25 ? "MEDIUM" : "LOW";

            List<String> mitigations = new ArrayList<>();
            if (impactRatio > 0.5) {
                mitigations.add("Establish IX peering at geographically diverse metros");
                mitigations.add("Consider Fabric private connections as independent backup paths");
            }
            if (impactRatio > 0.8) {
                mitigations.add("CRITICAL: " + customerMetro + " is a single point of failure for " +
                        lostIxAsns.size() + " of " + totalAsns + " target ASNs");
            }

            blastReports.add(BlastRadiusReport.builder()
                    .metro(customerMetro)
                    .scope(FailureScope.METRO)
                    .lostIxPeeringAsns(lostIxAsns)
                    .lostFabricAsns(Collections.emptyList())
                    .lostIxPeeringLabels(lostIxLabels)
                    .lostFabricLabels(Collections.emptyList())
                    .lostIxCapacityMbps(lostCapacity)
                    .impactRatio(impactRatio)
                    .severity(severity)
                    .mitigations(mitigations)
                    .build());

            // Failover paths for ASNs at this metro
            List<FailoverPath> metroFailovers = new ArrayList<>();
            for (Long asn : lostIxAsns) {
                NetworkPresence np = networkPresences.get(asn);
                if (np == null) continue;

                for (MetroCode altMetro : np.getIxPeeringMetros()) {
                    if (altMetro == customerMetro) continue;

                    PresenceCell altCell = matrix.get(asn, altMetro);
                    if (altCell == null || !altCell.isIxPresent()) continue;

                    DiversityScore diversity = computeDiversity(customerMetro, altMetro);

                    metroFailovers.add(FailoverPath.builder()
                            .targetAsn(asn)
                            .targetLabel(np.getLabel())
                            .primaryMetro(customerMetro)
                            .failoverMetro(altMetro)
                            .connectivityType(altCell.getConnectivityType())
                            .ixCapacityMbps(altCell.getTotalIxCapacityMbps())
                            .routeServerAvailable(altCell.isRouteServerPeer())
                            .diversity(diversity)
                            .ixSessions(altCell.getIxSessions())
                            .recommendation(buildFailoverRecommendation(np, altMetro, altCell, diversity))
                            .build());
                }
            }
            failoverPaths.put(customerMetro, metroFailovers);
        }

        // Correlated failures: ASNs that only exist at one customer metro
        for (Long asn : matrix.getAsns()) {
            NetworkPresence np = networkPresences.get(asn);
            if (np == null) continue;

            List<MetroCode> customerMetrosWithAsn = request.getCustomerMetros().stream()
                    .filter(np::hasIxPeeringAt)
                    .collect(Collectors.toList());

            if (customerMetrosWithAsn.size() == 1) {
                MetroCode singleMetro = customerMetrosWithAsn.get(0);
                correlatedFailures.add(CorrelatedFailure.builder()
                        .scope(FailureScope.METRO)
                        .failureDomain(singleMetro.name() + " metro")
                        .affectedMetro(singleMetro)
                        .affectedAsns(Collections.singletonList(asn))
                        .affectedLabels(Collections.singletonList(np.getLabel()))
                        .affectedPaths(Collections.singletonList("IX Peering to " + np.getLabel()))
                        .impactRatio(1.0 / totalAsns)
                        .severity(totalAsns <= 2 ? "HIGH" : "MEDIUM")
                        .recommendation("Establish IX peering with " + np.getLabel() +
                                " at a second metro for geographic redundancy")
                        .build());
            }
        }

        // Diversity scores between customer metro pairs
        List<MetroCode> customerMetroList = new ArrayList<>(request.getCustomerMetros());
        for (int i = 0; i < customerMetroList.size(); i++) {
            for (int j = i + 1; j < customerMetroList.size(); j++) {
                diversityScores.add(computeDiversity(customerMetroList.get(i), customerMetroList.get(j)));
            }
        }

        // Compute overall score
        double resiliencyScore = computeOverallResiliency(blastReports, correlatedFailures, diversityScores);
        String rating = resiliencyScore >= 0.8 ? "Excellent" : resiliencyScore >= 0.6 ? "Good" :
                resiliencyScore >= 0.4 ? "Moderate" : resiliencyScore >= 0.2 ? "Poor" : "Critical";

        // Generate findings
        if (request.getCustomerMetros().size() == 1) {
            findings.add("Single customer metro detected — no geographic redundancy for IX peering.");
        }
        for (BlastRadiusReport br : blastReports) {
            if (br.getImpactRatio() > 0.5) {
                findings.add("Metro " + br.getMetro() + " failure would impact " +
                        String.format("%.0f%%", br.getImpactRatio() * 100) + " of analyzed ASN connectivity.");
            }
        }
        if (correlatedFailures.stream().anyMatch(cf -> "CRITICAL".equals(cf.getSeverity()))) {
            findings.add("Critical correlated failure detected — some ASNs have no IX failover path.");
        }

        return ResiliencyAssessment.builder()
                .overallScore(resiliencyScore)
                .overallRating(rating)
                .failoverPaths(failoverPaths)
                .blastRadiusReports(blastReports)
                .correlatedFailures(correlatedFailures)
                .diversityScores(diversityScores)
                .findings(findings)
                .build();
    }

    // ---- Phase 7: Peering Opportunity Discovery ----

    private List<PeeringOpportunity> discoverPeeringOpportunities() throws IOException {
        List<PeeringOpportunity> opportunities = new ArrayList<>();

        // Get customer's IX presence at Equinix
        List<PeeringDbNetIxlan> customerIxPresence = peeringDb.getEquinixIxPresence(request.getCustomerAsn());

        // Build set of IX IDs where customer is present
        Set<Integer> customerIxIds = customerIxPresence.stream()
                .map(PeeringDbNetIxlan::getIxId)
                .collect(Collectors.toSet());

        // For each target ASN, find shared IXes
        for (Map.Entry<Long, String> entry : request.getTargetAsns().entrySet()) {
            long targetAsn = entry.getKey();
            String targetLabel = entry.getValue();
            NetworkPresence np = networkPresences.get(targetAsn);
            if (np == null) continue;

            if (targetLabel == null) targetLabel = np.getLabel();

            for (IxPresenceDetail ixDetail : np.getIxDetails()) {
                if (customerIxIds.contains(ixDetail.getIxId())) {
                    // Both present at this IX — peering opportunity!
                    double feasibility = np.getPeeringPolicy().getFeasibilityScore();
                    if (ixDetail.isRouteServerPeer()) feasibility = Math.min(1.0, feasibility + 0.3);

                    String complexity;
                    if (ixDetail.isRouteServerPeer() && np.getPeeringPolicy() == PeeringPolicy.OPEN) {
                        complexity = "Automatic";
                        feasibility = 1.0;
                    } else if (np.getPeeringPolicy() == PeeringPolicy.OPEN) {
                        complexity = "Simple";
                    } else if (np.getPeeringPolicy() == PeeringPolicy.SELECTIVE) {
                        complexity = "Negotiation Required";
                    } else {
                        complexity = "Difficult";
                    }

                    opportunities.add(PeeringOpportunity.builder()
                            .customerAsn(request.getCustomerAsn())
                            .targetAsn(targetAsn)
                            .targetLabel(targetLabel)
                            .metro(ixDetail.getMetro())
                            .ixName(ixDetail.getIxName())
                            .ixId(ixDetail.getIxId())
                            .targetPolicy(np.getPeeringPolicy())
                            .targetUsesRouteServer(ixDetail.isRouteServerPeer())
                            .targetSpeedMbps(ixDetail.getSpeedMbps())
                            .feasibility(feasibility)
                            .complexity(complexity)
                            .recommendation(buildPeeringRecommendation(np, ixDetail, complexity))
                            .build());
                }
            }
        }

        // Sort by feasibility descending
        opportunities.sort(Comparator.comparingDouble(PeeringOpportunity::getFeasibility).reversed());
        return opportunities;
    }

    // ---- Phase 8: Unified Connectivity Views ----

    private Map<Long, UnifiedConnectivityView> buildUnifiedViews(PresenceMatrix matrix) {
        Map<Long, UnifiedConnectivityView> views = new LinkedHashMap<>();

        for (Long asn : matrix.getAsns()) {
            NetworkPresence np = networkPresences.get(asn);
            if (np == null) continue;

            List<UnifiedConnectivityView.MetroConnectivity> metroConns = new ArrayList<>();
            long totalCapacity = 0;
            boolean anyFabric = false;

            for (MetroCode metro : matrix.getMetros()) {
                PresenceCell cell = matrix.get(asn, metro);
                if (cell == null || cell.getConnectivityType() == ConnectivityType.NONE) continue;

                metroConns.add(UnifiedConnectivityView.MetroConnectivity.builder()
                        .metro(metro)
                        .connectivityType(cell.getConnectivityType())
                        .hasIxPeering(cell.isIxPresent())
                        .hasFabric(cell.isFabricAvailable())
                        .ixCapacityMbps(cell.getTotalIxCapacityMbps())
                        .routeServerAvailable(cell.isRouteServerPeer())
                        .bfdAvailable(cell.isBfdSupported())
                        .ixSessions(cell.getIxSessions())
                        .fabricServiceProfileUuid(null)
                        .build());

                totalCapacity += cell.getTotalIxCapacityMbps();
                if (cell.isFabricAvailable()) anyFabric = true;
            }

            views.put(asn, UnifiedConnectivityView.builder()
                    .asn(asn)
                    .label(np.getLabel())
                    .metroConnectivity(metroConns)
                    .reachableMetroCount(metroConns.size())
                    .totalIxCapacityMbps(totalCapacity)
                    .fabricAvailableAnywhere(anyFabric)
                    .build());
        }

        return views;
    }

    // ---- Helpers ----

    private DiversityScore computeDiversity(MetroCode metro1, MetroCode metro2) {
        // Use IX city coordinates from the mapping for distance calculation. The
        // metro → coordinate lookup is precomputed once per analysis (see
        // metroCoordinatesMap()) rather than rescanning the full facility map on
        // each of the O(metros^2) diversity calls.
        Map<MetroCode, double[]> coords = metroCoordinatesMap();
        double[] c1 = coords.get(metro1);
        double[] c2 = coords.get(metro2);
        boolean found1 = c1 != null;
        boolean found2 = c2 != null;

        double distance = (found1 && found2)
                ? haversineKm(c1[0], c1[1], c2[0], c2[1])
                : 0;
        DiversityRating rating = DiversityRating.fromDistance(distance);

        // Check if same region (basic check via metro naming conventions)
        boolean sameRegion = isSameRegion(metro1, metro2);

        String explanation;
        if (rating == DiversityRating.EXCELLENT) {
            explanation = metro1 + " and " + metro2 + " are " + (int) distance +
                    " km apart — excellent geographic diversity across regions.";
        } else if (rating == DiversityRating.GOOD) {
            explanation = metro1 + " and " + metro2 + " are " + (int) distance +
                    " km apart — good diversity within the same continent.";
        } else if (rating == DiversityRating.MODERATE) {
            explanation = metro1 + " and " + metro2 + " are " + (int) distance +
                    " km apart — moderate diversity; some shared infrastructure risk.";
        } else {
            explanation = metro1 + " and " + metro2 + " are only " + (int) distance +
                    " km apart — limited geographic diversity; consider a more distant failover.";
        }

        return DiversityScore.builder()
                .primaryMetro(metro1)
                .backupMetro(metro2)
                .distanceKm(distance)
                .sameRegion(sameRegion)
                .rating(rating)
                .explanation(explanation)
                .build();
    }

    /**
     * Loads metro coordinates and regions from the live Fabric Metros API into {@link #metroCoordinates}
     * and {@link #metroRegion}. These drive the IX/facility-to-metro bridge and the geographic
     * diversity scoring. Best-effort: if Fabric is unavailable the maps stay empty and the bridge
     * degrades to whatever can be resolved, rather than failing the analysis. Well-known metros only
     * ({@link MetroCode#UNKNOWN} is skipped).
     */
    private void loadMetroGeo() {
        try {
            for (Metro metro : fabric.metros().list().loadAll()) {
                MetroCode code = metro.getCode();
                if (code == MetroCode.UNKNOWN) {
                    continue;
                }
                GeoCoordinate geo = metro.geoCoordinates();
                if (geo != null && geo.getLatitude() != null && geo.getLongitude() != null) {
                    metroCoordinates.put(code, new double[]{geo.getLatitude(), geo.getLongitude()});
                }
                if (metro.getRegion() != null) {
                    metroRegion.put(code, metro.getRegion().name());
                }
            }
        }
        catch (RuntimeException e) {
            // Fabric metros unavailable; proceed with whatever is already loaded.
        }
    }

    /**
     * @return the live metro → [latitude, longitude] lookup used by {@link #computeDiversity}
     */
    private Map<MetroCode, double[]> metroCoordinatesMap() {
        return metroCoordinates;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private boolean isSameRegion(MetroCode m1, MetroCode m2) {
        return getRegionForMetro(m1).equals(getRegionForMetro(m2));
    }

    private String getRegionForMetro(MetroCode metro) {
        // Live Fabric region (AMER/EMEA/APAC), loaded in loadMetroGeo(); the geographic-distance
        // measure in computeDiversity() — not this coarse bucket — is the primary diversity signal.
        return metroRegion.getOrDefault(metro, "UNKNOWN");
    }

    private double computeOverallResiliency(List<BlastRadiusReport> blastReports,
                                            List<CorrelatedFailure> correlations,
                                            List<DiversityScore> diversity) {
        if (blastReports.isEmpty()) return 0.5;

        // Factor 1: Average blast radius impact (lower is better)
        double avgImpact = blastReports.stream()
                .mapToDouble(BlastRadiusReport::getImpactRatio)
                .average().orElse(0.5);
        double blastScore = 1.0 - avgImpact;

        // Factor 2: Correlated failure penalty
        long criticalCorrelations = correlations.stream()
                .filter(cf -> "CRITICAL".equals(cf.getSeverity()) || "HIGH".equals(cf.getSeverity()))
                .count();
        double correlationPenalty = Math.min(0.3, criticalCorrelations * 0.1);

        // Factor 3: Diversity bonus
        double avgDiversity = diversity.stream()
                .mapToDouble(ds -> ds.getRating().getScore())
                .average().orElse(0.5);

        // Weighted combination
        double score = (blastScore * 0.5) + (avgDiversity * 0.3) - correlationPenalty + 0.2;
        return Math.max(0.0, Math.min(1.0, score));
    }

    private String buildFailoverRecommendation(NetworkPresence np, MetroCode altMetro,
                                                PresenceCell altCell, DiversityScore diversity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Establish IX peering with ").append(np.getLabel()).append(" at ").append(altMetro);

        if (altCell.isRouteServerPeer()) {
            sb.append(" via route server (automatic peering)");
        } else {
            sb.append(" via bilateral BGP session");
        }

        sb.append(". Capacity: ").append(altCell.getTotalIxCapacityMbps() / 1000).append("G");
        sb.append(". Geographic diversity: ").append(diversity.getRating().getDisplayName());
        sb.append(".");
        return sb.toString();
    }

    private String buildPeeringRecommendation(NetworkPresence np, IxPresenceDetail ix, String complexity) {
        StringBuilder sb = new StringBuilder();
        if ("Automatic".equals(complexity)) {
            sb.append("Both networks are at ").append(ix.getIxName());
            sb.append(" and ").append(np.getLabel()).append(" uses route servers — ");
            sb.append("peering can be established automatically by connecting to the MLPE route server.");
        } else if ("Simple".equals(complexity)) {
            sb.append("Both networks are at ").append(ix.getIxName());
            sb.append(". ").append(np.getLabel()).append(" has an Open peering policy — ");
            sb.append("configure a bilateral BGP session to establish peering.");
        } else {
            sb.append("Both networks are at ").append(ix.getIxName());
            sb.append(". ").append(np.getLabel()).append(" has a ").append(np.getPeeringPolicy().getDisplayName());
            sb.append(" peering policy — contact their peering team to negotiate terms.");
        }
        return sb.toString();
    }
}
