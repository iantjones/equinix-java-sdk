package api.equinix.javasdk.design.optimizer.wizard;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.design.optimizer.model.*;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import api.equinix.javasdk.design.optimizer.wizard.model.*;
import api.equinix.javasdk.design.value.CurrencyReconciler;
import api.equinix.javasdk.design.value.ratecard.EquinixRateCard;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Internal engine that transforms an {@link OptimizationResult} into a {@link DeploymentPlan}
 * based on the wizard builder configuration. Derives Cloud Routers, provider connections,
 * backbone links, routing protocols, and pricing from the optimization recommendations.
 */
final class DeploymentWizardEngine {

    private DeploymentWizardEngine() {}

    /**
     * Generates a complete deployment plan from the wizard builder configuration.
     */
    static DeploymentPlan generatePlan(DeploymentWizard.Builder config) {
        OptimizationResult result = config.getOptimizationResult();
        List<MetroRecommendation> metros = result.getRecommendations();
        List<String> validationErrors = new ArrayList<>();

        if (metros == null || metros.isEmpty()) {
            validationErrors.add("No metro recommendations available from optimization result");
            return DeploymentPlan.builder()
                    .sourceOptimization(result)
                    .cloudRouters(Collections.emptyList())
                    .providerConnections(Collections.emptyList())
                    .backboneLinks(Collections.emptyList())
                    .routingProtocols(Collections.emptyList())
                    .valid(false)
                    .validationErrors(validationErrors)
                    .fabric(config.getFabric())
                    .build();
        }

        // The /30 peering-subnet allocator is PLAN-SCOPED (never static): concurrent generatePlan
        // calls — the embedded MCP server plans on server threads — each get their own sequence, so
        // one plan's allocation can never corrupt another's. Note the documented cross-plan caveat on
        // DeploymentWizard.Builder#subnetBase: sequential plans deliberately restart from the same
        // base, so plans executed into the same project should be given distinct bases.
        SubnetAllocator subnets = new SubnetAllocator(config.getSubnetBase());

        // Every generated name flows through one PlanNames instance so the whole plan shares a single
        // uniqueness namespace and a single < 24-character cap (Fabric error EQ-3142539). The prefix is
        // validated and length-bounded up front so the composed names still fit.
        PlanNames names = new PlanNames();
        String prefix = PlanNames.validatePrefix(config.getRouterNamePrefix());

        // Allocate the canonical Cloud Router name per metro first, so every reference to it — the
        // router itself, each provider connection's A-side, and each backbone link's A/Z-side — resolves
        // to the exact same unique, capped name.
        Map<MetroId, String> routerNames = new LinkedHashMap<>();
        for (MetroRecommendation metro : metros) {
            routerNames.computeIfAbsent(metro.getMetroId(),
                    metroId -> names.unique(prefix + "-" + metroId));
        }

        // Phase 1: Plan Cloud Routers
        List<PlannedCloudRouter> cloudRouters = planCloudRouters(config, metros, routerNames);

        // Phase 2: Plan Provider Connections. Bandwidth-aware profile selection may find a metro's
        // provider offers no service profile that covers the computed connection bandwidth; that is a
        // real, plan-invalidating error (an unbuildable connection), so it is recorded here rather than
        // emitted silently and left for the Layer-1 tier check to catch downstream.
        List<PlannedConnection> providerConnections =
                planProviderConnections(config, metros, result, names, routerNames, validationErrors);

        // Phase 3: Plan Backbone Links
        List<PlannedBackboneLink> backboneLinks = planBackboneLinks(config, metros, names, routerNames);

        // Phase 4: Plan Routing Protocols
        List<PlannedRoutingProtocol> routingProtocols =
                planRoutingProtocols(config, providerConnections, backboneLinks, names, subnets);

        // Phase 5: Estimate Pricing
        PlanPricing pricing = estimatePricing(config, cloudRouters, providerConnections, backboneLinks);

        // Phase 6: Layered plan-time validation.
        //   Layer 1 — structural + catalog checks (no provisioning, no live connection dry-run).
        //   Layer 2 — live router dry-run (self-contained FCRs, POST /routers?dryRun=true).
        //   Layer 3 — connection endpoint dry-run: DEFERRED for a to-be-created FCR (recorded, not sent
        //             as a doomed endpoint-less dry-run), or a REAL dry-run for a pre-existing endpoint.
        // The connection-authorization each connection will need before provisioning is enumerated
        // separately so a structurally-fine plan is never reported as a validation error.
        PlanValidator.Result validation = PlanValidator.validate(
                metros,
                result.getRequest(),
                result.getTopology(),
                cloudRouters,
                providerConnections,
                backboneLinks,
                routingProtocols,
                config.getCustomerAsn(),
                config.getFabric());
        validationErrors.addAll(validation.errors);

        return DeploymentPlan.builder()
                .sourceOptimization(result)
                .cloudRouters(cloudRouters)
                .providerConnections(providerConnections)
                .backboneLinks(backboneLinks)
                .routingProtocols(routingProtocols)
                .pricing(pricing)
                .valid(validationErrors.isEmpty())
                .validationErrors(validationErrors)
                .deferredValidations(validation.deferred)
                .skippedValidations(validation.skipped)
                .requiredInputs(validation.requiredInputs)
                .fabric(config.getFabric())
                .build();
    }

