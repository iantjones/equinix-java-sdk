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
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
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
 *   <li>Cross-reference Fabric service profiles (best-effort; "not analyzed" on failure)</li>
 *   <li>Build PresenceMatrix (ASN × Metro grid)</li>
 *   <li>Build MetroPresenceReports (per metro)</li>
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
     * Per target ASN, the Equinix metros where a Fabric service profile matching that network is
     * published: metro → the matching profile's UUID (first in catalog order when several match).
     * Populated by {@code analyzeFabricAvailability()}; empty for an ASN with no matching profile,
     * and empty overall when the Fabric service-profile catalog could not be read (in which case a
     * warning marks Fabric availability as NOT analyzed rather than genuinely absent).
     */
    private final Map<Long, Map<MetroId, String>> fabricProfilesByAsn = new LinkedHashMap<>();

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

            // Phase 2: Query PeeringDB for all target ASNs (batched asn__in requests)
            queryTargetAsns();

            // Phase 3: Build NetworkPresence per ASN
            buildNetworkPresences();

            // Phase 3.5: Cross-reference Fabric service profiles for private-connectivity
            // availability (best-effort; a failure degrades to "not analyzed" with a warning).
            analyzeFabricAvailability();

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

    // ---- Phase 2: Target-ASN data collection (batched) ----

    private void queryTargetAsns() throws IOException {
        // ALL target ASNs are queried per endpoint in one batched call: the client uses
        // PeeringDB's asn__in query operator, collapsing the former one-request-per-ASN loop
        // (3 x N requests) into one request per endpoint per 150-ASN chunk — the single most
        // effective way to stay under PeeringDB's anonymous ~20 req/min rate limit. The three
        // endpoints (net, netixlan, netfac) are independent of one another, so they are fanned
        // out onto a virtual-thread executor to overlap their blocking I/O; a failed sub-call
        // still surfaces as an IOException out of this method.
        Set<Long> asns = new LinkedHashSet<>(request.getTargetAsns().keySet());

        Map<Long, PeeringDbNetwork> nets;
        Map<Long, List<PeeringDbNetIxlan>> ixPresence;
        Map<Long, List<PeeringDbNetFac>> facPresence;

        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Map<Long, PeeringDbNetwork>> netFuture =
                    exec.submit(() -> peeringDb.getNetworks(asns));
            Future<Map<Long, List<PeeringDbNetIxlan>>> ixFuture =
                    exec.submit(() -> peeringDb.getEquinixIxPresence(asns));
            Future<Map<Long, List<PeeringDbNetFac>>> facFuture =
                    exec.submit(() -> peeringDb.getEquinixFacPresence(asns));

            nets = awaitResult(netFuture);
            ixPresence = awaitResult(ixFuture);
            facPresence = awaitResult(facFuture);
        }

        networkMetadata.putAll(nets);
        for (Long asn : asns) {
            ixPresenceByAsn.put(asn, ixPresence.getOrDefault(asn, Collections.emptyList()));
            facPresenceByAsn.put(asn, facPresence.getOrDefault(asn, Collections.emptyList()));
        }
    }

    /**
     * Joins a PeeringDB sub-call, unwrapping its result while preserving the exact
     * exception behaviour of sequential calls: an {@link IOException} thrown by the
     * underlying GET is re-thrown as-is, and any other failure is wrapped in an
     * {@link IOException} so it still surfaces from {@code queryTargetAsns()}.
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

    // ---- Phase 3.5: Fabric service-profile availability ----

    /**
     * Tokens too generic to identify a network in a Fabric service-profile name. A profile-name
     * match decides that a network is privately REACHABLE via Fabric, so a false positive is worse
     * than a false negative (it silently promises an on-ramp that does not exist — the same
     * asymmetry {@link CloudProviderType} documents for its curated alias sets). Corporate suffixes,
     * connectivity vocabulary, and plain industry words are therefore never accepted as evidence
     * on their own; only the remaining brand-distinctive tokens are.
     */
    private static final Set<String> GENERIC_NAME_TOKENS = Set.of(
            // corporate forms and suffixes
            "inc", "llc", "ltd", "limited", "corp", "corporation", "company", "co", "com", "net",
            "org", "gmbh", "ag", "sa", "sarl", "bv", "nv", "plc", "pty", "kk", "ab", "as", "spa",
            "srl", "oy", "the", "of", "and", "for", "de", "la", "group", "holdings", "holding",
            "global", "international", "worldwide", "enterprise", "enterprises", "partners",
            // industry vocabulary
            "services", "service", "solutions", "systems", "technologies", "technology", "tech",
            "communications", "communication", "telecom", "telecommunications", "telekom",
            "network", "networks", "networking", "internet", "online", "digital", "media",
            "cloud", "hosting", "data", "datacenter", "web", "connect", "connectivity", "direct",
            "link", "exchange", "transit", "peering", "backbone", "carrier", "wireless", "mobile",
            "broadband", "fiber", "fibre", "cable", "usa", "america", "americas", "europe", "asia");

    /**
     * Cross-references the Fabric service-profile catalog against every target network, populating
     * {@link #fabricProfilesByAsn} with the metros where a matching profile is published.
     *
     * <p>Matching mirrors the optimizer's provider-availability approach: whole-token,
     * brand-distinctive evidence only. Two complementary matchers run per target:</p>
     * <ol>
     *   <li><b>Known cloud providers</b> — when the target's names identify a
     *       {@link CloudProviderType} (e.g. label {@code "AWS"}, or PeeringDB name
     *       {@code "Amazon.com, Inc."} via the {@code amazon} alias), profiles are matched with
     *       {@code CloudProviderType.matchesServiceProfileName}, which bridges the
     *       corporate-vs-product naming gap: marketplace profiles are named after the
     *       <em>product</em> ("AWS Direct Connect"), not the corporation.</li>
     *   <li><b>Everything else</b> — the profile name must contain, on whole-token boundaries, a
     *       brand-distinctive token of the target's label/PeeringDB names (generic corporate and
     *       connectivity words are excluded — see {@link #GENERIC_NAME_TOKENS} — so an NSP's
     *       "&lt;NSP&gt; Direct Connect" is never mis-attributed to a target).</li>
     * </ol>
     *
     * <p>Best-effort: if the Fabric service-profile catalog cannot be read, Fabric availability is
     * reported honestly as NOT analyzed (warning) rather than as {@code false} everywhere.</p>
     */
    private void analyzeFabricAvailability() {
        List<ServiceProfile> profiles;
        try {
            profiles = fabric.serviceProfiles().search().loadAll().toList();
        } catch (RuntimeException e) {
            warnings.add("Fabric service-profile availability was NOT analyzed: the Fabric "
                    + "service-profile catalog could not be read (" + describe(e) + "). Every "
                    + "fabricAvailable=false in this result means 'not analyzed', never 'no "
                    + "Fabric on-ramp exists'.");
            return;
        }

        for (Map.Entry<Long, String> entry : request.getTargetAsns().entrySet()) {
            long asn = entry.getKey();
            PeeringDbNetwork net = networkMetadata.get(asn);
            List<String> evidence = evidenceNames(entry.getValue(), net);
            CloudProviderType provider = resolveCloudProvider(evidence);
            Set<String> distinctive = distinctiveTokens(evidence);

            Map<MetroId, String> byMetro = new LinkedHashMap<>();
            for (ServiceProfile profile : profiles) {
                if (profile == null || profile.getName() == null) continue;
                if (!profileMatchesTarget(profile.getName(), provider, distinctive)) continue;
                List<ServiceProfileMetro> profileMetros = profile.metros();
                if (profileMetros == null) continue;
                for (ServiceProfileMetro spm : profileMetros) {
                    if (spm == null || spm.metroId() == null) continue;
                    byMetro.putIfAbsent(spm.metroId(), profile.getUuid());
                }
            }
            if (!byMetro.isEmpty()) {
                fabricProfilesByAsn.put(asn, byMetro);
            }
        }
    }

    /** The names that may identify a target network: the caller's label plus its PeeringDB names. */
    private static List<String> evidenceNames(String label, PeeringDbNetwork net) {
        List<String> names = new ArrayList<>();
        if (label != null && !label.isBlank()) names.add(label);
        if (net != null) {
            if (net.getName() != null && !net.getName().isBlank()) names.add(net.getName());
            if (net.getAka() != null && !net.getAka().isBlank()) names.add(net.getAka());
            if (net.getNameLong() != null && !net.getNameLong().isBlank()) names.add(net.getNameLong());
        }
        return names;
    }

    /**
     * Resolves the target to a known {@link CloudProviderType} when any of its names identifies
     * one (whole-token, brand-distinctive matching — the same test used for profile names), or
     * {@code null} for targets that are not a recognized cloud provider.
     */
    private static CloudProviderType resolveCloudProvider(List<String> evidence) {
        for (CloudProviderType type : CloudProviderType.values()) {
            if (type == CloudProviderType.OTHER) continue;
            for (String name : evidence) {
                if (type.matchesServiceProfileName(name)) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * The brand-distinctive whole tokens of the target's names: normalized tokens minus
     * {@link #GENERIC_NAME_TOKENS}, minus purely numeric tokens and single characters
     * (region codes and "365"-style digits are not brand evidence).
     */
    private static Set<String> distinctiveTokens(List<String> evidence) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String name : evidence) {
            for (String token : normalizeName(name).split(" ")) {
                if (token.length() < 2) continue;
                if (token.chars().allMatch(Character::isDigit)) continue;
                if (GENERIC_NAME_TOKENS.contains(token)) continue;
                tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * Whether a Fabric service-profile name identifies the target: via the resolved cloud
     * provider's curated matching when the target is a known cloud, or via a brand-distinctive
     * whole-token hit otherwise. Matching is one-directional — the profile name must contain the
     * evidence, never the reverse — because over-claiming reachability is the damaging error.
     */
    private static boolean profileMatchesTarget(String profileName, CloudProviderType provider,
                                                Set<String> distinctiveTokens) {
        if (provider != null && provider.matchesServiceProfileName(profileName)) {
            return true;
        }
        if (distinctiveTokens.isEmpty()) {
            return false;
        }
        String padded = " " + normalizeName(profileName) + " ";
        for (String token : distinctiveTokens) {
            if (padded.contains(" " + token + " ")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lower-cases and collapses every run of non-alphanumeric characters to a single space, so
     * token-boundary containment is well-defined ("Amazon.com, Inc." → "amazon com inc").
     */
    private static String normalizeName(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        boolean pendingSpace = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = Character.toLowerCase(raw.charAt(i));
            if (Character.isLetterOrDigit(ch)) {
                if (pendingSpace && sb.length() > 0) sb.append(' ');
                pendingSpace = false;
                sb.append(ch);
            } else {
                pendingSpace = true;
            }
        }
        return sb.toString();
    }

    // ---- Phase 4: Build PresenceMatrix ----

    private PresenceMatrix buildPresenceMatrix() {
        // Determine columns: all metros that have any presence
        Set<MetroId> allMetros = new TreeSet<>(Comparator.comparing(MetroId::code));
        for (NetworkPresence np : networkPresences.values()) {
            allMetros.addAll(np.getAllMetros());
        }

        // Metros where a target is reachable via a Fabric service profile are presence too —
        // a Fabric-only metro still earns a column (ConnectivityType.FABRIC_CONNECTION).
        for (Map<MetroId, String> fabricMetros : fabricProfilesByAsn.values()) {
            allMetros.addAll(fabricMetros.keySet());
        }

        // If customer metros specified, ensure they're included
        allMetros.addAll(request.getCustomerMetros());

        List<Long> asns = new ArrayList<>(request.getTargetAsns().keySet());
        List<MetroId> metros = new ArrayList<>(allMetros);

        Map<Long, Map<MetroId, PresenceCell>> cells = new LinkedHashMap<>();

        for (Long asn : asns) {
            Map<MetroId, PresenceCell> row = new LinkedHashMap<>();
            NetworkPresence np = networkPresences.get(asn);
            Map<MetroId, String> fabricMetros =
                    fabricProfilesByAsn.getOrDefault(asn, Collections.emptyMap());

            for (MetroId metro : metros) {
                boolean ixPresent = np != null && np.hasIxPeeringAt(metro);
                boolean facPresent = np != null && np.hasFacilityAt(metro);
                String fabricUuid = fabricMetros.get(metro);
                boolean fabricAvailable = fabricUuid != null;

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

                ConnectivityType connType = ConnectivityType.resolve(ixPresent, fabricAvailable, facPresent);

                PresenceCell cell = PresenceCell.builder()
                        .asn(asn)
                        .metro(metro)
                        .connectivityType(connType)
                        .ixPresent(ixPresent)
                        .facilityPresent(facPresent)
                        .fabricAvailable(fabricAvailable)
                        .fabricServiceProfileUuid(fabricUuid)
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
                    lostIxLabels.add(resolveLabel(asn));
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
            // Rank the failover options instead of leaking PeeringDB iteration order: best
            // geographic diversity first, then the biggest IX capacity, then route-server
            // availability (automatic peering beats bilateral setup at equal diversity/capacity).
            metroFailovers.sort(FAILOVER_RANKING);
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
        // Metros with no analyzed-ASN presence carry no blast signal and are EXCLUDED from the
        // blast-score average (scoring them 1.0 would reward having nothing to lose). Say so.
        long noPresenceMetros = blastReports.stream()
                .filter(br -> br.totalAffectedAsns() == 0)
                .count();
        if (noPresenceMetros > 0) {
            findings.add(noPresenceMetros + " customer metro(s) have no presence from any analyzed "
                    + "ASN; they were excluded from blast-radius scoring (an empty metro is not "
                    + "resilience) — establish connectivity there or drop them from the analysis.");
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

            // ONE opportunity per (target ASN, IX): a target with several parallel ports on the
            // same IX LAN is a single peering opportunity, not a duplicate per netixlan session.
            // Capacity is aggregated across the sessions and route-server participation is true
            // when ANY session peers with the route servers.
            Map<Integer, List<IxPresenceDetail>> sharedIxSessions = new LinkedHashMap<>();
            for (IxPresenceDetail ixDetail : np.getIxDetails()) {
                if (customerIxIds.contains(ixDetail.getIxId())) {
                    sharedIxSessions.computeIfAbsent(ixDetail.getIxId(), k -> new ArrayList<>())
                            .add(ixDetail);
                }
            }

            for (Map.Entry<Integer, List<IxPresenceDetail>> ixEntry : sharedIxSessions.entrySet()) {
                List<IxPresenceDetail> sessions = ixEntry.getValue();
                IxPresenceDetail representative = sessions.get(0);
                boolean anyRouteServer = sessions.stream().anyMatch(IxPresenceDetail::isRouteServerPeer);
                long aggregateSpeedMbps = sessions.stream().mapToLong(IxPresenceDetail::getSpeedMbps).sum();

                // Both present at this IX — peering opportunity!
                double feasibility = np.getPeeringPolicy().getFeasibilityScore();
                if (anyRouteServer) feasibility = Math.min(1.0, feasibility + 0.3);

                String complexity;
                if (anyRouteServer && np.getPeeringPolicy() == PeeringPolicy.OPEN) {
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
                        .metro(representative.getMetro())
                        .ixName(representative.getIxName())
                        .ixId(ixEntry.getKey())
                        .targetPolicy(np.getPeeringPolicy())
                        .targetUsesRouteServer(anyRouteServer)
                        .targetSpeedMbps(aggregateSpeedMbps)
                        .targetSessionCount(sessions.size())
                        .feasibility(feasibility)
                        .complexity(complexity)
                        .recommendation(buildPeeringRecommendation(np, representative, complexity))
                        .build());
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
                        .fabricServiceProfileUuid(cell.getFabricServiceProfileUuid())
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

    /**
     * Ranking for a metro's failover options: geographic diversity rating first (descending;
     * an UNKNOWN/unavailable distance ranks below every real rating rather than tying CRITICAL),
     * then aggregate IX capacity (descending), then route-server availability (available first).
     */
    private static final Comparator<FailoverPath> FAILOVER_RANKING = Comparator
            .comparingDouble(PeeringIntelligenceEngine::diversityRank).reversed()
            .thenComparing(Comparator.comparingLong(FailoverPath::getIxCapacityMbps).reversed())
            .thenComparing(fp -> fp.isRouteServerAvailable() ? 0 : 1);

    /** A failover path's diversity as a sortable rank; unknown distance sorts below CRITICAL (0.0). */
    private static double diversityRank(FailoverPath path) {
        DiversityScore diversity = path.getDiversity();
        if (diversity == null || diversity.isDistanceUnavailable()) {
            return -1.0;
        }
        return diversity.getRating().getScore();
    }

    /**
     * Resolves a target ASN's display label, null-safely: the caller-supplied label when one was
     * given, otherwise the {@link NetworkPresence} label (PeeringDB name, or {@code "AS<asn>"}).
     * The request map stores a {@code null} value for {@code addAsn(long)} without a label, so a
     * plain {@code getOrDefault} would return {@code null} — the key exists.
     */
    private String resolveLabel(long asn) {
        String label = request.getTargetAsns().get(asn);
        if (label != null) {
            return label;
        }
        NetworkPresence np = networkPresences.get(asn);
        return np != null ? np.getLabel() : "AS" + asn;
    }

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

        // Factor 1: Average blast radius impact (lower is better), computed over ONLY the customer
        // metros that have some analyzed-ASN presence to lose. A metro with ZERO relevant presence
        // has impactRatio 0, and folding it in would score the absence as a perfect 1.0 — rewarding
        // a customer for having nothing at a site. Absence is excluded, never rewarded; if NO metro
        // has relevant presence the blast factor is neutral (0.5), not perfect.
        OptionalDouble avgImpact = blastReports.stream()
                .filter(br -> br.totalAffectedAsns() > 0)
                .mapToDouble(BlastRadiusReport::getImpactRatio)
                .average();
        double blastScore = avgImpact.isPresent() ? 1.0 - avgImpact.getAsDouble() : 0.5;

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
