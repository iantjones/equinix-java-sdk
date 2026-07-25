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
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.geo.SpeedOfLightLatency;
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
 *   <li>Build IX ID → MetroId mapping</li>
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

    /** Sentinel for a metro whose Fabric region was not loaded — an absence, never a real region. */
    private static final String UNKNOWN_REGION = "UNKNOWN";

    private final FabricGateway fabric;
    private final PeeringDbClient peeringDb;
    private final PeeringRequest request;

    /**
     * Non-fatal data-completeness notes accumulated across the pipeline (a data source that could not be
     * loaded, records excluded because they would not resolve, an optional phase skipped after a
     * recoverable failure). Surfaced on {@link PeeringIntelligenceResult#warnings()} so a partial result
     * is never silently presented as complete.
     */
    private final List<String> warnings = new ArrayList<>();

    private EquinixIXMapping ixMapping;
    private final Map<Long, PeeringDbNetwork> networkMetadata = new LinkedHashMap<>();
    private final Map<Long, List<PeeringDbNetIxlan>> ixPresenceByAsn = new LinkedHashMap<>();
    private final Map<Long, List<PeeringDbNetFac>> facPresenceByAsn = new LinkedHashMap<>();
    private final Map<Long, NetworkPresence> networkPresences = new LinkedHashMap<>();

    /**
     * Live Fabric metro geo data, loaded once per analysis from {@code fabric.metros()}: metro →
     * [latitude, longitude] (used to bind PeeringDB facilities/IXes to metros and to compute
     * geographic diversity) and metro → region (used for the same-region diversity descriptor).
     * Keyed by {@link MetroId}, so metros that are live in Fabric but not yet in the
     * {@code MetroCode} enum are still included and stay distinct.
     */
    private final Map<MetroId, double[]> metroCoordinates = new LinkedHashMap<>();
    private final Map<MetroId, String> metroRegion = new LinkedHashMap<>();
    private final Map<String, MetroId> ibxToMetro = new LinkedHashMap<>();

    /**
     * Per-metro representative coordinate derived from the actual Equinix IBX data centers in that
     * metro — the centroid of the PeeringDB Equinix facility coordinates resolved to the metro. This
     * grounds the geographic-diversity distance in real data-center positions (IBX-to-IBX) rather than
     * the single Fabric metro centroid, and is preferred over {@link #metroCoordinates} when present.
     */
    private final Map<MetroId, double[]> metroFacilityCoordinates = new LinkedHashMap<>();

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
            // Phase 1: Load the Equinix catalog (PeeringDB) and live metro data (Fabric), then build
            // the IX/facility -> metro bridge from the live IBX->metro map — facilities first (each
            // PeeringDB name carries its IBX code, e.g. "LA4", and seeds the city bridge), then IXes
            // (city-only, resolved via that bridge).
            peeringDb.loadEquinixCatalog();
            loadMetroGeo();
            if (!metroCoordinates.isEmpty()) {
                dataSources.add("Equinix Fabric");
            }
            ixMapping = new EquinixIXMapping(ibxToMetro);
            ixMapping.mapFacilities(peeringDb.getEquinixFacMap());
            ixMapping.mapIxes(peeringDb.getEquinixIxMap());
            loadFacilityCoordinates();

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
            Map<MetroId, MetroPresenceReport> metroReports = buildMetroReports(matrix);

            // Phase 6: Resiliency analysis (if requested)
            ResiliencyAssessment resiliency = null;
            if (request.isIncludeResiliency() && !request.getCustomerMetros().isEmpty()) {
                resiliency = buildResiliencyAssessment(matrix);
            }

            // Phase 7: Peering opportunity discovery (if customer ASN provided). This is an OPTIONAL
            // enrichment phase: a failure here (e.g. the extra PeeringDB lookup for the customer ASN)
            // must not abort the core analysis, so it is caught and surfaced as a skipped-phase note
            // rather than propagating out of execute().
            List<PeeringOpportunity> opportunities = Collections.emptyList();
            if (request.getCustomerAsn() > 0) {
                try {
                    opportunities = discoverPeeringOpportunities();
                } catch (IOException | RuntimeException e) {
                    warnings.add("Peering-opportunity discovery was skipped: the PeeringDB lookup for your "
                            + "ASN " + request.getCustomerAsn() + " failed (" + describe(e) + "). The rest "
                            + "of the analysis is unaffected; peering_opportunities is empty for this run.");
                }
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
                    .warnings(new ArrayList<>(warnings))
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("PeeringDB API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts a short, human-readable reason from a failure for a warning message, falling back to the
     * exception's simple class name when it carries no message.
     */
    private static String describe(Throwable t) {
        String message = t.getMessage();
        return (message != null && !message.isBlank()) ? message : t.getClass().getSimpleName();
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
        boolean includeCapacity = request.isIncludeCapacity();
        boolean includePolicies = request.isIncludePolicies();
        // Count Equinix IX/facility presence records that could not be bound to a metro. These are
        // dropped from the presence matrix (there is no column to place them in); tracking the count lets
        // the analysis honestly report that the matrix may understate presence rather than silently
        // hiding the gap.
        int droppedIxRecords = 0;
        int droppedFacRecords = 0;

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
            Set<MetroId> ixMetros = new LinkedHashSet<>();
            List<IxPresenceDetail> ixDetails = new ArrayList<>();
            boolean anyRouteServer = false;
            boolean anyBfd = false;
            boolean anyIpv6 = false;
            long totalCapacity = 0;

            for (PeeringDbNetIxlan nix : ixEntries) {
                MetroId metro = ixMapping.metroForIx(nix.getIxId());
                if (metro == null) {
                    // An Equinix IX (already filtered to Equinix by the client) whose IX could not be
                    // resolved to a metro — exclude it, but count it so the gap is surfaced.
                    droppedIxRecords++;
                    continue;
                }

                ixMetros.add(metro);

                PeeringDbIx ix = peeringDb.getEquinixIx(nix.getIxId());
                String ixName = ix != null ? ix.getName() : "IX-" + nix.getIxId();

                // Capacity is collected only when capacity analysis is enabled; when disabled the speed
                // is left at 0 and a warning marks the figures as "not analyzed" (never a real 0 Gbps).
                int speedMbps = includeCapacity ? nix.getSpeed() : 0;

                IxPresenceDetail detail = IxPresenceDetail.builder()
                        .metro(metro)
                        .ixId(nix.getIxId())
                        .ixName(ixName)
                        .speedMbps(speedMbps)
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
                totalCapacity += speedMbps;
            }

            // Facility metros
            Set<MetroId> facMetros = new LinkedHashSet<>();
            for (PeeringDbNetFac nf : facEntries) {
                MetroId metro = ixMapping.metroForFacility(nf.getFacId());
                if (metro != null) {
                    facMetros.add(metro);
                } else {
                    droppedFacRecords++;
                }
            }

            // All metros
            Set<MetroId> allMetros = new LinkedHashSet<>();
            allMetros.addAll(ixMetros);
            allMetros.addAll(facMetros);

            NetworkPresence presence = NetworkPresence.builder()
                    .asn(asn)
                    .label(label)
                    .peeringDbName(net != null ? net.getName() : null)
                    .peeringPolicy((includePolicies && net != null)
                            ? PeeringPolicy.fromPeeringDb(net.getPolicyGeneral()) : PeeringPolicy.UNKNOWN)
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

        // Surface any presence records that were excluded, and any capacity/policy analysis the caller
        // turned off, so the presence matrix is never shown as complete when it is not.
        if (droppedIxRecords > 0 || droppedFacRecords > 0) {
            warnings.add(droppedIxRecords + " Equinix IX session(s) and " + droppedFacRecords
                    + " facility record(s) could not be resolved to an Equinix metro and were excluded "
                    + "from the presence matrix; presence may be understated for the affected networks.");
        }
        if (!includeCapacity) {
            warnings.add("IX port-capacity analysis was disabled (includeCapacity=false); all capacity "
                    + "figures in this result are 0 and must be read as 'not analyzed', not as zero capacity.");
        }
        if (!includePolicies) {
            warnings.add("Peering-policy analysis was disabled (includePolicies=false); peering policies "
                    + "are reported as UNKNOWN and peering-opportunity feasibility is not policy-based.");
        }
    }

    // ---- Phase 4: Build PresenceMatrix ----

    private PresenceMatrix buildPresenceMatrix() {
        // Determine columns: all metros that have any presence
        Set<MetroId> allMetros = new TreeSet<>(Comparator.comparing(MetroId::code));
        for (NetworkPresence np : networkPresences.values()) {
            allMetros.addAll(np.getAllMetros());
        }

        // If customer metros specified, ensure they're included
        allMetros.addAll(request.getCustomerMetros());

        List<Long> asns = new ArrayList<>(request.getTargetAsns().keySet());
        List<MetroId> metros = new ArrayList<>(allMetros);

        Map<Long, Map<MetroId, PresenceCell>> cells = new LinkedHashMap<>();

        for (Long asn : asns) {
            Map<MetroId, PresenceCell> row = new LinkedHashMap<>();
            NetworkPresence np = networkPresences.get(asn);

            for (MetroId metro : metros) {
                boolean ixPresent = np != null && np.hasIxPeeringAt(metro);
                boolean facPresent = np != null && np.hasFacilityAt(metro);

                // Collect IX details for this metro
                List<IxPresenceDetail> metroIxSessions = np != null
                        ? np.getIxDetails().stream()
                            .filter(d -> metro.equals(d.getMetro()))
                            .collect(Collectors.toList())
                        : Collections.emptyList();

                // Sum as long: a metro can aggregate many high-speed sessions, and an int sum could
                // overflow (each PresenceCell.totalIxCapacityMbps is a long for the same reason).
                long totalCapacity = metroIxSessions.stream().mapToLong(IxPresenceDetail::getSpeedMbps).sum();
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

    private Map<MetroId, MetroPresenceReport> buildMetroReports(PresenceMatrix matrix) {
        Map<MetroId, MetroPresenceReport> reports = new LinkedHashMap<>();

        for (MetroId metro : matrix.getMetros()) {
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

            String metroName = sampleIx != null ? sampleIx.getCity() : metro.code();

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
        Map<MetroId, List<FailoverPath>> failoverPaths = new LinkedHashMap<>();
        List<CorrelatedFailure> correlatedFailures = new ArrayList<>();
        List<DiversityScore> diversityScores = new ArrayList<>();
        List<String> findings = new ArrayList<>();

        // Blast radius per customer metro
        for (MetroId customerMetro : request.getCustomerMetros()) {
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

                for (MetroId altMetro : np.getIxPeeringMetros()) {
                    if (altMetro.equals(customerMetro)) continue;

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

            List<MetroId> customerMetrosWithAsn = request.getCustomerMetros().stream()
                    .filter(np::hasIxPeeringAt)
                    .collect(Collectors.toList());

            if (customerMetrosWithAsn.size() == 1) {
                MetroId singleMetro = customerMetrosWithAsn.get(0);
                correlatedFailures.add(CorrelatedFailure.builder()
                        .scope(FailureScope.METRO)
                        .failureDomain(singleMetro.code() + " metro")
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
        List<MetroId> customerMetroList = new ArrayList<>(request.getCustomerMetros());
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
        // Correlated failures here are all single-customer-metro cases (an ASN that peers at only ONE
        // of the customer's metros), created with HIGH/MEDIUM severity — never CRITICAL. The old finding
        // gated on "CRITICAL" so it could never fire, and its "no IX failover path" wording was wrong
        // (the ASN may still peer at non-customer metros). Report the real, reachable condition instead.
        long singleMetroAsns = correlatedFailures.stream()
                .filter(cf -> cf.getScope() == FailureScope.METRO)
                .count();
        if (singleMetroAsns > 0) {
            findings.add(singleMetroAsns + " analyzed network(s) have IX peering at only one of your "
                    + "metros — a failure of that metro removes IX peering to them from your footprint. "
                    + "Establish IX peering at a second metro for these networks.");
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

            for (MetroId metro : matrix.getMetros()) {
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

    private DiversityScore computeDiversity(MetroId metro1, MetroId metro2) {
        // Distance is IBX-grounded: each metro is represented by the centroid of its actual Equinix
        // IBX data centers (resolveCoordinates), falling back to the Fabric metro centroid only when
        // no facility coordinates are available. Both lookups are precomputed once per analysis.
        double[] c1 = resolveCoordinates(metro1);
        double[] c2 = resolveCoordinates(metro2);

        // Same-region is a coarse, independent signal (region buckets, not coordinates); an UNKNOWN
        // region for either metro is honestly reported as "not the same region" rather than matched.
        boolean sameRegion = isSameRegion(metro1, metro2);

        // If either metro has no known coordinate, the distance is genuinely UNAVAILABLE. Do NOT
        // fabricate 0 km — that would misread as a CRITICAL "same-site" pairing, invent a "0 km apart"
        // narrative, and drag the resiliency score toward 0. Model it as absent instead: NaN distance,
        // an UNKNOWN rating (excluded from the score), and an explanation that names the missing metros.
        if (c1 == null || c2 == null) {
            List<String> missing = new ArrayList<>();
            if (c1 == null) missing.add(metro1.code());
            if (c2 == null) missing.add(metro2.code());
            String explanation = "Geographic distance between " + metro1 + " and " + metro2
                    + " is unavailable — no Equinix facility or Fabric coordinates for "
                    + String.join(" and ", missing) + "; diversity could not be assessed for this pair.";
            return DiversityScore.builder()
                    .primaryMetro(metro1)
                    .backupMetro(metro2)
                    .distanceKm(Double.NaN)
                    .estimatedRttMs(Double.NaN)
                    .distanceUnavailable(true)
                    .sameRegion(sameRegion)
                    .rating(DiversityRating.UNKNOWN)
                    .explanation(explanation)
                    .build();
        }

        double distance = haversineKm(c1[0], c1[1], c2[0], c2[1]);
        DiversityRating rating = DiversityRating.fromDistance(distance);

        // Speed-of-light floor for the round-trip between the two metros (physical lower bound).
        double rttMs = SpeedOfLightLatency.roundTrip().millisForKm(distance);
        String rttHint = String.format(" (~%.1f ms RTT floor)", rttMs);

        String explanation;
        if (rating == DiversityRating.EXCELLENT) {
            explanation = metro1 + " and " + metro2 + " are " + (int) distance +
                    " km apart" + rttHint + " — excellent geographic diversity across regions.";
        } else if (rating == DiversityRating.GOOD) {
            explanation = metro1 + " and " + metro2 + " are " + (int) distance +
                    " km apart" + rttHint + " — good diversity within the same continent.";
        } else if (rating == DiversityRating.MODERATE) {
            explanation = metro1 + " and " + metro2 + " are " + (int) distance +
                    " km apart" + rttHint + " — moderate diversity; some shared infrastructure risk.";
        } else {
            explanation = metro1 + " and " + metro2 + " are only " + (int) distance +
                    " km apart" + rttHint + " — limited geographic diversity; consider a more distant failover.";
        }

        return DiversityScore.builder()
                .primaryMetro(metro1)
                .backupMetro(metro2)
                .distanceKm(distance)
                .estimatedRttMs(rttMs)
                .distanceUnavailable(false)
                .sameRegion(sameRegion)
                .rating(rating)
                .explanation(explanation)
                .build();
    }

    /**
     * Loads metro coordinates and regions from the live Fabric Metros API into {@link #metroCoordinates}
     * and {@link #metroRegion}. These drive the IX/facility-to-metro bridge and the geographic
     * diversity scoring. Best-effort: if Fabric is unavailable the maps stay empty and the bridge
     * degrades to whatever can be resolved, rather than failing the analysis. Keyed by
     * {@link MetroId}, so a metro that is live in Fabric but not yet in the {@code MetroCode} enum
     * still participates.
     */
    private void loadMetroGeo() {
        try {
            for (Metro metro : fabric.metros().list().loadAll()) {
                MetroId code = metro.metroId();
                if (code == null) {
                    continue;
                }
                GeoCoordinate geo = metro.geoCoordinates();
                if (geo != null && geo.getLatitude() != null && geo.getLongitude() != null) {
                    metroCoordinates.put(code, new double[]{geo.getLatitude(), geo.getLongitude()});
                }
                if (metro.getRegion() != null) {
                    metroRegion.put(code, metro.getRegion().name());
                }
                if (metro.getIbxs() != null) {
                    for (String ibx : metro.getIbxs()) {
                        if (ibx != null && !ibx.trim().isEmpty()) {
                            ibxToMetro.put(ibx.trim().toUpperCase(java.util.Locale.ROOT), code);
                        }
                    }
                }
            }
        }
        catch (RuntimeException e) {
            // Fabric metros unavailable: proceed with whatever loaded, but never swallow it silently —
            // geographic diversity and the IX/facility-to-metro bridge are degraded, so say so.
            warnings.add("Equinix Fabric metro data could not be fully loaded (" + describe(e) + "); "
                    + "geographic diversity scoring and IX/facility-to-metro binding are degraded for "
                    + "this run, and some presence may be unresolved.");
        }
    }

    /**
     * Builds {@link #metroFacilityCoordinates} — a per-metro representative coordinate from the actual
     * Equinix IBX data centers, computed as the centroid of the PeeringDB Equinix facility coordinates
     * resolved to each metro (via the IBX-name bridge). Requires {@link EquinixIXMapping#mapFacilities}
     * to have run. Best-effort: facilities without coordinates or without a resolvable metro are
     * skipped, and the diversity distance falls back to the Fabric metro centroid for any metro with
     * no facility coordinates.
     */
    private void loadFacilityCoordinates() {
        Map<Integer, PeeringDbFacility> facs = peeringDb.getEquinixFacMap();
        if (facs == null || ixMapping == null) {
            return;
        }
        Map<MetroId, double[]> sums = new LinkedHashMap<>();   // metro -> [sumLat, sumLon, count]
        for (Map.Entry<Integer, PeeringDbFacility> entry : facs.entrySet()) {
            PeeringDbFacility fac = entry.getValue();
            if (fac == null || fac.getLatitude() == null || fac.getLongitude() == null) {
                continue;
            }
            MetroId metro = ixMapping.metroForFacility(entry.getKey());
            if (metro == null) {
                continue;
            }
            double[] acc = sums.computeIfAbsent(metro, k -> new double[3]);
            acc[0] += fac.getLatitude();
            acc[1] += fac.getLongitude();
            acc[2] += 1;
        }
        for (Map.Entry<MetroId, double[]> entry : sums.entrySet()) {
            double[] acc = entry.getValue();
            if (acc[2] > 0) {
                metroFacilityCoordinates.put(entry.getKey(),
                        new double[]{acc[0] / acc[2], acc[1] / acc[2]});
            }
        }
    }

    /**
     * Resolves a metro's coordinate for distance: the IBX-grounded facility centroid when available,
     * otherwise the Fabric metro centroid, or {@code null} if neither is known.
     */
    private double[] resolveCoordinates(MetroId metro) {
        double[] facility = metroFacilityCoordinates.get(metro);
        return facility != null ? facility : metroCoordinates.get(metro);
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

    private boolean isSameRegion(MetroId m1, MetroId m2) {
        String r1 = getRegionForMetro(m1);
        String r2 = getRegionForMetro(m2);
        // "UNKNOWN" is a not-loaded sentinel, not a region: two metros with unknown regions are NOT
        // known to share a region, so never report them as same-region on the strength of the sentinel.
        if (UNKNOWN_REGION.equals(r1) || UNKNOWN_REGION.equals(r2)) {
            return false;
        }
        return r1.equals(r2);
    }

    private String getRegionForMetro(MetroId metro) {
        // Live Fabric region (AMER/EMEA/APAC), loaded in loadMetroGeo(); the geographic-distance
        // measure in computeDiversity() — not this coarse bucket — is the primary diversity signal.
        return metroRegion.getOrDefault(metro, UNKNOWN_REGION);
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

        // Factor 3: Diversity bonus. Pairs whose distance is unavailable are EXCLUDED — they carry no
        // real diversity signal, and folding their placeholder score in would drag the result toward 0
        // (a data gap must not masquerade as poor resiliency). If every pair is unavailable, fall back
        // to a neutral 0.5 rather than inventing a diversity verdict.
        double avgDiversity = diversity.stream()
                .filter(ds -> !ds.isDistanceUnavailable())
                .mapToDouble(ds -> ds.getRating().getScore())
                .average().orElse(0.5);

        // Weighted combination
        double score = (blastScore * 0.5) + (avgDiversity * 0.3) - correlationPenalty + 0.2;
        return Math.max(0.0, Math.min(1.0, score));
    }

    private String buildFailoverRecommendation(NetworkPresence np, MetroId altMetro,
                                                PresenceCell altCell, DiversityScore diversity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Establish IX peering with ").append(np.getLabel()).append(" at ").append(altMetro);

        if (altCell.isRouteServerPeer()) {
            sb.append(" via route server (automatic peering)");
        } else {
            sb.append(" via bilateral BGP session");
        }

        sb.append(". Capacity: ").append(formatCapacityGbps(altCell.getTotalIxCapacityMbps()));
        sb.append(". Geographic diversity: ");
        if (diversity.isDistanceUnavailable()) {
            sb.append("unavailable (metro distance unknown)");
        } else {
            sb.append(diversity.getRating().getDisplayName());
        }
        sb.append(".");
        return sb.toString();
    }

    /**
     * Formats an Mbps capacity as a Gbps string without the integer-division truncation that silently
     * dropped sub-Gbps capacity: whole Gbps render as {@code "10G"}, fractional Gbps keep one decimal
     * ({@code "0.5G"}), and any positive sub-Gbps figure shows Mbps rather than collapsing to {@code "0G"}.
     */
    private static String formatCapacityGbps(long mbps) {
        if (mbps > 0 && mbps < 1000) {
            return mbps + "M";
        }
        double gbps = mbps / 1000.0;
        return (gbps == Math.rint(gbps))
                ? String.format("%.0fG", gbps)
                : String.format("%.1fG", gbps);
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
