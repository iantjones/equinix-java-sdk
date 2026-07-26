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

package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.exception.EquinixAuthorizationException;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.exception.EquinixRateLimitException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.DeploymentTopology;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationRequest;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.ProviderRequirement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadSpec;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionBodies;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionInputRequirement;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.design.optimizer.wizard.model.RouterBodies;
import com.eqixiac.equinix.fabric.client.CloudRouters;
import com.eqixiac.equinix.fabric.client.Connections;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.enums.CloudRouterPackageCode;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import com.eqixiac.equinix.fabric.model.CloudRouterPackage;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.AccessPointTypeConfig;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.fabric.model.json.creators.ConnectionOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Layered, plan-time validation for the Deployment Wizard. Separates "validate the plan/data" from
 * "live dry-run a connection" so a brand-new customer — who owns no resources and may target a metro
 * with zero footprint — still gets a fully validated plan without provisioning anything first.
 *
 * <ul>
 *   <li><b>Layer&nbsp;1 — structural / catalog (no provisioning, no live connection dry-run):</b>
 *       names (length/charset, asserted locally — never learned from a 400), duplicate names, IP /30
 *       overlaps, ASN sanity, package/metro validity; and, against the public catalog the optimizer
 *       already fetched, that every Z-side service-profile uuid resolves, is offered in the connection's
 *       metro with a valid seller region, and that the bandwidth fits the profile's allowed tiers and
 *       the router package ceiling. A required cloud that is NOT offered at a targeted metro is flagged
 *       as a new-market gap rather than silently dropped.</li>
 *   <li><b>Layer&nbsp;2 — live router dry-run (self-contained, real):</b> each planned Cloud Router is
 *       posted to {@code POST /fabric/v4/routers?dryRun=true}. FCRs have no dependency, so this fully
 *       validates package/metro/notifications at plan time with nothing provisioned.</li>
 *   <li><b>Layer&nbsp;3 — connection endpoint dry-run:</b> a connection whose A-side Cloud Router does
 *       not exist yet is <em>not</em> sent a doomed endpoint-less dry-run; it is recorded as DEFERRED
 *       (validated structurally now; the live endpoint dry-run runs at provisioning, and needs the
 *       customer's authorization key). A connection whose A-side endpoint already exists (a
 *       caller-supplied port/FCR, lens 3b) IS dry-run for real, with a complete body.</li>
 * </ul>
 *
 * <p>Every catalog / dry-run step is best-effort: a gateway that cannot serve a surface at all (a bare
 * stub, an offline run) skips that step rather than failing the plan, exactly as the optimizer's
 * catalog scan degrades. Hard errors are only raised from working catalog data or a genuine API
 * rejection.</p>
 *
 * <h3>Three outcome buckets</h3>
 * <p>A live-layer step reaches one of three distinct verdicts, never conflated:</p>
 * <ul>
 *   <li><b>ERROR</b> — the plan is wrong: a hard structural/catalog failure, or a genuine API
 *       <em>rejection</em> of a well-formed request (a 4xx validation rejection, e.g. HTTP 400/409).
 *       Errors invalidate the plan.</li>
 *   <li><b>DEFERRED</b> — will legitimately validate at provisioning: the connection endpoint dry-run
 *       postponed because the A-side Cloud Router does not exist yet (lens&nbsp;3a). Not an error.</li>
 *   <li><b>SKIPPED</b> — could not validate now, and here is why: an <em>infeasibility</em> — the live
 *       surface was unavailable (offline / bare stub / not connected), or the API answered with a
 *       non-rejection failure (401/403 not entitled, 429 throttled, 5xx server-side, or a
 *       transport/timeout error). A skip never invalidates the plan; it is surfaced with a
 *       human/LLM-readable reason so the gap is called out rather than hidden.</li>
 * </ul>
 * <p>The rejection-vs-infeasibility split is centralized in {@link #classifyLiveFailure(Throwable)} so
 * every live-layer catch classifies identically, and so the execution-time pre-flight can reuse it.</p>
 */
public final class PlanValidator {

    /** The 32-bit ASN space; a customer ASN outside {@code 1..this} is invalid. */
    private static final long MAX_ASN = 4_294_967_295L;

    private PlanValidator() {}

    /**
     * The outcome of validating a plan, in three distinct buckets — hard/rejection <b>errors</b> (which
     * invalidate the plan), <b>deferred</b> validations (which do NOT — they run at provisioning), and
     * <b>skipped</b> validations (which do NOT — they could not be attempted now, each with a reason) —
     * plus the per-connection customer inputs a first-time customer must gather.
     */
    public static final class Result {
        /**
         * Hard structural / catalog failures and genuine live API rejections of a well-formed request;
         * a non-empty list invalidates the plan.
         */
        public final List<String> errors;
        /** Validations deferred to provisioning (the live connection endpoint dry-run); not errors. */
        public final List<String> deferred;
        /**
         * Validations that could NOT be attempted now — an infeasibility, never a plan defect: a live
         * surface was unavailable (offline / bare stub), or the API answered with a non-rejection
         * failure (401/403/429/5xx or a transport/timeout error). Each entry is a human/LLM-readable
         * reason. Skipped validations never invalidate the plan.
         */
        public final List<String> skipped;
        /** The per-connection authorization a customer must gather before provisioning. */
        public final List<ConnectionInputRequirement> requiredInputs;

        Result(List<String> errors, List<String> deferred, List<String> skipped,
               List<ConnectionInputRequirement> requiredInputs) {
            this.errors = errors;
            this.deferred = deferred;
            this.skipped = skipped;
            this.requiredInputs = requiredInputs;
        }
    }

    /**
     * Validates a plan across all layers.
     *
     * @param metros              the source metro recommendations (for the new-market check); may be {@code null}
     * @param request             the optimization request (for workload provider dependencies); may be {@code null}
     * @param topology            the placement topology (which workloads sit where); may be {@code null}
     * @param routers             the planned Cloud Routers
     * @param providerConnections the planned provider connections
     * @param backboneLinks       the planned backbone links
     * @param protocols           the planned routing protocols (for IP overlap checks)
     * @param customerAsn         the customer BGP ASN, or {@code null} to skip the ASN sanity check
     * @param fabric              the gateway for catalog + dry-run access; {@code null} skips every live step
     * @return the validation result
     */
    public static Result validate(
            List<MetroRecommendation> metros,
            OptimizationRequest request,
            DeploymentTopology topology,
            List<PlannedCloudRouter> routers,
            List<PlannedConnection> providerConnections,
            List<PlannedBackboneLink> backboneLinks,
            List<PlannedRoutingProtocol> protocols,
            Long customerAsn,
            FabricGateway fabric) {

        List<PlannedCloudRouter> rs = nz(routers);
        List<PlannedConnection> pcs = nz(providerConnections);
        List<PlannedBackboneLink> bbs = nz(backboneLinks);
        List<PlannedRoutingProtocol> rps = nz(protocols);

        List<String> errors = new ArrayList<>();
        List<String> deferred = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<ConnectionInputRequirement> requiredInputs = new ArrayList<>();

        // ── Layer 1: local structural checks (no API) ──
        checkRouterReferences(rs, pcs, bbs, errors);
        checkNames(rs, pcs, bbs, rps, errors);
        checkDuplicateNames(rs, pcs, bbs, rps, errors);
        checkSubnetOverlap(rps, errors);
        checkAsn(customerAsn, errors);
        checkPackagesAndMetros(rs, errors);
        checkNotifications(rs, errors);
        checkConnectionTypes(pcs, bbs, errors);

        // ── Layer 1: new-market gap (required provider not available at a placed metro) ──
        checkNewMarketGaps(metros, request, topology, errors);

        // ── Layer 1: catalog checks (best-effort — an unavailable surface is SKIPPED, not silent) ──
        catalogChecks(fabric, pcs, errors, skipped);
        packageCeilingChecks(fabric, rs, pcs, bbs, errors, skipped);

        // ── Layer 2: live router dry-run ──
        routerDryRun(fabric, rs, errors, skipped);

        // ── Layer 3: connection endpoint dry-run (deferred, real, or skipped when infeasible) ──
        connectionDispatch(fabric, pcs, errors, deferred, skipped, requiredInputs);

        return new Result(errors, deferred, skipped, requiredInputs);
    }

    // ══════════════════════════════════════════════
    //  Layer 1 — local structural
    // ══════════════════════════════════════════════

    private static void checkRouterReferences(
            List<PlannedCloudRouter> routers, List<PlannedConnection> connections,
            List<PlannedBackboneLink> links, List<String> errors) {

        if (routers.isEmpty() && (!connections.isEmpty() || !links.isEmpty())) {
            errors.add("No Cloud Routers planned — at least one metro recommendation is required");
        }

        Set<String> routerNames = new LinkedHashSet<>();
        for (PlannedCloudRouter r : routers) {
            routerNames.add(r.getName());
        }

        for (PlannedConnection conn : connections) {
            if (!routerNames.contains(conn.getASideRouterName())) {
                errors.add("Connection '" + conn.getName() + "' references unknown A-side router: "
                        + conn.getASideRouterName());
            }
        }
        for (PlannedBackboneLink link : links) {
            PlannedConnection conn = link.getConnection();
            if (conn == null) {
                continue;
            }
            if (!routerNames.contains(conn.getASideRouterName())) {
                errors.add("Backbone link '" + link.getName() + "' references unknown A-side router: "
                        + conn.getASideRouterName());
            }
            if (!routerNames.contains(conn.getZSideRouterName())) {
                errors.add("Backbone link '" + link.getName() + "' references unknown Z-side router: "
                        + conn.getZSideRouterName());
            }
        }
    }

    private static void checkNames(
            List<PlannedCloudRouter> routers, List<PlannedConnection> connections,
            List<PlannedBackboneLink> links, List<PlannedRoutingProtocol> protocols, List<String> errors) {

        for (PlannedCloudRouter r : routers) {
            checkName("Cloud Router", r.getName(), errors);
        }
        for (PlannedConnection c : connections) {
            checkName("Connection", c.getName(), errors);
        }
        for (PlannedBackboneLink l : links) {
            checkName("Backbone link", l.getName(), errors);
        }
        for (PlannedRoutingProtocol p : protocols) {
            checkName("Routing protocol", p.getName(), errors);
        }
    }

    private static void checkName(String kind, String name, List<String> errors) {
        if (name == null || name.isEmpty()) {
            errors.add(kind + " has a blank name");
            return;
        }
        if (name.length() > 23) {
            errors.add(kind + " name '" + name + "' is " + name.length()
                    + " characters — Fabric rejects names of 24 or more (EQ-3142539)");
        }
        if (!name.matches("[A-Za-z0-9_-]+")) {
            errors.add(kind + " name '" + name + "' contains characters outside [A-Za-z0-9_-]");
        }
    }

    private static void checkDuplicateNames(
            List<PlannedCloudRouter> routers, List<PlannedConnection> connections,
            List<PlannedBackboneLink> links, List<PlannedRoutingProtocol> protocols, List<String> errors) {

        List<String> all = new ArrayList<>();
        routers.forEach(r -> all.add(r.getName()));
        connections.forEach(c -> all.add(c.getName()));
        links.forEach(l -> all.add(l.getName()));
        protocols.forEach(p -> all.add(p.getName()));

        Set<String> seen = new LinkedHashSet<>();
        Set<String> dupes = new LinkedHashSet<>();
        for (String n : all) {
            if (n != null && !seen.add(n)) {
                dupes.add(n);
            }
        }
        for (String d : dupes) {
            errors.add("Duplicate resource name '" + d + "' — every name in a plan must be unique");
        }
    }

    /**
     * Flags two <em>different</em> connections assigned the same /30 peering subnet. The DIRECT and
     * BGP protocols of one connection intentionally share a /30, so overlap is detected across
     * distinct parent connections only.
     */
    private static void checkSubnetOverlap(List<PlannedRoutingProtocol> protocols, List<String> errors) {
        Map<String, Set<String>> baseToConns = new HashMap<>();
        for (PlannedRoutingProtocol p : protocols) {
            String conn = p.getConnectionName();
            for (String ip : List.of(
                    orEmpty(p.getEquinixIfaceIpv4()), orEmpty(p.getEquinixPeerIpv4()), orEmpty(p.getCustomerPeerIpv4()))) {
                String base = base30(ip);
                if (base != null && conn != null) {
                    baseToConns.computeIfAbsent(base, k -> new LinkedHashSet<>()).add(conn);
                }
            }
        }
        for (Map.Entry<String, Set<String>> e : baseToConns.entrySet()) {
            if (e.getValue().size() > 1) {
                errors.add("IP overlap: connections " + e.getValue()
                        + " are assigned the same /30 subnet (" + e.getKey() + "/30)");
            }
        }
    }

    private static void checkAsn(Long customerAsn, List<String> errors) {
        if (customerAsn != null && (customerAsn <= 0 || customerAsn > MAX_ASN)) {
            errors.add("Customer ASN " + customerAsn + " is out of the valid range (1.." + MAX_ASN + ")");
        }
    }

    private static void checkPackagesAndMetros(List<PlannedCloudRouter> routers, List<String> errors) {
        for (PlannedCloudRouter r : routers) {
            if (r.getMetroId() == null) {
                errors.add("Cloud Router '" + r.getName() + "': missing metro");
            }
            GatewayPackageCode pkg = r.getPackageCode();
            if (pkg == null || pkg == GatewayPackageCode.UNKNOWN) {
                errors.add("Cloud Router '" + r.getName() + "': missing or unknown package code (" + pkg + ")");
            }
        }
    }

    /**
     * Fabric REQUIRES at least one notification recipient to create a Cloud Router. A router POSTed
     * without one is rejected live with HTTP 400 {@code EQ-3040013} ("Notifications is mandatory field /
     * Property: $.notifications"). This asserts the requirement structurally, at plan time, so an omitted
     * notification surfaces as a friendly pre-flight error here — and its router is skipped by the live
     * router dry-run ({@link #routerDryRun}) — instead of a raw API 400 from a doomed dry-run.
     * "No notification email" means the router's recipient LIST is empty (or holds only blanks) —
     * routers carry every configured address, not a single one.
     */
    private static void checkNotifications(List<PlannedCloudRouter> routers, List<String> errors) {
        for (PlannedCloudRouter r : routers) {
            if (RouterBodies.usableNotificationEmails(r).isEmpty()) {
                errors.add("Cloud Router '" + r.getName() + "' (" + r.getMetroId()
                        + ") has no notification email: Fabric requires at least one notification recipient "
                        + "to create a Cloud Router. Supply deployment.notifications.");
            }
        }
    }

    /**
     * Fabric accepts a virtual connection whose A-side is a Cloud Router only as {@code IP_VC}
     * (FCR A-side =&gt; IP_VC); the port-based types ({@code EVPL_VC}, ...) are rejected live. Every
     * wizard-planned connection — provider connections (unless redeemed from a caller-supplied
     * customer port) and all backbone links (Cloud Router to Cloud Router) — has an FCR A-side, so
     * an incompatible explicitly-configured type is a structural error here rather than a live 400.
     */
    private static void checkConnectionTypes(
            List<PlannedConnection> connections, List<PlannedBackboneLink> links, List<String> errors) {
        for (PlannedConnection conn : connections) {
            checkFcrConnectionType("Connection", conn, errors);
        }
        for (PlannedBackboneLink link : links) {
            if (link.getConnection() != null) {
                checkFcrConnectionType("Backbone link", link.getConnection(), errors);
            }
        }
    }

    private static void checkFcrConnectionType(String kind, PlannedConnection conn, List<String> errors) {
        if (conn.getASidePortUuid() != null) {
            return; // a customer-port A-side may legitimately be EVPL_VC etc.
        }
        ConnectionType type = conn.getConnectionType();
        if (type != null && type != ConnectionType.IP_VC) {
            errors.add(kind + " '" + conn.getName() + "': connection type " + type
                    + " is incompatible with a Cloud Router A-side — Fabric only accepts IP_VC for "
                    + "FCR-originated connections (FCR A-side => IP_VC). Use IP_VC, or supply a "
                    + "customer port A-side for a port-based type.");
        }
    }

    // ══════════════════════════════════════════════
    //  Layer 1 — new-market gap
    // ══════════════════════════════════════════════

    /**
     * Flags a metro that hosts a workload requiring a cloud provider which is NOT available there —
     * the brand-new-market case, surfaced at plan time (which provider, which metro) rather than
     * silently omitted and only discovered at provisioning.
     */
    private static void checkNewMarketGaps(
            List<MetroRecommendation> metros, OptimizationRequest request,
            DeploymentTopology topology, List<String> errors) {

        if (metros == null || request == null || topology == null || request.getWorkloads() == null) {
            return;
        }
        Map<String, WorkloadSpec> byLabel = new HashMap<>();
        for (WorkloadSpec spec : request.getWorkloads()) {
            if (spec != null && spec.getLabel() != null) {
                byLabel.putIfAbsent(spec.getLabel(), spec);
            }
        }

        for (MetroRecommendation metro : metros) {
            MetroId metroId = metro.getMetroId();
            List<WorkloadPlacement> placements = topology.forMetro(metroId);
            if (placements.isEmpty()) {
                continue;
            }
            Set<String> requiredLabels = new LinkedHashSet<>();
            for (WorkloadPlacement placement : placements) {
                WorkloadSpec spec = byLabel.get(placement.getWorkloadLabel());
                if (spec == null || spec.getDependsOnProviders() == null) {
                    continue;
                }
                for (ProviderRequirement dep : spec.getDependsOnProviders()) {
                    if (dep != null && dep.displayLabel() != null) {
                        requiredLabels.add(dep.displayLabel());
                    }
                }
            }
            for (String label : requiredLabels) {
                if (!availableAt(metro, label)) {
                    errors.add("New-market gap: metro " + metroId + " hosts workload(s) that require provider '"
                            + label + "', but it is not available there — no connection can be planned for it");
                }
            }
        }
    }

    private static boolean availableAt(MetroRecommendation metro, String label) {
        if (metro.getAvailableProviders() == null) {
            return false;
        }
        for (ProviderAvailability p : metro.getAvailableProviders()) {
            if (p.isAvailable() && label.equalsIgnoreCase(p.getProviderLabel())) {
                return true;
            }
        }
        return false;
    }

    // ══════════════════════════════════════════════
    //  Layer 1 — catalog checks
    // ══════════════════════════════════════════════

    private static void catalogChecks(FabricGateway fabric, List<PlannedConnection> connections,
                                      List<String> errors, List<String> skipped) {
        if (connections.isEmpty()) {
            return;
        }
        ServiceProfiles client = fabric == null ? null : safeGet(() -> fabric.serviceProfiles());
        if (client == null) {
            // Offline / bare stub — no usable Service Profile catalog surface. Call the skip out rather
            // than validating silently to nothing.
            skipped.add("Catalog validation skipped: no Service Profile catalog surface available "
                    + "(offline or not connected)");
            return;
        }

        Map<String, Lookup> cache = new HashMap<>();
        for (PlannedConnection conn : connections) {
            String uuid = conn.getZSideServiceProfileUuid();
            if (uuid == null || uuid.isBlank()) {
                errors.add("Connection '" + conn.getName() + "': missing Z-side service profile UUID");
                continue;
            }
            Lookup lookup = cache.computeIfAbsent(uuid, u -> lookupProfile(client, u));
            switch (lookup.status) {
                case UNAVAILABLE:
                    // The catalog could not be reached for this uuid — SKIPPED with a reason, never silent.
                    skipped.add("Catalog check skipped for connection '" + conn.getName()
                            + "': service profile '" + uuid + "' could not be retrieved from the catalog "
                            + "(unreachable)");
                    break;
                case NOT_FOUND:
                    errors.add("Connection '" + conn.getName() + "': service profile '" + uuid
                            + "' was not found in the catalog");
                    break;
                case RESOLVED:
                    checkProfile(conn, lookup.profile, errors);
                    break;
            }
        }
    }

    private static void checkProfile(PlannedConnection conn, ServiceProfile profile, List<String> errors) {
        MetroId aMetro = conn.getASideMetro();
        ServiceProfileMetro match = null;
        List<ServiceProfileMetro> spMetros = profile.metros();
        if (spMetros != null && aMetro != null) {
            for (ServiceProfileMetro spm : spMetros) {
                if (aMetro.equals(spm.metroId())) {
                    match = spm;
                    break;
                }
            }
        }

        if (match == null) {
            errors.add("Connection '" + conn.getName() + "': provider '" + conn.getZSideProviderLabel()
                    + "' (profile " + conn.getZSideServiceProfileUuid() + ") is not offered in metro "
                    + aMetro + " — new-market gap");
        } else {
            String region = conn.getZSideSellerRegion();
            if (region != null && match.getSellerRegions() != null
                    && !match.getSellerRegions().containsKey(region)) {
                errors.add("Connection '" + conn.getName() + "': seller region '" + region
                        + "' is not offered for profile " + conn.getZSideServiceProfileUuid()
                        + " in metro " + aMetro);
            }
            Integer vcMax = match.getVcBandwidthMax();
            if (vcMax != null && conn.getBandwidthMbps() > vcMax) {
                errors.add("Connection '" + conn.getName() + "': bandwidth " + conn.getBandwidthMbps()
                        + " Mbps exceeds the profile's maximum (" + vcMax + " Mbps) in metro " + aMetro);
            }
        }

        // Bandwidth vs the profile's allowed tiers (profile-level, metro-independent).
        List<Integer> tiers = new ArrayList<>();
        boolean allowCustom = false;
        if (profile.getAccessPointTypeConfigs() != null) {
            for (AccessPointTypeConfig cfg : profile.getAccessPointTypeConfigs()) {
                if (cfg == null) {
                    continue;
                }
                if (Boolean.TRUE.equals(cfg.getAllowCustomBandwidth())) {
                    allowCustom = true;
                }
                if (cfg.getSupportedBandwidths() != null) {
                    tiers.addAll(cfg.getSupportedBandwidths());
                }
            }
        }
        if (!allowCustom && !tiers.isEmpty() && !tiers.contains(conn.getBandwidthMbps())) {
            errors.add("Connection '" + conn.getName() + "': bandwidth " + conn.getBandwidthMbps()
                    + " Mbps is not an allowed tier " + tiers + " for profile "
                    + conn.getZSideServiceProfileUuid());
        }
    }

    private static void packageCeilingChecks(
            FabricGateway fabric, List<PlannedCloudRouter> routers,
            List<PlannedConnection> providerConnections, List<PlannedBackboneLink> backboneLinks,
            List<String> errors, List<String> skipped) {

        if (routers.isEmpty()) {
            return;
        }
        CloudRouters client = fabric == null ? null : safeGet(() -> fabric.cloudRouters());
        if (client == null) {
            // Offline / bare stub — the Cloud Router package catalog cannot be consulted. Call it out.
            skipped.add("Package-ceiling checks skipped: no Cloud Router API surface available "
                    + "(offline or not connected)");
            return;
        }

        // VC count per router name.
        Map<String, Integer> vcCount = new HashMap<>();
        Map<String, GatewayPackageCode> routerPkg = new HashMap<>();
        for (PlannedCloudRouter r : routers) {
            routerPkg.put(r.getName(), r.getPackageCode());
        }
        for (PlannedConnection c : providerConnections) {
            increment(vcCount, c.getASideRouterName());
        }
        for (PlannedBackboneLink link : backboneLinks) {
            PlannedConnection c = link.getConnection();
            if (c != null) {
                increment(vcCount, c.getASideRouterName());
                increment(vcCount, c.getZSideRouterName());
            }
        }

        Map<GatewayPackageCode, CloudRouterPackage> pkgCache = new HashMap<>();

        for (PlannedCloudRouter r : routers) {
            CloudRouterPackage pkg = resolvePackage(client, r.getPackageCode(), pkgCache);
            if (pkg == null) {
                continue;
            }
            Integer vcCountMax = pkg.getVcCountMax();
            int count = vcCount.getOrDefault(r.getName(), 0);
            if (vcCountMax != null && count > vcCountMax) {
                errors.add("Cloud Router '" + r.getName() + "' plans " + count + " connection(s), exceeding package "
                        + r.getPackageCode() + " maximum of " + vcCountMax);
            }
        }

        // Per-VC bandwidth ceiling against each connection's A-side router package.
        for (PlannedConnection c : providerConnections) {
            checkVcBandwidth(client, c, "Connection", routerPkg.get(c.getASideRouterName()), pkgCache, errors);
        }
        for (PlannedBackboneLink link : backboneLinks) {
            PlannedConnection c = link.getConnection();
            if (c != null) {
                checkVcBandwidth(client, c, "Backbone link", routerPkg.get(c.getASideRouterName()), pkgCache, errors);
            }
        }
    }

    private static void checkVcBandwidth(
            CloudRouters client, PlannedConnection conn, String kind, GatewayPackageCode gateway,
            Map<GatewayPackageCode, CloudRouterPackage> pkgCache, List<String> errors) {

        CloudRouterPackage pkg = resolvePackage(client, gateway, pkgCache);
        if (pkg == null || pkg.getVcBandwidthMax() == null) {
            return;
        }
        if (conn.getBandwidthMbps() > pkg.getVcBandwidthMax()) {
            errors.add(kind + " '" + conn.getName() + "': bandwidth " + conn.getBandwidthMbps()
                    + " Mbps exceeds the A-side router package " + gateway + " per-VC maximum ("
                    + pkg.getVcBandwidthMax() + " Mbps)");
        }
    }

    private static CloudRouterPackage resolvePackage(
            CloudRouters client, GatewayPackageCode gateway,
            Map<GatewayPackageCode, CloudRouterPackage> cache) {

        if (gateway == null || gateway == GatewayPackageCode.UNKNOWN) {
            return null;
        }
        if (cache.containsKey(gateway)) {
            return cache.get(gateway);
        }
        CloudRouterPackage pkg = null;
        try {
            // The wizard stores the package as GatewayPackageCode; the package catalog is keyed by the
            // constant-identical CloudRouterPackageCode. Bridge by name.
            CloudRouterPackageCode code = CloudRouterPackageCode.valueOf(gateway.name());
            pkg = client.routerPackageByCode(code);
        } catch (RuntimeException e) {
            pkg = null; // unknown bridge or transport error — best-effort skip
        }
        cache.put(gateway, pkg);
        return pkg;
    }

    // ══════════════════════════════════════════════
    //  Layer 2 — router dry-run
    // ══════════════════════════════════════════════

    private static void routerDryRun(FabricGateway fabric, List<PlannedCloudRouter> routers,
                                     List<String> errors, List<String> skipped) {
        if (routers.isEmpty()) {
            return;
        }
        CloudRouters client = fabric == null ? null : safeGet(() -> fabric.cloudRouters());
        if (client == null) {
            // Best-effort: no usable cloud-router surface (bare stub / offline). SKIPPED with a reason,
            // never a silent return — a fully offline run must still call out that this layer ran nowhere.
            skipped.add("Live router dry-run skipped: no Cloud Router API surface available "
                    + "(offline or not connected)");
            return;
        }
        for (PlannedCloudRouter r : routers) {
            // Layer 1 already flags a missing metro / package / notification; do not send a doomed dry-run
            // for it — a router with no notification would 400 live on the mandatory $.notifications field
            // (EQ-3040013), so the friendly Layer-1 error stands in for it rather than a raw API rejection.
            if (r.getMetroId() == null || r.getPackageCode() == null
                    || r.getPackageCode() == GatewayPackageCode.UNKNOWN
                    || RouterBodies.usableNotificationEmails(r).isEmpty()) {
                continue;
            }
            try {
                // The body comes from the SAME shared builder the execution-time Phase-1 create uses
                // (RouterBodies), so what this dry-run validates is byte-for-byte what execute() will
                // send — the only difference is the dryRun() terminator.
                RouterBodies.routerBody(client, r).dryRun().create();
            } catch (Exception e) {
                LiveFailure failure = classifyLiveFailure(e);
                if (failure.isInfeasible()) {
                    // Infeasibility (not entitled / throttled / server-side / transport): the dry-run
                    // could not be attempted. SKIPPED with a reason — never an error that would
                    // invalidate an otherwise-sound plan.
                    skipped.add("Router dry-run skipped for '" + r.getName() + "': " + failure.reason());
                } else {
                    // A genuine API rejection of a well-formed request — the plan is wrong.
                    errors.add("Router dry-run validation warning for '" + r.getName() + "': " + e.getMessage());
                }
            }
        }
    }

    // ══════════════════════════════════════════════
    //  Layer 3 — connection endpoint dry-run dispatch
    // ══════════════════════════════════════════════

    private static void connectionDispatch(
            FabricGateway fabric, List<PlannedConnection> connections,
            List<String> errors, List<String> deferred, List<String> skipped,
            List<ConnectionInputRequirement> requiredInputs) {

        if (connections.isEmpty()) {
            return;
        }
        Connections client = fabric == null ? null : safeGet(() -> fabric.connections());

        for (PlannedConnection conn : connections) {
            ConnectionInputRequirement requirement = requirementFor(conn);
            requiredInputs.add(requirement);

            if (!conn.hasPreExistingEndpoint()) {
                // Lens 3a: the A-side Cloud Router does not exist yet, so the live endpoint dry-run is
                // genuinely DEFERRED to provisioning — recorded honestly rather than sent as a doomed
                // dry-run. This is the ONLY connection shape that is deferred.
                deferred.add(deferredNote(conn, requirement));
                continue;
            }

            // Lens 3b: the A-side endpoint already exists, so a full, real dry-run is possible in
            // principle at PLAN time. If it cannot actually be attempted now, that is a SKIP with a
            // truthful reason — never a defer carrying the false "Cloud Router does not exist yet" note.
            if (client == null) {
                skipped.add("Live dry-run skipped for connection '" + conn.getName()
                        + "': no Connections API surface available (offline or not connected)");
                continue;
            }
            if (conn.getZSideAuthenticationKey() == null) {
                skipped.add("Live dry-run skipped for connection '" + conn.getName()
                        + "': the cloud authorization key is not yet supplied, so a complete body "
                        + "cannot be assembled to dry-run now");
                continue;
            }
            try {
                ConnectionOperator.ConnectionBuilder body = conn.getASidePortUuid() != null
                        ? ConnectionBodies.providerBodyOnPort(client, conn, conn.getASidePortUuid())
                        : ConnectionBodies.providerBody(client, conn, conn.getASideExistingRouterUuid());
                body.dryRun().create();
            } catch (Exception e) {
                LiveFailure failure = classifyLiveFailure(e);
                if (failure.isInfeasible()) {
                    // Not entitled / throttled / server-side / transport — the dry-run could not run.
                    skipped.add("Live dry-run skipped for connection '" + conn.getName() + "': "
                            + failure.reason());
                } else {
                    // A genuine API rejection of a well-formed request — the plan is wrong.
                    errors.add("Live dry-run for connection '" + conn.getName() + "' failed: " + e.getMessage());
                }
            }
        }
    }

    private static ConnectionInputRequirement requirementFor(PlannedConnection conn) {
        CloudProviderType type = conn.getZSideCloudType() != null
                ? conn.getZSideCloudType()
                : ConnectionBodies.resolveCloudType(conn.getZSideProviderLabel());
        boolean authRequired = type != CloudProviderType.OTHER;
        return ConnectionInputRequirement.builder()
                .connectionName(conn.getName())
                .providerLabel(conn.getZSideProviderLabel())
                .cloudType(type)
                .authenticationKeyLabel(ConnectionBodies.authenticationKeyLabel(type))
                .authenticationKeyRequired(authRequired)
                .authenticationKeyProvided(conn.getZSideAuthenticationKey() != null)
                .vlanTagRequired(true)
                .peeringTypeRequired(type == CloudProviderType.AZURE)
                .build();
    }

    private static String deferredNote(PlannedConnection conn, ConnectionInputRequirement requirement) {
        StringBuilder sb = new StringBuilder();
        sb.append("Connection '").append(conn.getName()).append("'");
        if (conn.getZSideProviderLabel() != null) {
            sb.append(" (").append(conn.getZSideProviderLabel()).append(")");
        }
        sb.append(": endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router '")
                .append(conn.getASideRouterName())
                .append("' exists");
        String needs = requirementNeeds(requirement);
        if (!needs.isEmpty()) {
            sb.append(" — you will need to supply ").append(needs);
        }
        return sb.toString();
    }

    private static String requirementNeeds(ConnectionInputRequirement requirement) {
        StringBuilder needs = new StringBuilder();
        if (requirement.isAuthenticationKeyRequired() && !requirement.isAuthenticationKeyProvided()) {
            needs.append(requirement.getAuthenticationKeyLabel());
        }
        if (requirement.isVlanTagRequired()) {
            if (needs.length() > 0) needs.append(", ");
            needs.append("a VLAN tag");
        }
        if (requirement.isPeeringTypeRequired()) {
            if (needs.length() > 0) needs.append(", ");
            needs.append("an Azure peering type");
        }
        return needs.toString();
    }

    // ══════════════════════════════════════════════
    //  Live-failure classification (shared)
    // ══════════════════════════════════════════════

    /** Whether a live-layer failure is a genuine rejection (a plan defect) or an infeasibility (a skip). */
    public enum LiveFailureKind {
        /** A genuine API rejection of a well-formed request — the plan is wrong; record an ERROR. */
        REJECTION,
        /** Not entitled / not authed / throttled / server-side / transport — record a SKIPPED(reason). */
        INFEASIBLE
    }

    /**
     * The classification of a single live-layer failure. For an {@link LiveFailureKind#INFEASIBLE}
     * failure it also carries a human/LLM-readable {@link #reason() reason} fragment describing why the
     * validation could not be attempted; for a {@link LiveFailureKind#REJECTION} the caller uses the
     * exception's own message, so the reason is {@code null}.
     */
    public static final class LiveFailure {
        private final LiveFailureKind kind;
        private final String reason;

        private LiveFailure(LiveFailureKind kind, String reason) {
            this.kind = kind;
            this.reason = reason;
        }

        /** @return {@code true} when this failure is an infeasibility (skip), not a rejection. */
        public boolean isInfeasible() {
            return kind == LiveFailureKind.INFEASIBLE;
        }

        /** @return {@code true} when this failure is a genuine rejection (a plan defect). */
        public boolean isRejection() {
            return kind == LiveFailureKind.REJECTION;
        }

        /** @return the classification. */
        public LiveFailureKind kind() {
            return kind;
        }

        /** @return the infeasibility reason fragment, or {@code null} for a rejection. */
        public String reason() {
            return reason;
        }
    }

    /**
     * Classifies a failure thrown from a live validation step (a router or connection dry-run) as either
     * a genuine <em>rejection</em> of a well-formed request — a real plan defect that must become an
     * ERROR — or an <em>infeasibility</em> that must become a SKIPPED(reason) and never invalidate an
     * otherwise-sound plan.
     *
     * <ul>
     *   <li><b>Infeasibility (SKIPPED):</b> {@link EquinixAuthenticationException} (401, not authed),
     *       {@link EquinixAuthorizationException} (403, not entitled — the brand-new-customer case),
     *       {@link EquinixRateLimitException} (429, throttled), {@link EquinixServerException} (5xx,
     *       server-side), and any transport/timeout/connect error (a non-service {@code RuntimeException}
     *       or SDK client error). A base {@link EquinixServiceException} with no status is also treated
     *       as infeasible rather than risk invalidating a sound plan on an ambiguous error.</li>
     *   <li><b>Rejection (ERROR):</b> an {@link EquinixServiceException} carrying a 4xx status that is a
     *       validation rejection (e.g. HTTP 400/409) — the API rejected a well-formed request because the
     *       request itself is wrong.</li>
     * </ul>
     *
     * @param e the failure thrown from the live step; {@code null} tolerated (treated as infeasible)
     * @return the classification, with an infeasibility reason when applicable
     */
    public static LiveFailure classifyLiveFailure(Throwable e) {
        if (e == null) {
            return infeasible("the live validation could not be attempted (no detail)");
        }
        // Typed infeasibility subclasses first — these must never be read as a rejection even though
        // 401/403/429 are themselves 4xx statuses.
        if (e instanceof EquinixAuthorizationException) {
            return infeasible("the credential is not entitled to this operation (HTTP 403)");
        }
        if (e instanceof EquinixAuthenticationException) {
            return infeasible("the credential is not authenticated for this operation (HTTP 401)");
        }
        if (e instanceof EquinixRateLimitException) {
            return infeasible("the Equinix API is rate-limiting requests (HTTP 429)");
        }
        if (e instanceof EquinixServerException server) {
            return infeasible("the Equinix API returned a server error (HTTP "
                    + statusOr(server, "5xx") + ")");
        }
        if (e instanceof EquinixServiceException service) {
            Integer status = service.getStatusCode();
            if (status != null && status >= 500) {
                return infeasible("the Equinix API returned a server error (HTTP " + status + ")");
            }
            if (status != null && status >= 400) {
                // A 4xx that is not 401/403/429 — a genuine validation rejection of a well-formed request.
                return rejection();
            }
            // No status to confirm a validation rejection: prefer a skip over invalidating a sound plan.
            return infeasible("the Equinix API could not complete the request (service error)");
        }
        // Anything else — a transport/timeout/connect error, a circuit-open, or an SDK client error.
        return infeasible("Equinix API unreachable (" + transportReason(e) + ")");
    }

    private static LiveFailure infeasible(String reason) {
        return new LiveFailure(LiveFailureKind.INFEASIBLE, reason);
    }

    private static LiveFailure rejection() {
        return new LiveFailure(LiveFailureKind.REJECTION, null);
    }

    private static String statusOr(EquinixServiceException service, String fallback) {
        Integer status = service.getStatusCode();
        return status != null ? status.toString() : fallback;
    }

    /**
     * A short cause label for a transport-layer failure — {@code "timeout"}, {@code "connection failed"},
     * or the raw message — walking the cause chain so a wrapped {@code SocketTimeoutException} is still
     * recognised.
     */
    private static String transportReason(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage() == null ? "" : t.getMessage().toLowerCase(Locale.ROOT);
            String type = t.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            if (message.contains("timeout") || message.contains("timed out") || type.contains("timeout")) {
                return "timeout";
            }
            if (message.contains("connect")) {
                return "connection failed";
            }
        }
        String message = e.getMessage();
        return message != null && !message.isBlank() ? message : e.getClass().getSimpleName();
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private static <T> List<T> nz(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private static void increment(Map<String, Integer> counts, String key) {
        if (key != null) {
            counts.merge(key, 1, Integer::sum);
        }
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * The /30 network base of a {@code "10.100.0.1/30"}-style address, or {@code null} if unparseable.
     * The last two host bits are masked off so both ends of a peering pair map to the same base.
     */
    private static String base30(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return null;
        }
        try {
            String ip = cidr.contains("/") ? cidr.substring(0, cidr.indexOf('/')) : cidr;
            String[] octets = ip.split("\\.");
            if (octets.length != 4) {
                return null;
            }
            long value = 0;
            for (String octet : octets) {
                int o = Integer.parseInt(octet.trim());
                if (o < 0 || o > 255) {
                    return null;
                }
                value = (value << 8) | o;
            }
            long base = value & 0xFFFFFFFCL;
            return ((base >> 24) & 0xFF) + "." + ((base >> 16) & 0xFF) + "."
                    + ((base >> 8) & 0xFF) + "." + (base & 0xFF);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static <T> T safeGet(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The outcome of a single service-profile lookup: resolved, definitively absent, or unreachable. */
    private enum Status { RESOLVED, NOT_FOUND, UNAVAILABLE }

    private static final class Lookup {
        final Status status;
        final ServiceProfile profile;

        Lookup(Status status, ServiceProfile profile) {
            this.status = status;
            this.profile = profile;
        }
    }

    /**
     * Resolves a service profile by uuid, distinguishing a genuine 404 (a definitively unknown
     * profile — a hard error) from any other failure or a missing stub (unreachable — skipped).
     */
    private static Lookup lookupProfile(ServiceProfiles client, String uuid) {
        try {
            ServiceProfile profile = client.getByUuid(uuid);
            return profile != null
                    ? new Lookup(Status.RESOLVED, profile)
                    : new Lookup(Status.UNAVAILABLE, null);
        } catch (EquinixNotFoundException notFound) {
            return new Lookup(Status.NOT_FOUND, null);
        } catch (RuntimeException e) {
            return new Lookup(Status.UNAVAILABLE, null);
        }
    }
}