    // ══════════════════════════════════════════════
    //  Phase 1: Cloud Routers
    // ══════════════════════════════════════════════

    private static List<PlannedCloudRouter> planCloudRouters(
            DeploymentWizard.Builder config, List<MetroRecommendation> metros,
            Map<MetroId, String> routerNames) {

        List<String> notificationEmails = notificationEmails(config);

        return metros.stream()
                .map(metro -> PlannedCloudRouter.builder()
                        .metroId(metro.getMetroId())
                        .name(routerNames.get(metro.getMetroId()))
                        .packageCode(config.getRouterPackage())
                        .accountNumber(config.getAccountNumber())
                        .projectId(config.getProjectId())
                        .notificationEmails(notificationEmails)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * The FULL configured notification list, stamped verbatim onto every planned resource — the old
     * code took only {@code get(0)}, silently dropping every additional recipient the caller
     * supplied. {@code null} when none was configured (Layer-1 validation flags that).
     */
    private static List<String> notificationEmails(DeploymentWizard.Builder config) {
        List<String> emails = config.getNotificationEmails();
        return emails == null || emails.isEmpty() ? null : List.copyOf(emails);
    }

    // ══════════════════════════════════════════════
    //  Phase 2: Provider Connections
    // ══════════════════════════════════════════════

    private static List<PlannedConnection> planProviderConnections(
            DeploymentWizard.Builder config, List<MetroRecommendation> metros, OptimizationResult result,
            PlanNames names, Map<MetroId, String> routerNames, List<String> validationErrors) {

        List<PlannedConnection> connections = new ArrayList<>();
        List<String> notificationEmails = notificationEmails(config);

        for (MetroRecommendation metro : metros) {
            if (metro.getAvailableProviders() == null) continue;

            String routerName = routerNames.get(metro.getMetroId());

            List<ProviderAvailability> availableProviders = metro.getAvailableProviders().stream()
                    .filter(ProviderAvailability::isAvailable)
                    .collect(Collectors.toList());

            for (ProviderAvailability provider : availableProviders) {
                BandwidthAllocation bandwidth = computeBandwidth(
                        config, metro, provider, result);

                if (bandwidth.getTotalMbps() <= 0) continue;

                // Compose the connection name from the Cloud Router's own (already unique, capped) name
                // plus a COMPACT provider token — "aws", not "amazon-web-services" — then cap and dedupe
                // it so it clears Fabric's < 24-character limit (EQ-3142539) even for long prefixes.
                String connName = names.unique(routerName + "-to-" + PlanNames.providerToken(provider.getProviderLabel()));

                // Bandwidth-aware profile selection. The optimizer's single winner was chosen with no
                // knowledge of this bandwidth, so a hosted profile (capped at, say, 500 Mbps) can be the
                // default for a metro sized at 3000 Mbps. When the provider carries candidate profiles,
                // pick one whose tiers cover the bandwidth — ROUNDING UP to the smallest satisfying tier
                // (3000 -> 5000) rather than erroring on a non-exact requirement. The connection is
                // stamped at that covering tier (so the Layer-1 tier check and pricing agree), the choice
                // and any round-up are recorded on the connection's ProfileSelection (never a silent
                // upsell), and every covering candidate is exposed for an interactive layer to resolve.
                // Only when NOTHING can carry the bandwidth even rounded up is a precise error recorded.
                int requestedMbps = bandwidth.getTotalMbps();
                String serviceProfileUuid;
                String sellerRegion;
                int stampedBandwidth;
                ProfileSelection profileSelection = null;
                BandwidthAllocation stampedAllocation = bandwidth;
                List<ServiceProfileOption> options = provider.getProfileOptions();
                if (options != null && !options.isEmpty()) {
                    ProfileSelection selection = selectProfile(provider, requestedMbps);
                    if (selection == null) {
                        validationErrors.add(noCoveringProfileError(
                                connName, provider, metro.getMetroId(), requestedMbps, options));
                        continue;
                    }
                    profileSelection = selection;
                    serviceProfileUuid = selection.getSelectedProfileUuid();
                    sellerRegion = selection.getSelectedSellerRegion();
                    stampedBandwidth = selection.getSelectedTierMbps();
                    if (selection.isRoundedUp()) {
                        // Surface the round-up on the sizing rationale too, so a reader of the bandwidth
                        // breakdown sees the requirement AND the billed tier, not just the billed tier.
                        stampedAllocation = bandwidth.toBuilder()
                                .reasoning(bandwidth.getReasoning() + " | Rounded up " + requestedMbps + "→"
                                        + stampedBandwidth + " Mbps to the nearest service-profile tier (+"
                                        + selection.roundedUpByMbps() + " Mbps billable)")
                                .build();
                    }
                } else {
                    // Legacy / hand-built entry with no capability data: keep the pre-selected default and
                    // the raw requirement — there is no tier list to round against.
                    serviceProfileUuid = provider.getServiceProfileUuid();
                    sellerRegion = firstOrNull(provider.getSellerRegions());
                    stampedBandwidth = requestedMbps;
                }

                connections.add(PlannedConnection.builder()
                        .name(connName)
                        .connectionType(config.getProviderConnectionType())
                        .purpose(ConnectionPurpose.PROVIDER)
                        .bandwidthMbps(stampedBandwidth)
                        .bandwidthAllocation(stampedAllocation)
                        .profileSelection(profileSelection)
                        .aSideMetro(metro.getMetroId())
                        .aSideRouterName(routerName)
                        .zSideServiceProfileUuid(serviceProfileUuid)
                        .zSideProviderLabel(provider.getProviderLabel())
                        .zSideSellerRegion(sellerRegion)
                        // Resolve the provider label to a typed cloud provider so the plan can
                        // enumerate the exact authorization key the connection will need at
                        // provisioning (AWS Account ID, Azure service key, GCP pairing key, ...).
                        .zSideCloudType(ConnectionBodies.resolveCloudType(provider.getProviderLabel()))
                        .notificationEmails(notificationEmails)
                        .build());
            }
        }

        return connections;
    }

    /**
     * Selects, among a provider's candidate service profiles for a metro, the one to build a connection
     * of the requested speed on — <em>rounding up</em> to the smallest tier that satisfies the
     * requirement when no exact tier exists (3000&nbsp;Mbps against {@code [1000, 5000, 10000]} selects
     * 5000, never an error), and stamping the connection at that covering tier.
     *
     * <p>Policy: prefer the profile whose smallest satisfying tier is smallest (least over-provisioning,
     * so a 3000&nbsp;Mbps request prefers a profile rounding to 4000 over one rounding to 5000, and a
     * profile carrying it exactly over either); then the tighter overall ceiling (hosted before
     * dedicated when both would round to the same tier); then fewest wasted tiers; then the optimizer's
     * default winner (preserving its seller-region preference); then the lowest uuid, so the choice is
     * deterministic regardless of catalog order. Every covering candidate is returned on the selection
     * so an interactive layer can present the alternatives.</p>
     *
     * @param provider the available provider entry, carrying its candidate {@code profileOptions}
     * @param mbps     the computed connection bandwidth requirement in Mbps
     * @return the selection (default profile, stamped tier, and covering alternatives), or {@code null}
     *         when no candidate can carry {@code mbps} even after rounding up
     */
    private static ProfileSelection selectProfile(ProviderAvailability provider, int mbps) {
        List<ServiceProfileOption> options = provider.getProfileOptions();
        if (options == null || options.isEmpty()) {
            return null;
        }
        String defaultUuid = provider.getServiceProfileUuid();
        Comparator<ServiceProfileOption> byFit = Comparator
                .comparingInt((ServiceProfileOption o) -> o.coveringTier(mbps))
                .thenComparingInt(ServiceProfileOption::capacityCeiling)
                .thenComparingInt(o -> o.excessTiersAbove(mbps))
                .thenComparingInt(o -> Objects.equals(o.getServiceProfileUuid(), defaultUuid) ? 0 : 1)
                .thenComparing(ServiceProfileOption::getServiceProfileUuid,
                        Comparator.nullsLast(Comparator.naturalOrder()));

        List<ServiceProfileOption> covering = options.stream()
                .filter(option -> option.canCover(mbps))
                .sorted(byFit)
                .collect(Collectors.toList());
        if (covering.isEmpty()) {
            return null;
        }
        ServiceProfileOption chosen = covering.get(0);
        int chosenTier = chosen.coveringTier(mbps);
        boolean roundedUp = chosenTier > mbps;
        List<ProfileCandidate> alternatives = covering.stream()
                .map(option -> toCandidate(option, mbps))
                .collect(Collectors.toList());

        return ProfileSelection.builder()
                .requestedMbps(mbps)
                .selectedProfileUuid(chosen.getServiceProfileUuid())
                .selectedSellerRegion(firstOrNull(chosen.getSellerRegions()))
                .selectedTierMbps(chosenTier)
                .roundedUp(roundedUp)
                .alternatives(alternatives)
                .reasoning(selectionReasoning(chosen, mbps, chosenTier, covering.size(), roundedUp))
                .build();
    }

    /** Projects a chosen/candidate service-profile option into the exposed {@link ProfileCandidate}. */
    private static ProfileCandidate toCandidate(ServiceProfileOption option, int mbps) {
        return ProfileCandidate.builder()
                .serviceProfileUuid(option.getServiceProfileUuid())
                .sellerRegions(option.getSellerRegions())
                .coveringTierMbps(option.coveringTier(mbps))
                .supportedBandwidths(option.getSupportedBandwidths())
                .allowCustomBandwidth(option.isAllowCustomBandwidth())
                .vcBandwidthMax(option.getVcBandwidthMax())
                .build();
    }

    /** A one-line, honest explanation of the profile selection, including any round-up and its cost. */
    private static String selectionReasoning(ServiceProfileOption chosen, int requested, int chosenTier,
                                             int candidateCount, boolean roundedUp) {
        String profile = chosen.getServiceProfileUuid() != null ? chosen.getServiceProfileUuid() : "(unknown)";
        String choice = candidateCount > 1
                ? " " + candidateCount + " profiles can carry this bandwidth; the tightest-fitting was chosen "
                    + "(the alternatives are exposed for review)."
                : "";
        if (roundedUp) {
            return "Requested " + requested + " Mbps has no exact tier on any candidate profile; rounded up to "
                    + "the smallest satisfying tier " + chosenTier + " Mbps on profile " + profile
                    + " (+" + (chosenTier - requested) + " Mbps billable)." + choice;
        }
        return "Requested " + requested + " Mbps is carried exactly at " + chosenTier + " Mbps by profile "
                + profile + "." + choice;
    }

    /**
     * A precise, actionable error for a connection whose bandwidth exceeds every available profile even
     * after rounding up — the genuine over-capacity case (never the merely-non-exact case, which now
     * rounds up). It names the provider, metro and requested bandwidth, states the largest bandwidth any
     * candidate can actually carry, and suggests reducing the speed or splitting across connections.
     */
    private static String noCoveringProfileError(String connName, ProviderAvailability provider,
                                                 MetroId metro, int mbps, List<ServiceProfileOption> options) {
        int maxCoverable = options.stream()
                .mapToInt(ServiceProfileOption::maxCoverableMbps)
                .filter(m -> m > 0 && m != Integer.MAX_VALUE)
                .max()
                .orElse(0);
        String ceilingClause;
        String suggestion;
        if (maxCoverable > 0) {
            int splits = (int) Math.ceil((double) mbps / maxCoverable);
            ceilingClause = "the largest bandwidth any available service profile can carry is " + maxCoverable
                    + " Mbps";
            suggestion = "reduce the bandwidth to " + maxCoverable + " Mbps or below, or split the workload "
                    + "across multiple connections (for example " + splits + " connections of " + maxCoverable
                    + " Mbps)";
        } else {
            ceilingClause = "no available service profile publishes a usable bandwidth";
            suggestion = "choose a bandwidth within a candidate profile's ceiling, or split the workload "
                    + "across multiple connections";
        }
        return "Connection '" + connName + "' to provider '" + provider.getProviderLabel() + "' at metro "
                + metro + ": bandwidth " + mbps + " Mbps exceeds every available service profile even after "
                + "rounding up to the nearest tier — " + ceilingClause + ". " + suggestion + ". Candidate "
                + "profiles: " + describeProfileOptions(options) + ".";
    }

    /** Renders each candidate profile's bandwidth capability for the no-covering-profile error. */
    private static String describeProfileOptions(List<ServiceProfileOption> options) {
        return options.stream().map(option -> {
            String uuid = option.getServiceProfileUuid() != null ? option.getServiceProfileUuid() : "(unknown)";
            if (option.getSupportedBandwidths() != null && !option.getSupportedBandwidths().isEmpty()) {
                String custom = option.isAllowCustomBandwidth() ? " or custom" : "";
                String max = option.getVcBandwidthMax() != null ? " (max " + option.getVcBandwidthMax() + " Mbps)" : "";
                return "profile " + uuid + " offers " + option.getSupportedBandwidths() + custom + max;
            }
            if (option.isAllowCustomBandwidth()) {
                String max = option.getVcBandwidthMax() != null ? " up to " + option.getVcBandwidthMax() + " Mbps" : "";
                return "profile " + uuid + " allows custom bandwidth" + max;
            }
            String max = option.getVcBandwidthMax() != null ? " (ceiling " + option.getVcBandwidthMax() + " Mbps)" : "";
            return "profile " + uuid + " publishes no discrete tiers" + max;
        }).collect(Collectors.joining("; "));
    }

    private static String firstOrNull(List<String> values) {
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    private static BandwidthAllocation computeBandwidth(
            DeploymentWizard.Builder config,
            MetroRecommendation metro,
            ProviderAvailability provider,
            OptimizationResult result) {

        BandwidthStrategy strategy = config.getBandwidthStrategy();
        Map<String, Integer> perWorkload = new LinkedHashMap<>();

        String unmatchedCustomKeys = null;
        if (strategy == BandwidthStrategy.CUSTOM && config.getCustomBandwidthMap() != null) {
            // Match the documented key forms, most specific first: "<metroId>-<providerLabel>" (a
            // per-metro-per-provider override) then "<providerLabel>" (that provider in every metro).
            // The old code looked up ONLY the compound key with the full provider label, so a caller
            // whose real provider label differs from the key they wrote silently matched nothing. The
            // provider-label form is now honoured too.
            Map<String, Integer> customMap = config.getCustomBandwidthMap();
            String perMetroKey = metro.getMetroId() + "-" + provider.getProviderLabel();
            String providerKey = provider.getProviderLabel();
            String matchedKey = customMap.containsKey(perMetroKey) ? perMetroKey
                    : (customMap.containsKey(providerKey) ? providerKey : null);
            if (matchedKey != null) {
                int customBw = customMap.get(matchedKey);
                return BandwidthAllocation.builder()
                        .totalMbps(customBw)
                        .perWorkload(Collections.singletonMap("custom", customBw))
                        .reasoning("Custom bandwidth map (key '" + matchedKey + "'): " + customBw + " Mbps")
                        .build();
            }
            // No key matched. The Builder#customBandwidthMap contract promises fallback to the
            // normal per-workload aggregation for an unmatched (metro, provider) — NOT a fabricated
            // flat default (the old 1000 Mbps invention, which mis-sized and mis-priced every
            // unmapped connection). Fall through to the standard sizing below, recording the keys
            // that were tried so the reasoning states plainly why the map did not supply the value.
            unmatchedCustomKeys = "'" + perMetroKey + "' or '" + providerKey + "'";
        }

        // Determine workloads at this metro
        List<WorkloadPlacement> placements = result.getTopology() != null
                ? result.getTopology().forMetro(metro.getMetroId())
                : Collections.emptyList();

        List<WorkloadSpec> workloadSpecs = result.getRequest() != null
                ? result.getRequest().getWorkloads() : Collections.emptyList();

        int totalMbps = 0;

        for (WorkloadPlacement placement : placements) {
            WorkloadSpec spec = workloadSpecs.stream()
                    .filter(w -> w.getLabel().equals(placement.getWorkloadLabel()))
                    .findFirst().orElse(null);

            if (spec == null) continue;

            // Check if this workload depends on this provider
            boolean dependsOnProvider = false;
            if (spec.getDependsOnProviders() != null) {
                for (ProviderRequirement dep : spec.getDependsOnProviders()) {
                    String depLabel = dep.displayLabel();
                    if (depLabel.equalsIgnoreCase(provider.getProviderLabel())) {
                        dependsOnProvider = true;
                        break;
                    }
                }
            }

            // PER_WORKLOAD sizes from the workloads that depend on this provider; the CUSTOM
            // strategy's documented unmatched-key fallback is that same per-workload aggregation.
            if ((strategy == BandwidthStrategy.PER_WORKLOAD || strategy == BandwidthStrategy.CUSTOM)
                    && !dependsOnProvider) {
                continue;
            }

            // Honour the workload profile's minimum bandwidth: a workload declaring 10 Mbps against a
            // profile whose floor is 1000 must be sized (and priced) at 1000, not silently under-sized.
            // Mirrors MetroOptimizerEngine.effectiveBandwidthMbps so the wizard and the optimizer agree.
            int effective = effectiveWorkloadBandwidth(spec);
            int bw = effective > 0 ? effective : 1000;
            perWorkload.put(spec.getLabel(), bw);
            totalMbps += bw;
        }

        // Ensure minimum bandwidth
        if (totalMbps <= 0) {
            totalMbps = 1000;
            perWorkload.put("default", 1000);
        }

        String reasoning;
        if (unmatchedCustomKeys != null) {
            reasoning = "Custom strategy: no bandwidth-map key matched " + unmatchedCustomKeys
                    + ", so the documented fallback applied — sized by the normal per-workload "
                    + "aggregation at " + metro.getMetroId();
        }
        else if (strategy == BandwidthStrategy.AGGREGATED) {
            reasoning = "Aggregated bandwidth for all workloads at " + metro.getMetroId();
        }
        else {
            // PER_WORKLOAD, and CUSTOM configured with no bandwidth map at all (which sizes the
            // same way: by the workloads that depend on this provider).
            reasoning = "Sum of dependent workload bandwidths at " + metro.getMetroId();
        }

        return BandwidthAllocation.builder()
                .totalMbps(totalMbps)
                .perWorkload(perWorkload)
                .reasoning(reasoning)
                .build();
    }

    /**
     * The bandwidth a workload is actually sized at: its declared {@code bandwidthMbps}, raised to the
     * resolved profile's {@code minBandwidthMbps} when the declaration falls below that floor. A
     * minimum that is enforced nowhere is not a minimum, so the wizard applies it just as the
     * optimizer does (see {@code MetroOptimizerEngine.effectiveBandwidthMbps}).
     */
    private static int effectiveWorkloadBandwidth(WorkloadSpec spec) {
        int declared = spec.getBandwidthMbps();
        Double floor = spec.resolvedProfile() != null ? spec.resolvedProfile().getMinBandwidthMbps() : null;
        if (floor == null || !Double.isFinite(floor) || floor <= 0) {
            return declared;
        }
        return (int) Math.max(declared, Math.ceil(floor));
    }

    // ══════════════════════════════════════════════
    //  Phase 3: Backbone Links
    // ══════════════════════════════════════════════

    private static List<PlannedBackboneLink> planBackboneLinks(
            DeploymentWizard.Builder config, List<MetroRecommendation> metros,
            PlanNames names, Map<MetroId, String> routerNames) {

        if (metros.size() < 2) return Collections.emptyList();

        List<PlannedBackboneLink> links = new ArrayList<>();
        BackboneTopology topology = config.getBackboneTopology();
        List<String> notificationEmails = notificationEmails(config);

        List<MetroId> metroCodes = metros.stream()
                .map(MetroRecommendation::getMetroId)
                .collect(Collectors.toList());

        List<int[]> pairs = computeTopologyPairs(topology, metroCodes.size());

        for (int[] pair : pairs) {
            MetroId metroA = metroCodes.get(pair[0]);
            MetroId metroZ = metroCodes.get(pair[1]);
            String aSideRouterName = routerNames.get(metroA);
            String zSideRouterName = routerNames.get(metroZ);
            // Backbone link name extends the A-side router's (unique, capped) name with the Z-side
            // metro, then runs through the same cap-and-dedupe so it, too, stays < 24 characters.
            String linkName = names.unique(aSideRouterName + "-to-" + metroZ);

            PlannedConnection connection = PlannedConnection.builder()
                    .name(linkName)
                    .connectionType(config.getBackboneConnectionType())
                    .purpose(ConnectionPurpose.BACKBONE)
                    .bandwidthMbps(config.getBackboneBandwidthMbps())
                    .aSideMetro(metroA)
                    .aSideRouterName(aSideRouterName)
                    .zSideMetro(metroZ)
                    .zSideRouterName(zSideRouterName)
                    .notificationEmails(notificationEmails)
                    .build();

            links.add(PlannedBackboneLink.builder()
                    .metroA(metroA)
                    .metroZ(metroZ)
                    .name(linkName)
                    .bandwidthMbps(config.getBackboneBandwidthMbps())
                    .topology(topology)
                    .connection(connection)
                    .build());
        }

        return links;
    }

    private static List<int[]> computeTopologyPairs(BackboneTopology topology, int n) {
        List<int[]> pairs = new ArrayList<>();

        switch (topology) {
            case FULL_MESH:
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        pairs.add(new int[]{i, j});
                    }
                }
                break;

            case HUB_SPOKE:
                for (int i = 1; i < n; i++) {
                    pairs.add(new int[]{0, i});
                }
                break;

            case RING:
                if (n == 2) {
                    // A 2-metro "ring" degenerates to the single A-B link (as FULL_MESH yields).
                    // The naive modulo walk would emit BOTH (0,1) and (1,0) — two distinctly-named,
                    // doubly-billed connections between the same pair of routers.
                    pairs.add(new int[]{0, 1});
                    break;
                }
                for (int i = 0; i < n; i++) {
                    pairs.add(new int[]{i, (i + 1) % n});
                }
                break;
        }

        return pairs;
    }

    // ══════════════════════════════════════════════
    //  Phase 4: Routing Protocols
    // ══════════════════════════════════════════════

    private static List<PlannedRoutingProtocol> planRoutingProtocols(
            DeploymentWizard.Builder config,
            List<PlannedConnection> providerConnections,
            List<PlannedBackboneLink> backboneLinks,
            PlanNames names, SubnetAllocator subnets) {

        List<PlannedRoutingProtocol> protocols = new ArrayList<>();

        // Protocols for provider connections
        for (PlannedConnection conn : providerConnections) {
            protocols.addAll(createProtocolPair(config, conn.getName(), names, subnets));
        }

        // Protocols for backbone links
        for (PlannedBackboneLink link : backboneLinks) {
            protocols.addAll(createProtocolPair(config, link.getConnection().getName(), names, subnets));
        }

        return protocols;
    }

    private static List<PlannedRoutingProtocol> createProtocolPair(
            DeploymentWizard.Builder config, String connectionName, PlanNames names,
            SubnetAllocator subnets) {

        List<PlannedRoutingProtocol> pair = new ArrayList<>(2);
        String subnet = subnets.next();

        // The protocol's own name must also clear Fabric's < 24-character limit, so it is composed
        // through the same generator (which truncates the connection stem and, if that collides,
        // appends a hash) rather than by raw string concatenation. connectionName stays the exact
        // parent-connection name — it is the foreign key execute() resolves the parent by.

        // DIRECT protocol (IP assignment)
        pair.add(PlannedRoutingProtocol.builder()
                .type(RoutingProtocolType.DIRECT)
                .name(names.uniqueWithSuffix(connectionName, "DIRECT"))
                .connectionName(connectionName)
                .equinixIfaceIpv4(subnet + ".1/30")
                .bfdEnabled(false)
                .bfdInterval(0)
                .build());

        // BGP protocol (dynamic routing)
        pair.add(PlannedRoutingProtocol.builder()
                .type(RoutingProtocolType.BGP)
                .name(names.uniqueWithSuffix(connectionName, "BGP"))
                .connectionName(connectionName)
                .customerPeerIpv4(subnet + ".2/30")
                .equinixPeerIpv4(subnet + ".1/30")
                .customerAsn(config.getCustomerAsn())
                .bfdEnabled(config.isBfdEnabled())
                .bfdInterval(config.getBfdInterval())
                .build());

        return pair;
    }

    /**
     * Plan-scoped allocator for the /30 peering subnets stamped onto each connection's DIRECT+BGP
     * pair. One instance is built per {@code generatePlan} call (like {@link PlanNames}), so
     * concurrent plans — the embedded MCP server plans on server threads — never interleave into
     * each other's sequence, and a plan's addressing is deterministic for its configuration.
     *
     * <p>Each connection consumes one third-octet step of the configured base (default
     * {@code 10.100.0.0}): the first pair peers on {@code 10.100.0.1/30}÷{@code .2/30}, the next on
     * {@code 10.100.1.x}, carrying into the second octet past 256 connections — the exact sequence
     * the old static counter produced. Sequential plans deliberately restart from the same base;
     * see the cross-plan caveat on {@code DeploymentWizard.Builder#subnetBase}.</p>
     */
    static final class SubnetAllocator {

        /** The /24-aligned starting network, packed as an int (the base's fourth octet is ignored). */
        private final int base;
        private int counter;

        SubnetAllocator(String subnetBase) {
            String[] octets = subnetBase.split("\\.");
            this.base = (Integer.parseInt(octets[0]) << 24)
                    | (Integer.parseInt(octets[1]) << 16)
                    | (Integer.parseInt(octets[2]) << 8);
        }

        /** The next three-octet prefix (e.g. {@code "10.100.3"}); the caller appends the host part. */
        String next() {
            int network = base + (counter++ << 8);
            return ((network >>> 24) & 0xFF) + "." + ((network >>> 16) & 0xFF) + "." + ((network >>> 8) & 0xFF);
        }
    }

    // ══════════════════════════════════════════════
    //  Phase 5: Pricing
    // ══════════════════════════════════════════════

    // Package-private (not private) so DeploymentWizard — in this package — can reprice an existing
    // plan after its connections change (e.g. an MCP profile choice altered a billable tier). The
    // logic is unchanged; only the visibility is widened for that one same-package caller.
    static PlanPricing estimatePricing(
            DeploymentWizard.Builder config,
            List<PlannedCloudRouter> routers,
            List<PlannedConnection> providerConnections,
            List<PlannedBackboneLink> backboneLinks) {

        RateCard rateCard = resolveRateCard(config);
        Term term = config.getTerm();

        Map<String, BigDecimal> perConnectionCost = new LinkedHashMap<>();
        Set<PriceSource> sources = new HashSet<>();
        // Every priced line flows through one plan-wide reconciler PLUS its category's reconciler. A
        // plan can legitimately span metros in different currencies (live Fabric pricing quotes EUR in
        // Frankfurt, USD in Ashburn, ...), and summing those into one monthly total — at the plan
        // level OR within a category — would be a fabricated cross-currency figure.
        CurrencyReconciler recon = CurrencyReconciler.create();
        CurrencyReconciler routerRecon = CurrencyReconciler.create();
        CurrencyReconciler providerRecon = CurrencyReconciler.create();
        CurrencyReconciler backboneRecon = CurrencyReconciler.create();

        // Cloud Routers
        for (PlannedCloudRouter router : routers) {
            PriceQuote quote = priceRouter(rateCard, router, term);
            recon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            routerRecon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            sources.add(quote.getSource());
        }

        // Provider connections
        for (PlannedConnection conn : providerConnections) {
            PriceQuote quote = priceConnection(rateCard, conn.getConnectionType(),
                    conn.getBandwidthMbps(), conn.getASideMetro(), term);
            perConnectionCost.put(conn.getName(), quote.getMonthlyRecurring());
            recon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            providerRecon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            sources.add(quote.getSource());
        }

        // Backbone links
        for (PlannedBackboneLink link : backboneLinks) {
            ConnectionType type = link.getConnection() != null ? link.getConnection().getConnectionType() : null;
            PriceQuote quote = priceConnection(rateCard, type, link.getBandwidthMbps(), link.getMetroA(), term);
            perConnectionCost.put(link.getName(), quote.getMonthlyRecurring());
            recon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            backboneRecon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            sources.add(quote.getSource());
        }

        PriceSource source = dominantSource(sources);
        boolean authoritative = source == PriceSource.EQUINIX_LIVE || source == PriceSource.CUSTOM;
        boolean mixed = recon.isMixed();

        String currency = mixed ? null : recon.soleCurrencyOr("USD");

        String disclaimer;
        if (mixed) {
            // Mixed currencies: do not fabricate a combined total. Omit the monthly/setup totals and
            // the single currency, and surface the per-currency subtotals. Each CATEGORY figure gets
            // the same treatment: kept (with its own currency) only when that category reconciles to
            // a single currency, otherwise nulled with its per-currency subtotals carrying the truth.
            disclaimer = "This plan's connections are priced in multiple currencies ("
                    + recon.describeCurrencies() + "): " + recon.describeMonthlySubtotals() + " per month. A single "
                    + "plan total cannot be formed without an FX rate, so the monthly and setup totals are omitted "
                    + "rather than reported as a fabricated cross-currency sum. A per-category figure is shown (in "
                    + "its own currency) only when that category reconciles to one currency; a category that itself "
                    + "spans currencies is reported as per-currency subtotals instead of a single figure.";
        }
        else {
            disclaimer = authoritative
                    ? "Based on live Equinix Fabric pricing. Actual costs may vary based on contract terms, volume "
                        + "discounts, and promotional offers."
                    : "Some or all figures are heuristic or reference estimates (live Fabric pricing was unavailable "
                        + "for part of this plan). Actual costs may vary; contact your Equinix account team for precise quotes.";
        }

        return PlanPricing.builder()
                .monthlyTotal(mixed ? null : recon.monthlyTotal().orElse(BigDecimal.ZERO))
                .setupTotal(mixed ? null : recon.setupTotal().orElse(BigDecimal.ZERO))
                .currency(currency)
                .monthlyByCurrency(recon.monthlySubtotals())
                .routerMonthlyCost(categoryMonthly(routerRecon))
                .routerCurrency(routerRecon.soleCurrencyOr(currency))
                .routerMonthlyByCurrency(routerRecon.monthlySubtotals())
                .providerConnectionMonthlyCost(categoryMonthly(providerRecon))
                .providerConnectionCurrency(providerRecon.soleCurrencyOr(currency))
                .providerConnectionMonthlyByCurrency(providerRecon.monthlySubtotals())
                .backboneMonthlyCost(categoryMonthly(backboneRecon))
                .backboneCurrency(backboneRecon.soleCurrencyOr(currency))
                .backboneMonthlyByCurrency(backboneRecon.monthlySubtotals())
                .perConnectionCost(perConnectionCost)
                .source(source)
                .disclaimer(disclaimer)
                .build();
    }

    /**
     * A category's single monthly figure: its reconciled sum when all its line items share a
     * currency (zero for an empty category), or {@code null} when the category itself is mixed —
     * a raw cross-currency category sum (EUR&nbsp;280 + USD&nbsp;300 = "580") is a fabricated
     * number and is never reported.
     */
    private static BigDecimal categoryMonthly(CurrencyReconciler categoryRecon) {
        return categoryRecon.monthlyTotal().orElse(null);
    }

    private static PriceSource dominantSource(Set<PriceSource> sources) {
        if (sources.isEmpty()) {
            return PriceSource.ESTIMATE;
        }
        if (sources.size() == 1) {
            return sources.iterator().next();
        }
        return PriceSource.COMPOSITE;
    }

    /**
     * Resolves the rate card to price the plan with: the explicitly-configured
     * card if set, otherwise live Equinix pricing over the wizard's Fabric
     * gateway. Returns {@code null} only when neither is available (no gateway),
     * in which case pricing falls back entirely to the built-in heuristic.
     */
    private static RateCard resolveRateCard(DeploymentWizard.Builder config) {
        if (config.getRateCard() != null) {
            return config.getRateCard();
        }
        FabricGateway fabric = config.getFabric();
        return fabric != null ? EquinixRateCard.of(fabric) : null;
    }

    /**
     * Resolves a {@link MetroId} to a {@link MetroCode} for the rate-card APIs (which are keyed by
     * the enum). A metro not listed by the enum maps to {@link MetroCode#UNKNOWN}, so it prices via
     * the rate card's fallback rather than a metro-specific rate.
     */
    private static MetroCode toMetroCode(MetroId metroId) {
        return metroId == null ? MetroCode.UNKNOWN : metroId.asMetroCode().orElse(MetroCode.UNKNOWN);
    }

    /**
     * Prices a single connection via the rate card, falling back to the legacy
     * tiered heuristic (tagged {@link PriceSource#ESTIMATE}) when the card
     * cannot resolve a price.
     */
    private static PriceQuote priceConnection(RateCard rateCard, ConnectionType type,
                                              int bandwidthMbps, MetroId metro, Term term) {
        if (rateCard != null) {
            Optional<PriceQuote> quote = rateCard.connection(type, bandwidthMbps, toMetroCode(metro), term);
            if (quote.isPresent()) {
                return quote.get();
            }
        }
        return PriceQuote.of(estimateConnectionCost(bandwidthMbps), BigDecimal.valueOf(500),
                Currency.getInstance("USD"), PriceSource.ESTIMATE);
    }

    /**
     * Prices a single Cloud Router via the rate card, falling back to the legacy
     * ~$300/month heuristic (tagged {@link PriceSource#ESTIMATE}) when the card
     * cannot resolve a price.
     */
    private static PriceQuote priceRouter(RateCard rateCard, PlannedCloudRouter router, Term term) {
        if (rateCard != null) {
            String packageCode = router.getPackageCode() != null ? router.getPackageCode().name() : null;
            Optional<PriceQuote> quote = rateCard.cloudRouter(packageCode, toMetroCode(router.getMetroId()), term);
            if (quote.isPresent()) {
                return quote.get();
            }
        }
        return PriceQuote.of(BigDecimal.valueOf(300), BigDecimal.ZERO,
                Currency.getInstance("USD"), PriceSource.ESTIMATE);
    }

    private static BigDecimal estimateConnectionCost(int bandwidthMbps) {
        // Tiered pricing estimate based on bandwidth
        if (bandwidthMbps <= 50) return BigDecimal.valueOf(150);
        if (bandwidthMbps <= 200) return BigDecimal.valueOf(300);
        if (bandwidthMbps <= 500) return BigDecimal.valueOf(600);
        if (bandwidthMbps <= 1000) return BigDecimal.valueOf(1000);
        if (bandwidthMbps <= 5000) return BigDecimal.valueOf(3000);
        if (bandwidthMbps <= 10000) return BigDecimal.valueOf(5000);
        return BigDecimal.valueOf(8000);
    }

}
