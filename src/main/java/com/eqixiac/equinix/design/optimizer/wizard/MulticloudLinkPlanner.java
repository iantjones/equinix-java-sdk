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

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.MulticloudEnvironmentStatus;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.model.OptimizationRequest;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.ProviderRequirement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadSpec;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionBodies;
import com.eqixiac.equinix.design.optimizer.wizard.model.MulticloudLinkPricing;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect.EquinixLeg;
import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.MulticloudPathComparison;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Plans and prices the native multicloud links of a deployment plan. <b>Beta.</b>
 *
 * <p>Detection: a workload that depends on two or more well-known clouds implies one flow per pair
 * of those clouds. Flows are grouped by (placement metro, unordered cloud pair); a group's
 * bandwidth is the sum of its workloads' effective bandwidths in Mbps.</p>
 *
 * <p>Region matching: for each side the candidate regions are, in order, the seller region of the
 * plan's connection to that cloud at the metro, the dependency's {@code preferredSellerRegions},
 * and the other seller regions the metro advertises for the cloud. The first candidate pair the
 * catalog holds an environment for is used.</p>
 *
 * <p>Replacement: see {@link CloudToCloudStrategy}. A connection is omitted whole or kept
 * unchanged; it is never resized.</p>
 *
 * <p>Nothing here performs I/O beyond what the configured {@link RateCard} does when asked for a
 * Fabric connection or Cloud Router price. No cloud-provider API is called.</p>
 */
final class MulticloudLinkPlanner {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private MulticloudLinkPlanner() {}

    /** The provider connections to keep on the plan and the native links to carry, not yet priced. */
    static final class Planned {
        final List<PlannedConnection> providerConnections;
        final List<PlannedMulticloudInterconnect> links;

        Planned(List<PlannedConnection> providerConnections, List<PlannedMulticloudInterconnect> links) {
            this.providerConnections = providerConnections;
            this.links = links;
        }
    }

    // ══════════════════════════════════════════════
    //  Planning
    // ══════════════════════════════════════════════

    /**
     * Applies the configured {@link CloudToCloudStrategy}. Under {@code EQUINIX_ONLY} the catalog
     * is not read and the input connection list is returned as is with no links.
     */
    static Planned plan(DeploymentWizard.Builder config, OptimizationResult result,
                        List<MetroRecommendation> metros, List<PlannedConnection> connections) {

        CloudToCloudStrategy strategy = config.getCloudToCloudStrategy();
        OptimizationRequest request = result.getRequest();
        if (strategy == CloudToCloudStrategy.EQUINIX_ONLY || request == null
                || request.getWorkloads() == null || request.getWorkloads().isEmpty()) {
            return new Planned(connections, Collections.emptyList());
        }

        List<Flow> flows = detectFlows(request, result);
        if (flows.isEmpty()) {
            return new Planned(connections, Collections.emptyList());
        }
        MulticloudEnvironmentCatalog catalog = resolveCatalog(config, request);

        // Match every flow first: whether a workload is fully covered natively depends on all of
        // its flows, so the replacement decision needs the complete picture.
        for (Flow flow : flows) {
            matchEnvironment(flow, catalog, metros, connections);
        }

        Set<String> omitted = new LinkedHashSet<>();
        boolean mayReplace = strategy == CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE
                || strategy == CloudToCloudStrategy.NATIVE_ONLY;
        if (mayReplace) {
            for (Flow flow : flows) {
                if (!flow.usable()) {
                    continue;
                }
                for (CloudProviderType cloud : List.of(flow.a, flow.z)) {
                    PlannedConnection conn = connectionTo(connections, flow.metro, cloud);
                    if (conn == null) {
                        continue;
                    }
                    List<String> keep = keepReasons(conn, cloud, request, result, connections, flows);
                    if (keep.isEmpty()) {
                        omitted.add(conn.getName());
                        flow.replaced.add(conn.getName());
                    } else {
                        for (String reason : keep) {
                            flow.reasoning.add("Kept " + conn.getName() + ": " + reason + ".");
                        }
                    }
                }
            }
        }

        List<PlannedConnection> kept = new ArrayList<>();
        for (PlannedConnection conn : connections) {
            if (!omitted.contains(conn.getName())) {
                kept.add(conn);
            }
        }

        List<PlannedMulticloudInterconnect> links = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        for (Flow flow : flows) {
            if (flow.environment == null && strategy != CloudToCloudStrategy.NATIVE_ONLY) {
                continue; // nothing to compare against; the Equinix path is planned as usual
            }
            links.add(toLink(flow, strategy, config, connections, omitted, names));
        }
        return new Planned(kept, links);
    }

    /** One cloud-to-cloud flow: the workloads at one metro that depend on the same two clouds. */
    private static final class Flow {
        final MetroId metro;
        final CloudProviderType a;
        final CloudProviderType z;
        final List<WorkloadSpec> workloads = new ArrayList<>();
        final Set<String> preferredA = new LinkedHashSet<>();
        final Set<String> preferredZ = new LinkedHashSet<>();
        final List<String> reasoning = new ArrayList<>();
        final List<String> replaced = new ArrayList<>();
        int requestedMbps;
        String regionA;
        String regionZ;
        MulticloudEnvironment environment;
        Integer coveringTierMbps;

        Flow(MetroId metro, CloudProviderType a, CloudProviderType z) {
            this.metro = metro;
            this.a = a;
            this.z = z;
        }

        /** A GA environment that lists a size covering the request. */
        boolean usable() {
            return environment != null
                    && environment.getStatus() == MulticloudEnvironmentStatus.GA
                    && coveringTierMbps != null;
        }
    }

