package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import com.eqixiac.equinix.design.optimizer.model.*;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BackboneTopology;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BandwidthStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.ConnectionPurpose;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.*;
import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.design.value.ratecard.EquinixRateCard;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;

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
            return identity(config, DeploymentPlan.builder()
                    .sourceOptimization(result)
                    .cloudRouters(Collections.emptyList())
                    .providerConnections(Collections.emptyList())
                    .backboneLinks(Collections.emptyList())
                    .routingProtocols(Collections.emptyList())
                    .valid(false)
                    .validationErrors(validationErrors)
                    .fabric(config.getFabric())
                    .build());
        }

        // The /30 peering-subnet allocator is PLAN-SCOPED (never static): concurrent generatePlan
        // calls - the embedded MCP server plans on server threads - each get their own sequence, so
        // one plan's allocation can never corrupt another's. Note the documented cross-plan caveat on
        // DeploymentWizard.Builder#subnetBase: sequential plans deliberately restart from the same
        // base, so plans executed into the same project should be given distinct bases.
        SubnetAllocator subnets = new SubnetAllocator(config.getSubnetBase());

        // Every generated name flows through one PlanNames instance so the whole plan shares a single
        // uniqueness namespace and a single < 24-character cap (Fabric error EQ-3142539). The prefix is
        // validated and length-bounded up front so the composed names still fit.
        PlanNames names = new PlanNames();
        String prefix = PlanNames.validatePrefix(config.getRouterNamePrefix());

        // Allocate the canonical Cloud Router name per metro first, so every reference to it - the
        // router itself, each provider connection's A-side, and each backbone link's A/Z-side - resolves
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
        List<PlannedConnection> allProviderConnections =
                planProviderConnections(config, metros, result, names, routerNames, validationErrors);

        // Phase 2b (Beta): cloud-to-cloud flows. Under EQUINIX_ONLY this returns the list above
        // untouched and no links, without reading the multicloud catalog. Under COMPARE it only
        // adds links. Under the NATIVE strategies it can omit whole connections (never resize
        // one) where the replacement rule allows; routing protocols and /30 subnets are then
        // planned for the remaining connections only, and never for a native link.
        MulticloudLinkPlanner.Planned cloudToCloud =
                MulticloudLinkPlanner.plan(config, result, metros, allProviderConnections);
        List<PlannedConnection> providerConnections = cloudToCloud.providerConnections;

        // Phase 3: Plan Backbone Links
        List<PlannedBackboneLink> backboneLinks = planBackboneLinks(config, metros, names, routerNames);

        // Phase 4: Plan Routing Protocols
        List<PlannedRoutingProtocol> routingProtocols =
                planRoutingProtocols(config, providerConnections, backboneLinks, names, subnets);

        // Phase 5: Estimate Pricing. Native links are priced first (each against the Equinix path
        // for the same flow), then reported on PlanPricing separately from the Equinix totals.
        // One rate-card instance serves both steps: a live EquinixRateCard caches its catalogue
        // fetch per instance, so resolving it twice would fetch twice and could price the same
        // connection from two different fetch outcomes.
        RateCard rateCard = PlanPricer.resolveRateCard(config);
        List<PlannedMulticloudInterconnect> multicloudLinks = MulticloudLinkPlanner.price(
                config, rateCard, cloudToCloud.links, cloudRouters, providerConnections, backboneLinks,
                result.getRequest());
        PlanPricing pricing = PlanPricer.estimatePricing(config, rateCard, cloudRouters, providerConnections, backboneLinks,
                multicloudLinks);

        // Phase 6: Layered plan-time validation.
        //   Layer 1 - structural + catalog checks (no provisioning, no live connection dry-run).
        //   Layer 2 - live router dry-run (self-contained FCRs, POST /routers?dryRun=true).
        //   Layer 3 - connection endpoint dry-run: DEFERRED for a to-be-created FCR (recorded, not sent
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
                multicloudLinks,
                config.getCustomerAsn(),
                config.getFabric());
        validationErrors.addAll(validation.errors);

        return identity(config, DeploymentPlan.builder()
                .sourceOptimization(result)
                .cloudRouters(cloudRouters)
                .providerConnections(providerConnections)
                .backboneLinks(backboneLinks)
                .routingProtocols(routingProtocols)
                // null, not an empty list, when there is no link: a plan without native links is
                // field-for-field the plan the wizard produced before the list existed.
                .multicloudLinks(multicloudLinks.isEmpty() ? null : multicloudLinks)
                .interconnects(interconnects(config))
                .pricing(pricing)
                .valid(validationErrors.isEmpty())
                .validationErrors(validationErrors)
                .deferredValidations(validation.deferred)
                .skippedValidations(validation.skipped)
                .requiredInputs(validation.requiredInputs)
                .fabric(config.getFabric())
                .build());
    }

    /**
     * Stamps the plan's identity: deployment name (explicit, else the design spec's name, else the
     * prefix), name prefix, project, spec fingerprint and the optimization timestamp.
     */
    private static DeploymentPlan identity(DeploymentWizard.Builder config, DeploymentPlan plan) {
        String prefix = PlanNames.validatePrefix(config.getRouterNamePrefix());
        String deployment = config.getDeploymentName() != null ? config.getDeploymentName() : prefix;
        OptimizationResult result = config.getOptimizationResult();
        return plan.toBuilder()
                .deploymentName(deployment)
                .namePrefix(prefix)
                .projectId(config.getProjectId())
                // An explicit fingerprint wins; otherwise the plan inherits the fingerprint of the
                // DesignSpec the optimization ran on, so a plan always names the design it came from.
                .specFingerprint(config.getSpecFingerprint() != null ? config.getSpecFingerprint()
                        : (result != null ? result.getSpecFingerprint() : null))
                .sourceComputedAt(result != null ? result.getComputedAt() : null)
                .build();
    }

    /** The configured Interconnects, each given a logical key when it has none; {@code null} when none. */
    private static List<PlannedInterconnect> interconnects(DeploymentWizard.Builder config) {
        if (config.getInterconnects().isEmpty()) {
            return null;
        }
        List<PlannedInterconnect> out = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (PlannedInterconnect ic : config.getInterconnects()) {
            String base = ic.getKey() != null ? ic.getKey()
                    : "interconnect/" + (ic.getMetro() != null ? ic.getMetro().code() : "any") + "/"
                    + (ic.getName() != null ? ic.getName() : "ic");
            out.add(ic.toBuilder().key(uniqueKey(keys, base)).build());
        }
        return out;
    }

    /** {@code base}, or {@code base#2}, {@code base#3}, ... when already taken. */
    static String uniqueKey(Set<String> used, String base) {
        String key = base;
        int n = 2;
        while (!used.add(key)) {
            key = base + "#" + n++;
        }
        return key;
    }

    //  Phase 1: Cloud Routers

    private static List<PlannedCloudRouter> planCloudRouters(
            DeploymentWizard.Builder config, List<MetroRecommendation> metros,
            Map<MetroId, String> routerNames) {

        List<String> notificationEmails = notificationEmails(config);

        return metros.stream()
                .map(metro -> PlannedCloudRouter.builder()
                        .key("router/" + metro.getMetroId().code())
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
     * The FULL configured notification list, stamped verbatim onto every planned resource - the old
     * code took only {@code get(0)}, silently dropping every additional recipient the caller
     * supplied. {@code null} when none was configured (Layer-1 validation flags that).
     */
    private static List<String> notificationEmails(DeploymentWizard.Builder config) {
        List<String> emails = config.getNotificationEmails();
        return emails == null || emails.isEmpty() ? null : List.copyOf(emails);
    }

    //  Phase 2: Provider Connections

    private static List<PlannedConnection> planProviderConnections(
            DeploymentWizard.Builder config, List<MetroRecommendation> metros, OptimizationResult result,
            PlanNames names, Map<MetroId, String> routerNames, List<String> validationErrors) {

        List<PlannedConnection> connections = new ArrayList<>();
        List<String> notificationEmails = notificationEmails(config);
        Set<String> keys = new HashSet<>();

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
                // plus a COMPACT provider token - "aws", not "amazon-web-services" - then cap and dedupe
                // it so it clears Fabric's < 24-character limit (EQ-3142539) even for long prefixes.
                String connName = names.unique(routerName + "-to-" + PlanNames.providerToken(provider.getProviderLabel()));

                // Bandwidth-aware profile selection. The optimizer's single winner was chosen with no
                // knowledge of this bandwidth, so a hosted profile (capped at, say, 500 Mbps) can be the
                // default for a metro sized at 3000 Mbps. When the provider carries candidate profiles,
                // pick one whose tiers cover the bandwidth - ROUNDING UP to the smallest satisfying tier
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
                                .reasoning(bandwidth.getReasoning() + " | Rounded up " + requestedMbps + "->"
                                        + stampedBandwidth + " Mbps to the nearest service-profile tier (+"
                                        + selection.roundedUpByMbps() + " Mbps billable)")
                                .build();
                    }
                } else {
                    // Legacy / hand-built entry with no capability data: keep the pre-selected default and
                    // the raw requirement - there is no tier list to round against.
                    serviceProfileUuid = provider.getServiceProfileUuid();
                    sellerRegion = firstOrNull(provider.getSellerRegions());
                    stampedBandwidth = requestedMbps;
                }

                connections.add(PlannedConnection.builder()
                        .key(uniqueKey(keys, "connection/" + metro.getMetroId().code() + "/"
                                + PlanNames.providerToken(provider.getProviderLabel())))
                        .projectId(config.getProjectId())
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
     * of the requested speed on - <em>rounding up</em> to the smallest tier that satisfies the
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
     * after rounding up - the genuine over-capacity case (never the merely-non-exact case, which now
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
                + "rounding up to the nearest tier - " + ceilingClause + ". " + suggestion + ". Candidate "
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
            // normal per-workload aggregation for an unmatched (metro, provider) - NOT a fabricated
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
                    + ", so the documented fallback applied - sized by the normal per-workload "
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
    static int effectiveWorkloadBandwidth(WorkloadSpec spec) {
        int declared = spec.getBandwidthMbps();
        Double floor = spec.resolvedProfile() != null ? spec.resolvedProfile().getMinBandwidthMbps() : null;
        if (floor == null || !Double.isFinite(floor) || floor <= 0) {
            return declared;
        }
        return (int) Math.max(declared, Math.ceil(floor));
    }

    //  Phase 3: Backbone Links

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
                    .key("backbone/" + metroA.code() + "-" + metroZ.code())
                    .projectId(config.getProjectId())
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
                    // The naive modulo walk would emit BOTH (0,1) and (1,0) - two distinctly-named,
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

    //  Phase 4: Routing Protocols

    private static List<PlannedRoutingProtocol> planRoutingProtocols(
            DeploymentWizard.Builder config,
            List<PlannedConnection> providerConnections,
            List<PlannedBackboneLink> backboneLinks,
            PlanNames names, SubnetAllocator subnets) {

        List<PlannedRoutingProtocol> protocols = new ArrayList<>();

        // Protocols for provider connections
        for (PlannedConnection conn : providerConnections) {
            protocols.addAll(createProtocolPair(config, conn.getName(), conn.getKey(), names, subnets));
        }

        // Protocols for backbone links
        for (PlannedBackboneLink link : backboneLinks) {
            protocols.addAll(createProtocolPair(config, link.getConnection().getName(), link.getConnection().getKey(),
                    names, subnets));
        }

        return protocols;
    }

    private static List<PlannedRoutingProtocol> createProtocolPair(
            DeploymentWizard.Builder config, String connectionName, String connectionKey, PlanNames names,
            SubnetAllocator subnets) {

        List<PlannedRoutingProtocol> pair = new ArrayList<>(2);
        String subnet = subnets.next();

        // The protocol's own name must also clear Fabric's < 24-character limit, so it is composed
        // through the same generator (which truncates the connection stem and, if that collides,
        // appends a hash) rather than by raw string concatenation. connectionName stays the exact
        // parent-connection name - it is the foreign key execute() resolves the parent by.

        // DIRECT protocol (IP assignment)
        pair.add(PlannedRoutingProtocol.builder()
                .type(RoutingProtocolType.DIRECT)
                .key(connectionKey + "/rp/DIRECT")
                .name(names.uniqueWithSuffix(connectionName, "DIRECT"))
                .connectionName(connectionName)
                .equinixIfaceIpv4(subnet + ".1/30")
                .bfdEnabled(false)
                .bfdInterval(0)
                .build());

        // BGP protocol (dynamic routing)
        pair.add(PlannedRoutingProtocol.builder()
                .type(RoutingProtocolType.BGP)
                .key(connectionKey + "/rp/BGP")
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
     * concurrent plans - the embedded MCP server plans on server threads - never interleave into
     * each other's sequence, and a plan's addressing is deterministic for its configuration.
     *
     * <p>Each connection consumes one third-octet step of the configured base (default
     * {@code 10.100.0.0}): the first pair peers on {@code 10.100.0.1/30}÷{@code .2/30}, the next on
     * {@code 10.100.1.x}, carrying into the second octet past 256 connections - the exact sequence
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
}