    private static List<Flow> detectFlows(OptimizationRequest request, OptimizationResult result) {
        Map<String, Flow> flows = new LinkedHashMap<>();
        for (WorkloadSpec workload : request.getWorkloads()) {
            Map<CloudProviderType, ProviderRequirement> clouds = cloudsOf(workload);
            if (clouds.size() < 2) {
                continue;
            }
            MetroId metro = placementOf(result, workload.getLabel());
            List<CloudProviderType> ordered = new ArrayList<>(clouds.keySet());
            ordered.sort(Comparator.comparing(CloudProviderType::shortCode));
            for (int i = 0; i < ordered.size(); i++) {
                for (int j = i + 1; j < ordered.size(); j++) {
                    CloudProviderType a = ordered.get(i);
                    CloudProviderType z = ordered.get(j);
                    String key = (metro == null ? "-" : metro.code()) + "|" + a + "|" + z;
                    Flow flow = flows.computeIfAbsent(key, k -> new Flow(metro, a, z));
                    flow.workloads.add(workload);
                    flow.requestedMbps += flowBandwidth(workload);
                    addAll(flow.preferredA, clouds.get(a).getPreferredSellerRegions());
                    addAll(flow.preferredZ, clouds.get(z).getPreferredSellerRegions());
                }
            }
        }
        return new ArrayList<>(flows.values());
    }

    /** The well-known clouds a workload depends on, in declaration order, each with its requirement. */
    private static Map<CloudProviderType, ProviderRequirement> cloudsOf(WorkloadSpec workload) {
        Map<CloudProviderType, ProviderRequirement> clouds = new LinkedHashMap<>();
        if (workload.getDependsOnProviders() != null) {
            for (ProviderRequirement dep : workload.getDependsOnProviders()) {
                CloudProviderType cloud = cloudOf(dep);
                if (cloud != null) {
                    clouds.putIfAbsent(cloud, dep);
                }
            }
        }
        return clouds;
    }

    /** The well-known cloud a requirement names, or {@code null} for a third-party service profile. */
    private static CloudProviderType cloudOf(ProviderRequirement dep) {
        if (dep == null) {
            return null;
        }
        CloudProviderType cloud = dep.getCloudProvider() != null
                ? dep.getCloudProvider()
                : ConnectionBodies.resolveCloudType(dep.displayLabel());
        return cloud == null || cloud == CloudProviderType.OTHER ? null : cloud;
    }

    private static boolean dependsOnlyOnClouds(WorkloadSpec workload) {
        if (workload.getDependsOnProviders() == null) {
            return true;
        }
        for (ProviderRequirement dep : workload.getDependsOnProviders()) {
            if (cloudOf(dep) == null) {
                return false;
            }
        }
        return true;
    }

    /** Same sizing rule the wizard applies to a provider connection: the effective bandwidth, or 1000 Mbps when none is declared. */
    private static int flowBandwidth(WorkloadSpec workload) {
        int effective = DeploymentWizardEngine.effectiveWorkloadBandwidth(workload);
        return effective > 0 ? effective : 1000;
    }

    private static MetroId placementOf(OptimizationResult result, String workloadLabel) {
        if (result.getTopology() == null || result.getTopology().getPlacements() == null || workloadLabel == null) {
            return null;
        }
        for (WorkloadPlacement placement : result.getTopology().getPlacements()) {
            if (workloadLabel.equals(placement.getWorkloadLabel())) {
                return placement.getAssignedMetro();
            }
        }
        return null;
    }

    private static MulticloudEnvironmentCatalog resolveCatalog(DeploymentWizard.Builder config,
                                                               OptimizationRequest request) {
        if (config.getMulticloudEnvironments() != null) {
            return config.getMulticloudEnvironments();
        }
        if (request.getMulticloudEnvironments() != null) {
            return request.getMulticloudEnvironments();
        }
        return MulticloudEnvironmentCatalog.standard();
    }

    private static void matchEnvironment(Flow flow, MulticloudEnvironmentCatalog catalog,
                                         List<MetroRecommendation> metros, List<PlannedConnection> connections) {
        List<String> regionsA = candidateRegions(flow.a, flow.metro, flow.preferredA, metros, connections);
        List<String> regionsZ = candidateRegions(flow.z, flow.metro, flow.preferredZ, metros, connections);
        flow.regionA = regionsA.isEmpty() ? null : regionsA.get(0);
        flow.regionZ = regionsZ.isEmpty() ? null : regionsZ.get(0);

        for (String regionA : regionsA) {
            for (String regionZ : regionsZ) {
                Optional<MulticloudEnvironment> match = catalog.find(flow.a, regionA, flow.z, regionZ);
                if (match.isPresent()) {
                    flow.environment = match.get();
                    flow.regionA = regionA;
                    flow.regionZ = regionZ;
                    OptionalInt tier = match.get().sizesMbps().coveringTier(flow.requestedMbps);
                    flow.coveringTierMbps = tier.isPresent() ? tier.getAsInt() : null;
                    flow.reasoning.add("Catalog environment " + match.get().describe() + " matches the planned regions.");
                    if (tier.isPresent() && tier.getAsInt() > flow.requestedMbps) {
                        flow.reasoning.add("Requested " + flow.requestedMbps + " Mbps rounded up to the smallest listed "
                                + "size " + tier.getAsInt() + " Mbps (listed sizes: " + match.get().sizesMbps() + ")."
                                + (match.get().getSizesNote() != null ? " " + match.get().getSizesNote() : ""));
                    } else if (tier.isEmpty()) {
                        flow.reasoning.add("No listed size covers " + flow.requestedMbps + " Mbps (listed sizes: "
                                + match.get().sizesMbps() + ")."
                                + (match.get().getSizesNote() != null ? " " + match.get().getSizesNote() : ""));
                    }
                    if (match.get().getStatus() != MulticloudEnvironmentStatus.GA) {
                        flow.reasoning.add("The environment's status is " + match.get().getStatus()
                                + ", so it replaces no Equinix connection."
                                + (match.get().getNote() != null ? " " + match.get().getNote() : ""));
                    }
                    return;
                }
            }
        }
        flow.reasoning.add("The catalog" + (catalog.asOf() != null ? " (as of " + catalog.asOf() + ")" : "")
                + " has no environment for " + flow.a + " " + describeRegions(regionsA) + " <-> "
                + flow.z + " " + describeRegions(regionsZ) + ". Known region pairs for these clouds: "
                + describePairs(catalog.regionsFor(flow.a, flow.z)) + ".");
    }

    private static String describeRegions(List<String> regions) {
        return regions.isEmpty() ? "(no region on the plan)" : regions.toString();
    }

    private static String describePairs(List<MulticloudEnvironment> environments) {
        if (environments.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        for (MulticloudEnvironment environment : environments) {
            parts.add(environment.describe());
        }
        return String.join("; ", parts);
    }

    private static List<String> candidateRegions(CloudProviderType cloud, MetroId metro, Set<String> preferred,
                                                 List<MetroRecommendation> metros,
                                                 List<PlannedConnection> connections) {
        Set<String> regions = new LinkedHashSet<>();
        PlannedConnection conn = connectionTo(connections, metro, cloud);
        if (conn != null && conn.getZSideSellerRegion() != null && !conn.getZSideSellerRegion().isBlank()) {
            regions.add(conn.getZSideSellerRegion().trim());
        }
        regions.addAll(preferred);
        if (metro != null && metros != null) {
            for (MetroRecommendation rec : metros) {
                if (!metro.equals(rec.getMetroId()) || rec.getAvailableProviders() == null) {
                    continue;
                }
                for (ProviderAvailability availability : rec.getAvailableProviders()) {
                    if (availability.isAvailable()
                            && ConnectionBodies.resolveCloudType(availability.getProviderLabel()) == cloud) {
                        addAll(regions, availability.getSellerRegions());
                    }
                }
            }
        }
        return new ArrayList<>(regions);
    }

    private static void addAll(Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim());
            }
        }
    }

    /** The plan's provider connection from the metro's Cloud Router to the cloud, or {@code null}. */
    private static PlannedConnection connectionTo(List<PlannedConnection> connections, MetroId metro,
                                                  CloudProviderType cloud) {
        if (metro == null || connections == null) {
            return null;
        }
        for (PlannedConnection conn : connections) {
            if (metro.equals(conn.getASideMetro()) && cloudTypeOf(conn) == cloud) {
                return conn;
            }
        }
        return null;
    }

    private static CloudProviderType cloudTypeOf(PlannedConnection conn) {
        return conn.getZSideCloudType() != null
                ? conn.getZSideCloudType()
                : ConnectionBodies.resolveCloudType(conn.getZSideProviderLabel());
    }

    /**
     * The reasons the Cloud Router to cloud connection must stay on the plan; empty when the
     * replacement rule allows omitting it. Every consumer other than fully covered cloud-to-cloud
     * workloads is a reason.
     */
    private static List<String> keepReasons(PlannedConnection conn, CloudProviderType cloud,
                                            OptimizationRequest request, OptimizationResult result,
                                            List<PlannedConnection> connections, List<Flow> flows) {
        List<String> reasons = new ArrayList<>();

        if (request.getSites() != null && !request.getSites().isEmpty()) {
            reasons.add("the request declares " + request.getSites().size() + " user site(s); the wizard does "
                    + "not model which site reaches which cloud, so every site is assumed to reach " + cloud
                    + " through " + conn.getASideMetro());
        }
        if (request.getProviders() != null) {
            for (ProviderRequirement requirement : request.getProviders()) {
                if (cloudOf(requirement) == cloud) {
                    reasons.add(cloud + " is a request-level provider requirement ('" + requirement.displayLabel()
                            + "'), which asks for it to be reachable from the recommended metros");
                    break;
                }
            }
        }

        Map<String, Integer> perWorkload = conn.getBandwidthAllocation() != null
                ? conn.getBandwidthAllocation().getPerWorkload() : null;
        if (perWorkload != null && perWorkload.containsKey("custom")) {
            reasons.add("its bandwidth was set explicitly by the custom bandwidth map");
        }

        for (WorkloadSpec workload : request.getWorkloads()) {
            MetroId placedAt = placementOf(result, workload.getLabel());
            boolean sizedOntoConnection = perWorkload != null && perWorkload.containsKey(workload.getLabel());
            boolean dependsOnCloud = cloudsOf(workload).containsKey(cloud);
            boolean reachesThroughThisMetro = dependsOnCloud
                    && (placedAt == null
                        || placedAt.equals(conn.getASideMetro())
                        || connectionTo(connections, placedAt, cloud) == null);
            if (!sizedOntoConnection && !reachesThroughThisMetro) {
                continue;
            }
            String uncovered = uncoveredReason(workload, cloud, placedAt, flows);
            if (uncovered != null) {
                reasons.add("workload '" + workload.getLabel() + "' uses it and " + uncovered);
            }
        }
        return reasons;
    }

    /**
     * Why a workload still needs the Equinix connection to {@code cloud}, or {@code null} when
     * every one of its flows involving the cloud has a usable native environment.
     */
    private static String uncoveredReason(WorkloadSpec workload, CloudProviderType cloud, MetroId placedAt,
                                          List<Flow> flows) {
        Map<CloudProviderType, ProviderRequirement> clouds = cloudsOf(workload);
        if (!clouds.containsKey(cloud)) {
            return "is sized onto it without depending on a second cloud";
        }
        if (!dependsOnlyOnClouds(workload)) {
            return "also depends on a provider that is not a well-known cloud, which it reaches through the Cloud Router";
        }
        if (clouds.size() < 2) {
            return "depends on " + cloud + " only, so it has no cloud-to-cloud flow a native link could carry";
        }
        for (CloudProviderType other : clouds.keySet()) {
            if (other == cloud) {
                continue;
            }
            Flow flow = flowFor(flows, placedAt, cloud, other);
            if (flow == null || !flow.usable()) {
                return "its flow " + cloud + " <-> " + other + " has no GA catalog environment with a covering size";
            }
        }
        return null;
    }

    private static Flow flowFor(List<Flow> flows, MetroId metro, CloudProviderType one, CloudProviderType other) {
        for (Flow flow : flows) {
            boolean sameMetro = metro == null ? flow.metro == null : metro.equals(flow.metro);
            boolean samePair = (flow.a == one && flow.z == other) || (flow.a == other && flow.z == one);
            if (sameMetro && samePair) {
                return flow;
            }
        }
        return null;
    }

    private static PlannedMulticloudInterconnect toLink(Flow flow, CloudToCloudStrategy strategy,
                                                        DeploymentWizard.Builder config,
                                                        List<PlannedConnection> originalConnections,
                                                        Set<String> omitted, Set<String> names) {
        MulticloudLinkRole role;
        if (!flow.replaced.isEmpty()) {
            role = MulticloudLinkRole.REPLACEMENT;
        } else if (strategy == CloudToCloudStrategy.NATIVE_ONLY && !flow.usable()) {
            role = MulticloudLinkRole.UNAVAILABLE;
        } else {
            role = MulticloudLinkRole.ALTERNATIVE;
        }

        List<String> workloadLabels = new ArrayList<>();
        for (WorkloadSpec workload : flow.workloads) {
            workloadLabels.add(workload.getLabel());
        }

        List<EquinixLeg> legs = new ArrayList<>();
        List<String> onPlan = new ArrayList<>();
        for (CloudProviderType cloud : List.of(flow.a, flow.z)) {
            PlannedConnection conn = connectionTo(originalConnections, flow.metro, cloud);
            if (conn == null) {
                continue;
            }
            Map<String, Integer> perWorkload = conn.getBandwidthAllocation() != null
                    ? conn.getBandwidthAllocation().getPerWorkload() : null;
            boolean shared = perWorkload == null || !workloadLabels.containsAll(perWorkload.keySet());
            boolean kept = !omitted.contains(conn.getName());
            legs.add(EquinixLeg.builder()
                    .cloud(cloud)
                    .connectionName(conn.getName())
                    .connectionType(conn.getConnectionType())
                    .pricedMbps(shared ? flow.requestedMbps : conn.getBandwidthMbps())
                    .sharedWithOtherWorkloads(shared)
                    .onPlan(kept)
                    .build());
            if (kept) {
                onPlan.add(conn.getName());
            }
        }

        List<String> reasoning = new ArrayList<>(flow.reasoning);
        if (role == MulticloudLinkRole.REPLACEMENT) {
            reasoning.add("Omitted " + flow.replaced + ": the replacement rule found no other consumer. No routing "
                    + "protocol or /30 subnet is planned for an omitted connection or for the native link.");
            boolean routerStillUsed = false;
            for (PlannedConnection conn : originalConnections) {
                if (flow.metro.equals(conn.getASideMetro()) && !omitted.contains(conn.getName())) {
                    routerStillUsed = true;
                }
            }
            if (!routerStillUsed) {
                reasoning.add("The Cloud Router at " + flow.metro + " is still planned and priced (the wizard plans "
                        + "one per recommended metro) and has no provider connection left on this plan. Review "
                        + "whether it is needed before executing.");
            }
        } else if (role == MulticloudLinkRole.ALTERNATIVE && strategy == CloudToCloudStrategy.COMPARE) {
            reasoning.add("Strategy COMPARE: the Equinix connections " + onPlan + " are planned unchanged.");
        }

        String base = flow.a.shortCode() + "-" + flow.z.shortCode() + (flow.metro != null ? "-" + flow.metro.code() : "");
        String name = base;
        for (int suffix = 2; !names.add(name); suffix++) {
            name = base + "-" + suffix;
        }

        return PlannedMulticloudInterconnect.builder()
                .name(name)
                .providerA(flow.a).regionA(flow.regionA)
                .providerZ(flow.z).regionZ(flow.regionZ)
                .environment(flow.environment)
                .role(role)
                .strategy(strategy)
                .metro(flow.metro)
                .requestedMbps(flow.requestedMbps)
                .coveringTierMbps(flow.coveringTierMbps)
                .workloadLabels(Collections.unmodifiableList(workloadLabels))
                .equinixLegs(Collections.unmodifiableList(legs))
                .equinixConnectionNames(Collections.unmodifiableList(onPlan))
                .replacedConnectionNames(List.copyOf(flow.replaced))
                .reasoning(Collections.unmodifiableList(reasoning))
                .createThenAcceptRecipe(recipeFor(flow.a, flow.z, flow.environment))
                .build();
    }

    // ══════════════════════════════════════════════
    //  Create-then-accept steps (from provider documentation, retrieved 2026-09-21)
    // ══════════════════════════════════════════════

    private static final String AWS_GETTING_STARTED =
            "https://docs.aws.amazon.com/interconnect/latest/userguide/getting-started-multicloud.html";

    static List<String> recipeFor(CloudProviderType a, CloudProviderType z, MulticloudEnvironment environment) {
        if (environment == null) {
            return List.of();
        }
        boolean aws = a == CloudProviderType.AWS || z == CloudProviderType.AWS;
        CloudProviderType peer = a == CloudProviderType.AWS ? z : a;
        if (!aws) {
            return List.of();
        }
        if (peer == CloudProviderType.GOOGLE_CLOUD) {
            return List.of(
                    "AWS Direct Connect console, AWS Interconnect, Create new multicloud Interconnect: select Google "
                            + "Cloud, the AWS Region and the Google Cloud region, the bandwidth and a Direct Connect "
                            + "gateway as the attach point, and enter the Google Cloud project ID (" + AWS_GETTING_STARTED + ").",
                    "AWS displays an activation key.",
                    "Google Cloud: create the Partner Cross-Cloud Interconnect transport with that key (gcloud "
                            + "network-connectivity transports create, flag --activation-key; "
                            + "https://docs.cloud.google.com/sdk/gcloud/reference/network-connectivity/transports/create).",
                    "The flow can start on the Google Cloud side instead: Google Cloud issues the key and AWS accepts it "
                            + "under AWS Interconnect, Accept multicloud Interconnect.");
        }
        if (peer == CloudProviderType.ORACLE_CLOUD) {
            return List.of(
                    "AWS Direct Connect console, AWS Interconnect, Create new multicloud Interconnect: select Oracle "
                            + "Cloud Infrastructure, the Regions, the bandwidth and a Direct Connect gateway, and enter "
                            + "the tenancy OCID (format ocid1.tenancy.oc1..<unique_ID>) (" + AWS_GETTING_STARTED + ").",
                    "AWS displays an activation key.",
                    "OCI: create a FastConnect interconnect virtual circuit and enter the AWS activation key in the "
                            + "Service key field (https://docs.oracle.com/en-us/iaas/Content/multicloud/interconnect-aws.htm).");
        }
        if (peer == CloudProviderType.AZURE) {
            return List.of(
                    "Preview. AWS states that completing the activation with a provider in public preview can require "
                            + "the CLI (" + AWS_GETTING_STARTED + "). No Azure-side procedure was verified; follow the "
                            + "providers' preview documentation.");
        }
        return List.of();
    }

    // ══════════════════════════════════════════════
    //  Pricing
    // ══════════════════════════════════════════════

    /**
     * Returns the links with {@code pricing} and {@code recommendation} resolved against the
     * given rate card and the builder's term and path tier. The input list and its elements are
     * unchanged.
     *
     * <p>Native link and per-GB lookups use {@code configured} first and the bundled
     * {@link ReferenceRateCard} second, because the wizard's default card (live Equinix pricing)
     * holds no cloud-provider figures. Fabric connection and Cloud Router prices come from
     * {@code configured} through the same methods as the plan's own pricing, including its
     * heuristic fallback. The caller passes the one card instance it prices the plan with
     * ({@code DeploymentWizardEngine.resolveRateCard(config)}), so a live card's catalogue fetch
     * and cache are shared with the plan's pricing rather than repeated per call.</p>
     *
     * @param configured the resolved rate card, or {@code null} when the wizard has neither an
     *                   explicit card nor a gateway (the heuristic fallback then prices the
     *                   Equinix components)
     */
    static List<PlannedMulticloudInterconnect> price(DeploymentWizard.Builder config,
                                                     RateCard configured,
                                                     List<PlannedMulticloudInterconnect> links,
                                                     List<PlannedCloudRouter> routers,
                                                     List<PlannedConnection> providerConnections,
                                                     List<PlannedBackboneLink> backboneLinks,
                                                     OptimizationRequest request) {
        if (links == null || links.isEmpty()) {
            return links == null ? Collections.emptyList() : links;
        }
        ReferenceRateCard reference = ReferenceRateCard.standard();
        RateCard chain = configured == null ? reference : RateCard.layered(configured, reference);

        List<PlannedMulticloudInterconnect> priced = new ArrayList<>(links.size());
        for (PlannedMulticloudInterconnect link : links) {
            if (link.getEnvironment() == null) {
                priced.add(link.toBuilder()
                        .pricing(null)
                        .recommendation("No native link is planned: the catalog has no environment for "
                                + link.getProviderA() + " <-> " + link.getProviderZ() + " in the planned regions.")
                        .build());
                continue;
            }
            MulticloudLinkPricing pricing = priceLink(link, config, configured, chain, reference,
                    routers, providerConnections, backboneLinks, request);
            priced.add(link.toBuilder()
                    .pricing(pricing)
                    .recommendation(recommend(link, pricing))
                    .build());
        }
        return Collections.unmodifiableList(priced);
    }

    private static MulticloudLinkPricing priceLink(PlannedMulticloudInterconnect link,
                                                   DeploymentWizard.Builder config, RateCard configured,
                                                   RateCard chain, ReferenceRateCard reference,
                                                   List<PlannedCloudRouter> routers,
                                                   List<PlannedConnection> providerConnections,
                                                   List<PlannedBackboneLink> backboneLinks,
                                                   OptimizationRequest request) {
        Term term = config.getTerm();
        List<String> notes = new ArrayList<>();

        // ── Native link: priced at the covering size, exact match, no interpolation ──
        MulticloudLinkQuote quote = null;
        BigDecimal nativeMonthly = null;
        String nativeCurrency = null;
        if (link.getCoveringTierMbps() == null) {
            notes.add("Native link unpriced: the environment lists no size covering " + link.getRequestedMbps() + " Mbps.");
        } else {
            MulticloudLinkRequest linkRequest = MulticloudLinkRequest.builder()
                    .providerA(link.getProviderA()).regionA(link.getRegionA())
                    .providerZ(link.getProviderZ()).regionZ(link.getRegionZ())
                    .bandwidthMbps(link.getCoveringTierMbps())
                    .pathTier(config.getMulticloudPathTier())
                    .term(term)
                    .build();
            quote = chain.multicloudLink(linkRequest).orElse(null);
            if (quote == null) {
                notes.add("Native link unpriced: no rate card in the chain holds a price for " + link.getProviderA()
                        + " <-> " + link.getProviderZ() + ".");
            } else {
                nativeMonthly = quote.combinedMonthly().map(MulticloudLinkPlanner::cents).orElse(null);
                nativeCurrency = quote.combinedCurrency().orElse(null);
                if (!quote.isFullyPriced()) {
                    // Each side's reason is on the quote (getSideAUnpricedReason / getSideZUnpricedReason).
                    notes.add("Native link not fully priced: " + unpricedSides(quote) + ". The reason for each "
                            + "unpriced side is on the native quote.");
                }
                if (quote.isMixedCurrency()) {
                    notes.add("Native link sides are in different currencies (" + quote.monthlySubtotalsByCurrency()
                            + "); no combined figure is formed and no FX rate is applied.");
                }
                // The quote's own notes state the path-tier assumption and the 730 h conversion.
                notes.addAll(quote.getNotes());
            }
        }

        // ── Equinix path for the same flow ──
        List<String> components = new ArrayList<>();
        CurrencyReconciler equinix = CurrencyReconciler.create();
        boolean equinixComplete = true;
        Set<CloudProviderType> legClouds = new LinkedHashSet<>();
        for (EquinixLeg leg : link.equinixLegsOrEmpty()) {
            legClouds.add(leg.getCloud());
            ConnectionType type = leg.getConnectionType();
            int mbps = leg.getPricedMbps();
            if (leg.isOnPlan() && !leg.isSharedWithOtherWorkloads()) {
                // Follow the plan's current connection: a profile choice can change its billable tier.
                for (PlannedConnection conn : nz(providerConnections)) {
                    if (conn.getName() != null && conn.getName().equals(leg.getConnectionName())) {
                        mbps = conn.getBandwidthMbps();
                        type = conn.getConnectionType();
                    }
                }
            }
            PriceQuote vc = DeploymentWizardEngine.priceConnection(configured, type, mbps, link.getMetro(), term);
            equinix.add(vc.getCurrency(), vc.getMonthlyRecurring(), BigDecimal.ZERO);
            components.add("Equinix path: Fabric virtual connection to " + leg.getCloud() + " at " + mbps + " Mbps ("
                    + leg.getConnectionName() + (leg.isOnPlan() ? "" : ", omitted from the plan")
                    + (leg.isSharedWithOtherWorkloads() ? ", shared with other workloads; priced at this flow's bandwidth" : "")
                    + "): " + amount(vc.getMonthlyRecurring(), vc.getCurrency()) + " per month, " + vc.getSource() + ".");

            Optional<PriceQuote> port = reference.cspInterconnectPortMonthlyQuote(leg.getCloud(), mbps);
            if (port.isPresent()) {
                equinix.add(port.get().getCurrency(), port.get().getMonthlyRecurring(), BigDecimal.ZERO);
                components.add("Equinix path: " + leg.getCloud() + " interconnect port, billed by the cloud provider: "
                        + amount(port.get().getMonthlyRecurring(), port.get().getCurrency()) + " per month, "
                        + port.get().getSource() + " (" + port.get().getNote() + ", reference data as of "
                        + reference.asOf() + ").");
            } else {
                equinixComplete = false;
                notes.add("Equinix path cost withheld: no reference interconnect-port figure for " + leg.getCloud() + ".");
            }
        }
        for (CloudProviderType cloud : List.of(link.getProviderA(), link.getProviderZ())) {
            if (!legClouds.contains(cloud)) {
                equinixComplete = false;
                notes.add("Equinix path cost withheld: the plan has no connection to " + cloud
                        + (link.getMetro() != null ? " at " + link.getMetro() : "")
                        + ", so the Equinix path does not carry this flow in this plan.");
            }
        }
        PlannedCloudRouter router = routerAt(routers, link.getMetro());
        if (router != null) {
            String shared = routerSharedReason(link, providerConnections, backboneLinks, request);
            if (shared == null) {
                PriceQuote routerQuote = DeploymentWizardEngine.priceRouter(configured, router, term);
                equinix.add(routerQuote.getCurrency(), routerQuote.getMonthlyRecurring(), BigDecimal.ZERO);
                components.add("Equinix path: Cloud Router " + router.getName() + " (this flow is its only use): "
                        + amount(routerQuote.getMonthlyRecurring(), routerQuote.getCurrency()) + " per month, "
                        + routerQuote.getSource() + ".");
            } else {
                notes.add("Cloud Router " + router.getName() + " is not in the Equinix path cost: " + shared
                        + ". That understates the Equinix fixed cost and overstates the break-even rate.");
            }
        }
        BigDecimal equinixFixed = null;
        String equinixCurrency = null;
        if (equinixComplete && !equinix.isEmpty()) {
            if (equinix.isMixed() || equinix.sawUnknownCurrency()) {
                notes.add("Equinix path components span currencies (" + equinix.describeMonthlySubtotals()
                        + "); no single figure is formed and no FX rate is applied.");
            } else {
                equinixFixed = equinix.monthlyTotal().map(MulticloudLinkPlanner::cents).orElse(null);
                equinixCurrency = equinix.soleCurrency();
            }
        }

        // ── Per-GB rates on each path ──
        PerGbSum equinixPerGb = perGbSum(chain, link, EgressPath.PRIVATE, term, "private interconnect", notes);
        PerGbSum nativePerGb = perGbSum(chain, link, EgressPath.MULTICLOUD_INTERCONNECT, term, "native link", notes);
        // The currency of the four per-GB rates together: known only when all four share it.
        String perGbCurrency = null;
        if (equinixPerGb != null && nativePerGb != null) {
            if (CurrencyReconciler.sameKnownCurrency(equinixPerGb.currency, nativePerGb.currency)) {
                perGbCurrency = equinixPerGb.currency;
            } else {
                notes.add("The private-interconnect per-GB rates (" + equinixPerGb.currency + ") and the native-link "
                        + "per-GB rates (" + nativePerGb.currency + ") are not in one currency; no break-even is "
                        + "computed and no FX rate is applied.");
            }
        }

        // ── Break-even: one currency throughout, or none ──
        BigDecimal breakEven = null;
        boolean nativeCheaperAbove = false;
        if (nativeMonthly != null && equinixFixed != null && equinixPerGb != null && nativePerGb != null) {
            if (CurrencyReconciler.sameKnownCurrency(nativeCurrency, equinixCurrency)
                    && CurrencyReconciler.sameKnownCurrency(nativeCurrency, perGbCurrency)) {
                breakEven = MulticloudPathComparison.breakEvenSustainedMbps(
                        nativeMonthly, equinixFixed, equinixPerGb.sum, nativePerGb.sum).orElse(null);
                nativeCheaperAbove = breakEven != null
                        && MulticloudPathComparison.nativeCheaperAboveBreakEven(nativeMonthly, equinixFixed);
            } else {
                notes.add("Break-even not computed: the native fee (" + nativeCurrency + "), the Equinix path ("
                        + equinixCurrency + ") and the per-GB rates (" + perGbCurrency + ") are not in one currency.");
            }
        }

        return MulticloudLinkPricing.builder()
                .nativeQuote(quote)
                .nativeMonthly(nativeMonthly)
                .nativeCurrency(nativeCurrency)
                .equinixFixedMonthly(equinixFixed)
                .equinixCurrency(equinixCurrency)
                .equinixComponents(Collections.unmodifiableList(components))
                .equinixPerGb(equinixPerGb == null ? null : equinixPerGb.sum)
                .nativePerGb(nativePerGb == null ? null : nativePerGb.sum)
                .perGbCurrency(perGbCurrency)
                .breakEvenSustainedMbps(breakEven)
                .nativeCheaperAboveBreakEven(nativeCheaperAbove)
                .notes(Collections.unmodifiableList(notes))
                .build();
    }

    /** The two clouds' per-GB rates on one path, summed, with the ISO 4217 code they share ({@code null} when unknown). */
    private record PerGbSum(BigDecimal sum, String currency) {
    }

    /** The two clouds' per-GB rates on a path, summed; {@code null} (with a note) when either is unpublished or they differ in currency. */
    private static PerGbSum perGbSum(RateCard chain, PlannedMulticloudInterconnect link, EgressPath path,
                                     Term term, String label, List<String> notes) {
        Optional<EgressRate> a = chain.egress(link.getProviderA(), link.getRegionA(), path, term);
        Optional<EgressRate> z = chain.egress(link.getProviderZ(), link.getRegionZ(), path, term);
        if (a.isEmpty() || z.isEmpty()) {
            notes.add("No " + label + " per-GB rate for " + (a.isEmpty() ? link.getProviderA() : link.getProviderZ())
                    + "; the break-even rate is not computed.");
            return null;
        }
        if (CurrencyReconciler.knownDifferent(a.get().getCurrency(), z.get().getCurrency())) {
            notes.add("The " + label + " per-GB rates are in different currencies; they are not summed.");
            return null;
        }
        java.util.Currency currency = a.get().getCurrency() != null ? a.get().getCurrency() : z.get().getCurrency();
        return new PerGbSum(a.get().getPricePerGb().add(z.get().getPricePerGb()),
                currency == null ? null : currency.getCurrencyCode());
    }

    private static PlannedCloudRouter routerAt(List<PlannedCloudRouter> routers, MetroId metro) {
        if (metro == null || routers == null) {
            return null;
        }
        for (PlannedCloudRouter router : routers) {
            if (metro.equals(router.getMetroId())) {
                return router;
            }
        }
        return null;
    }

    /** Why the metro's Cloud Router is not attributable to this flow alone, or {@code null} when it is. */
    private static String routerSharedReason(PlannedMulticloudInterconnect link,
                                             List<PlannedConnection> providerConnections,
                                             List<PlannedBackboneLink> backboneLinks,
                                             OptimizationRequest request) {
        if (request != null && request.getSites() != null && !request.getSites().isEmpty()) {
            return "the request declares user sites, which reach the clouds through it";
        }
        for (PlannedBackboneLink backbone : nz(backboneLinks)) {
            if (link.getMetro().equals(backbone.getMetroA()) || link.getMetro().equals(backbone.getMetroZ())) {
                return "backbone link " + backbone.getName() + " also uses it";
            }
        }
        Set<String> legNames = new LinkedHashSet<>();
        for (EquinixLeg leg : link.equinixLegsOrEmpty()) {
            legNames.add(leg.getConnectionName());
            if (leg.isSharedWithOtherWorkloads()) {
                return "connection " + leg.getConnectionName() + " also serves other workloads";
            }
        }
        for (PlannedConnection conn : nz(providerConnections)) {
            if (link.getMetro().equals(conn.getASideMetro()) && !legNames.contains(conn.getName())) {
                return "connection " + conn.getName() + " also uses it";
            }
        }
        return null;
    }

    /** States which path costs less at which sustained rate, from the resolved figures only. */
    private static String recommend(PlannedMulticloudInterconnect link, MulticloudLinkPricing pricing) {
        StringBuilder sb = new StringBuilder();
        if (link.getEnvironment().getStatus() != MulticloudEnvironmentStatus.GA) {
            sb.append("The environment is ").append(link.getEnvironment().getStatus())
                    .append(link.getEnvironment().getAsOf() != null ? " as of " + link.getEnvironment().getAsOf() : "")
                    .append("; treat the native link as not available for production. ");
        }
        if (link.getRole() == MulticloudLinkRole.REPLACEMENT) {
            sb.append("The plan depends on this link in place of ").append(link.getReplacedConnectionNames()).append(". ");
        }
        if (pricing.getNativeMonthly() == null) {
            return sb.append("Cost comparison not possible: the native link is not fully priced (see the notes). "
                    + "Obtain the missing rate from the provider's pricing tool and supply it through "
                    + "CustomRateCard.Builder.multicloudLinkHourlyRate(...), then reprice the plan.").toString();
        }
        String nativeFee = amount(pricing.getNativeMonthly(), pricing.getNativeCurrency());
        if (pricing.getEquinixFixedMonthly() == null) {
            return sb.append("The native link costs ").append(nativeFee).append(" per month flat. The Equinix path's "
                    + "cost for the same flow could not be formed (see the notes), so no break-even is stated.").toString();
        }
        String equinixFee = amount(pricing.getEquinixFixedMonthly(), pricing.getEquinixCurrency());
        if (pricing.getBreakEvenSustainedMbps() != null) {
            BigDecimal breakEven = pricing.getBreakEvenSustainedMbps();
            BigDecimal eachWay = breakEven.divide(TWO, 1, RoundingMode.HALF_UP);
            BigDecimal capacity = BigDecimal.valueOf(2L * link.getCoveringTierMbps());
            boolean nativeAbove = pricing.isNativeCheaperAboveBreakEven();
            String below = nativeAbove ? "the Equinix path" : "the native link";
            String above = nativeAbove ? "the native link" : "the Equinix path";
            sb.append("Fixed monthly cost: native ").append(nativeFee).append(" plus ")
                    .append(pricing.getNativePerGb().toPlainString()).append(" ").append(pricing.getPerGbCurrency())
                    .append(" per GB, Equinix path ").append(equinixFee)
                    .append(" plus ").append(pricing.getEquinixPerGb().toPlainString()).append(" ")
                    .append(pricing.getPerGbCurrency()).append(" per GB (both clouds' rates summed). "
                            + "Break-even at ").append(breakEven.toPlainString())
                    .append(" Mbps sustained, both directions summed (").append(eachWay.toPlainString())
                    .append(" Mbps each way, symmetric traffic assumed). Below that rate ").append(below)
                    .append(" costs less per month; above it ").append(above).append(" does. ");
            if (breakEven.compareTo(capacity) > 0) {
                sb.append("The break-even exceeds the link's capacity (").append(capacity.toPlainString())
                        .append(" Mbps summed at ").append(link.getCoveringTierMbps())
                        .append(" Mbps), so at this size ").append(below)
                        .append(" costs less at every achievable rate.");
            } else {
                BigDecimal percent = breakEven.multiply(BigDecimal.valueOf(100)).divide(capacity, 0, RoundingMode.HALF_UP);
                sb.append("That is ").append(percent.toPlainString()).append("% sustained utilization of a ")
                        .append(link.getCoveringTierMbps()).append(" Mbps link in both directions.");
            }
            return sb.toString();
        }
        if (pricing.getEquinixPerGb() == null || pricing.getNativePerGb() == null) {
            return sb.append("Fixed monthly cost: native ").append(nativeFee).append(", Equinix path ").append(equinixFee)
                    .append(". A per-GB rate is missing (see the notes), so no break-even is stated.").toString();
        }
        if (!CurrencyReconciler.sameKnownCurrency(pricing.getNativeCurrency(), pricing.getEquinixCurrency())
                || !CurrencyReconciler.sameKnownCurrency(pricing.getNativeCurrency(), pricing.getPerGbCurrency())) {
            return sb.append("Fixed monthly cost: native ").append(nativeFee).append(", Equinix path ").append(equinixFee)
                    .append(". The fixed costs (").append(pricing.getNativeCurrency()).append(", ")
                    .append(pricing.getEquinixCurrency()).append(") and the per-GB rates (")
                    .append(pricing.getPerGbCurrency())
                    .append(") are not in one currency; no comparison is made and no FX rate is applied.")
                    .toString();
        }
        // No crossing: one path costs the same or less at every volume. Both differences are
        // stated, because a fixed-fee advantage alone does not decide it.
        EgressPath dominant = MulticloudPathComparison.dominatesAtEveryVolume(pricing.getNativeMonthly(),
                pricing.getEquinixFixedMonthly(), pricing.getEquinixPerGb(), pricing.getNativePerGb()).orElseThrow();
        String perGb = pricing.getNativePerGb().toPlainString() + " vs " + pricing.getEquinixPerGb().toPlainString()
                + " " + pricing.getPerGbCurrency() + " per GB";
        if (dominant == EgressPath.MULTICLOUD_INTERCONNECT) {
            return sb.append("The native link's flat fee (").append(nativeFee).append(") does not exceed the Equinix "
                    + "path's fixed cost (").append(equinixFee).append(") and its per-GB rate does not exceed the "
                    + "Equinix path's (").append(perGb).append("), so the native link costs the same or less at "
                    + "every volume.").toString();
        }
        return sb.append("The Equinix path's fixed cost (").append(equinixFee).append(") does not exceed the native "
                + "fee (").append(nativeFee).append(") and its per-GB rate does not exceed the native link's (")
                .append(perGb).append("), so the Equinix path costs the same or less at every volume.").toString();
    }

    /** Two decimal places, half-up: the scale every monthly amount on a link is reported at. */
    private static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String unpricedSides(MulticloudLinkQuote quote) {
        List<String> sides = new ArrayList<>(2);
        if (quote.getSideA().isEmpty()) {
            sides.add(quote.getProviderA() + " side");
        }
        if (quote.getSideZ().isEmpty()) {
            sides.add(quote.getProviderZ() + " side");
        }
        return String.join(" and ", sides);
    }

    private static String amount(BigDecimal value, Object currency) {
        String code = currency instanceof java.util.Currency
                ? ((java.util.Currency) currency).getCurrencyCode()
                : (currency == null ? null : currency.toString());
        String plain = value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return code == null ? plain : code + " " + plain;
    }

    private static <T> List<T> nz(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
