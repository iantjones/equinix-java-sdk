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

    private static int subnetCounter = 0;

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

        subnetCounter = 0;

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
        List<PlannedRoutingProtocol> routingProtocols = planRoutingProtocols(config, providerConnections, backboneLinks, names);

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

        String notificationEmail = config.getNotificationEmails().isEmpty()
                ? null : config.getNotificationEmails().get(0);

        return metros.stream()
                .map(metro -> PlannedCloudRouter.builder()
                        .metroId(metro.getMetroId())
                        .name(routerNames.get(metro.getMetroId()))
                        .packageCode(config.getRouterPackage())
                        .accountNumber(config.getAccountNumber())
                        .projectId(config.getProjectId())
                        .notificationEmail(notificationEmail)
                        .build())
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════
    //  Phase 2: Provider Connections
    // ══════════════════════════════════════════════

    private static List<PlannedConnection> planProviderConnections(
            DeploymentWizard.Builder config, List<MetroRecommendation> metros, OptimizationResult result,
            PlanNames names, Map<MetroId, String> routerNames, List<String> validationErrors) {

        List<PlannedConnection> connections = new ArrayList<>();
        String notificationEmail = config.getNotificationEmails().isEmpty()
                ? null : config.getNotificationEmails().get(0);

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
                // pick one whose allowed tiers actually cover the bandwidth (a dedicated profile when the
                // speed exceeds the hosted maximum). If NONE covers it, the connection is not buildable
                // as-is: record a precise, actionable error and do not emit an unbuildable connection.
                String serviceProfileUuid;
                String sellerRegion;
                List<ServiceProfileOption> options = provider.getProfileOptions();
                if (options != null && !options.isEmpty()) {
                    Optional<ServiceProfileOption> chosen =
                            chooseProfileForBandwidth(provider, bandwidth.getTotalMbps());
                    if (chosen.isEmpty()) {
                        validationErrors.add(noCoveringProfileError(
                                connName, provider, metro.getMetroId(), bandwidth.getTotalMbps(), options));
                        continue;
                    }
                    serviceProfileUuid = chosen.get().getServiceProfileUuid();
                    sellerRegion = firstOrNull(chosen.get().getSellerRegions());
                } else {
                    // Legacy / hand-built entry with no capability data: keep the pre-selected default.
                    serviceProfileUuid = provider.getServiceProfileUuid();
                    sellerRegion = firstOrNull(provider.getSellerRegions());
                }

                connections.add(PlannedConnection.builder()
                        .name(connName)
                        .connectionType(config.getProviderConnectionType())
                        .purpose(ConnectionPurpose.PROVIDER)
                        .bandwidthMbps(bandwidth.getTotalMbps())
                        .bandwidthAllocation(bandwidth)
                        .aSideMetro(metro.getMetroId())
                        .aSideRouterName(routerName)
                        .zSideServiceProfileUuid(serviceProfileUuid)
                        .zSideProviderLabel(provider.getProviderLabel())
                        .zSideSellerRegion(sellerRegion)
                        // Resolve the provider label to a typed cloud provider so the plan can
                        // enumerate the exact authorization key the connection will need at
                        // provisioning (AWS Account ID, Azure service key, GCP pairing key, ...).
                        .zSideCloudType(ConnectionBodies.resolveCloudType(provider.getProviderLabel()))
                        .notificationEmail(notificationEmail)
                        .build());
            }
        }

        return connections;
    }

    /**
     * Picks, among a provider's candidate service profiles for a metro, one whose allowed bandwidths
     * cover the requested speed. Policy: prefer the smallest-capable covering profile (so a 300&nbsp;Mbps
     * connection takes a hosted profile and a 10000&nbsp;Mbps connection takes a dedicated one rather
     * than the reverse), breaking ties by fewest wasted tiers, then by preferring the optimizer's
     * default winner (which preserves its seller-region preference), then by the lowest uuid so the
     * choice is deterministic regardless of catalog order.
     *
     * @param provider the available provider entry, carrying its candidate {@code profileOptions}
     * @param mbps     the computed connection bandwidth in Mbps
     * @return the chosen profile, or empty when no candidate can carry {@code mbps}
     */
    private static Optional<ServiceProfileOption> chooseProfileForBandwidth(
            ProviderAvailability provider, int mbps) {
        List<ServiceProfileOption> options = provider.getProfileOptions();
        if (options == null || options.isEmpty()) {
            return Optional.empty();
        }
        String defaultUuid = provider.getServiceProfileUuid();
        return options.stream()
                .filter(option -> option.covers(mbps))
                .min(Comparator
                        .comparingInt(ServiceProfileOption::capacityCeiling)
                        .thenComparingInt((ServiceProfileOption o) -> o.excessTiersAbove(mbps))
                        .thenComparingInt(o -> Objects.equals(o.getServiceProfileUuid(), defaultUuid) ? 0 : 1)
                        .thenComparing(ServiceProfileOption::getServiceProfileUuid,
                                Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * A precise, actionable error for a connection whose bandwidth no available profile offers: it
     * names the provider, metro and requested bandwidth, lists what each candidate DOES offer, and
     * (when discrete tiers exist) suggests the supported bandwidths — the honest outcome, since
     * silently snapping to a nearby tier would change the customer's stated intent.
     */
    private static String noCoveringProfileError(String connName, ProviderAvailability provider,
                                                 MetroId metro, int mbps, List<ServiceProfileOption> options) {
        List<Integer> offered = options.stream()
                .filter(o -> o.getSupportedBandwidths() != null)
                .flatMap(o -> o.getSupportedBandwidths().stream())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        String suggestion = offered.isEmpty()
                ? "choose a bandwidth within a candidate profile's ceiling, or split the workload across "
                    + "multiple connections"
                : "choose a supported bandwidth " + offered + ", or split the workload across multiple connections";
        return "Connection '" + connName + "' to provider '" + provider.getProviderLabel() + "' at metro "
                + metro + ": bandwidth " + mbps + " Mbps is not offered by any available service profile — "
                + describeProfileOptions(options) + ". " + suggestion + ".";
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

        if (strategy == BandwidthStrategy.CUSTOM && config.getCustomBandwidthMap() != null) {
            // Match the documented key forms, most specific first: "<metroId>-<providerLabel>" (a
            // per-metro-per-provider override) then "<providerLabel>" (that provider in every metro).
            // The old code looked up ONLY the compound key with the full provider label, so a caller
            // whose real provider label differs from the key they wrote silently matched nothing. The
            // provider-label form is now honoured too, and — crucially — the reasoning states plainly
            // whether the map actually supplied the value or the documented default was used, instead
            // of labelling the default as though it came from the caller's map.
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
            int defaultBw = 1000;
            return BandwidthAllocation.builder()
                    .totalMbps(defaultBw)
                    .perWorkload(Collections.singletonMap("custom", defaultBw))
                    .reasoning("Custom strategy: no bandwidth-map key matched '" + perMetroKey + "' or '"
                            + providerKey + "', so the documented " + defaultBw + " Mbps default was used")
                    .build();
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

            if (strategy == BandwidthStrategy.PER_WORKLOAD && !dependsOnProvider) {
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

        String reasoning = strategy == BandwidthStrategy.PER_WORKLOAD
                ? "Sum of dependent workload bandwidths at " + metro.getMetroId()
                : "Aggregated bandwidth for all workloads at " + metro.getMetroId();

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
        String notificationEmail = config.getNotificationEmails().isEmpty()
                ? null : config.getNotificationEmails().get(0);

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
                    .notificationEmail(notificationEmail)
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
            PlanNames names) {

        List<PlannedRoutingProtocol> protocols = new ArrayList<>();

        // Protocols for provider connections
        for (PlannedConnection conn : providerConnections) {
            protocols.addAll(createProtocolPair(config, conn.getName(), names));
        }

        // Protocols for backbone links
        for (PlannedBackboneLink link : backboneLinks) {
            protocols.addAll(createProtocolPair(config, link.getConnection().getName(), names));
        }

        return protocols;
    }

    private static List<PlannedRoutingProtocol> createProtocolPair(
            DeploymentWizard.Builder config, String connectionName, PlanNames names) {

        List<PlannedRoutingProtocol> pair = new ArrayList<>(2);
        String subnet = nextSubnet();

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

    private static String nextSubnet() {
        int index = subnetCounter++;
        int third = index % 256;
        int second = 100 + (index / 256);
        return "10." + second + "." + third;
    }

    // ══════════════════════════════════════════════
    //  Phase 5: Pricing
    // ══════════════════════════════════════════════

    private static PlanPricing estimatePricing(
            DeploymentWizard.Builder config,
            List<PlannedCloudRouter> routers,
            List<PlannedConnection> providerConnections,
            List<PlannedBackboneLink> backboneLinks) {

        RateCard rateCard = resolveRateCard(config);
        Term term = config.getTerm();

        Map<String, BigDecimal> perConnectionCost = new LinkedHashMap<>();
        Set<PriceSource> sources = new HashSet<>();
        // Every priced line flows through one reconciler. A plan can legitimately span metros in
        // different currencies (live Fabric pricing quotes EUR in Frankfurt, USD in Ashburn, ...), and
        // summing those into one monthly total would be a fabricated cross-currency figure.
        CurrencyReconciler recon = CurrencyReconciler.create();

        // Cloud Routers
        BigDecimal routerMonthly = BigDecimal.ZERO;
        BigDecimal routerSetup = BigDecimal.ZERO;
        for (PlannedCloudRouter router : routers) {
            PriceQuote quote = priceRouter(rateCard, router, term);
            routerMonthly = routerMonthly.add(quote.getMonthlyRecurring());
            routerSetup = routerSetup.add(quote.getNonRecurring());
            recon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            sources.add(quote.getSource());
        }

        // Provider connections
        BigDecimal providerMonthly = BigDecimal.ZERO;
        BigDecimal providerSetup = BigDecimal.ZERO;
        for (PlannedConnection conn : providerConnections) {
            PriceQuote quote = priceConnection(rateCard, conn.getConnectionType(),
                    conn.getBandwidthMbps(), conn.getASideMetro(), term);
            providerMonthly = providerMonthly.add(quote.getMonthlyRecurring());
            providerSetup = providerSetup.add(quote.getNonRecurring());
            perConnectionCost.put(conn.getName(), quote.getMonthlyRecurring());
            recon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            sources.add(quote.getSource());
        }

        // Backbone links
        BigDecimal backboneMonthly = BigDecimal.ZERO;
        BigDecimal backboneSetup = BigDecimal.ZERO;
        for (PlannedBackboneLink link : backboneLinks) {
            ConnectionType type = link.getConnection() != null ? link.getConnection().getConnectionType() : null;
            PriceQuote quote = priceConnection(rateCard, type, link.getBandwidthMbps(), link.getMetroA(), term);
            backboneMonthly = backboneMonthly.add(quote.getMonthlyRecurring());
            backboneSetup = backboneSetup.add(quote.getNonRecurring());
            perConnectionCost.put(link.getName(), quote.getMonthlyRecurring());
            recon.add(quote.getCurrency(), quote.getMonthlyRecurring(), quote.getNonRecurring());
            sources.add(quote.getSource());
        }

        PriceSource source = dominantSource(sources);
        boolean authoritative = source == PriceSource.EQUINIX_LIVE || source == PriceSource.CUSTOM;

        if (recon.isMixed()) {
            // Mixed currencies: do not fabricate a combined total. Omit the monthly/setup totals and
            // the single currency, surface the per-currency subtotals, and keep the per-category and
            // per-connection figures (each valid in its own currency).
            String mixDisclaimer = "This plan's connections are priced in multiple currencies ("
                    + recon.describeCurrencies() + "): " + recon.describeMonthlySubtotals() + " per month. A single "
                    + "plan total cannot be formed without an FX rate, so the monthly and setup totals are omitted "
                    + "rather than reported as a fabricated cross-currency sum; the per-connection and per-category "
                    + "figures are each shown in their own currency.";
            return PlanPricing.builder()
                    .monthlyTotal(null)
                    .setupTotal(null)
                    .currency(null)
                    .routerMonthlyCost(routerMonthly)
                    .providerConnectionMonthlyCost(providerMonthly)
                    .backboneMonthlyCost(backboneMonthly)
                    .perConnectionCost(perConnectionCost)
                    .source(source)
                    .disclaimer(mixDisclaimer)
                    .build();
        }

        String currency = recon.soleCurrencyOr("USD");
        BigDecimal monthlyTotal = recon.monthlyTotal().orElse(BigDecimal.ZERO);
        BigDecimal setupTotal = recon.setupTotal().orElse(BigDecimal.ZERO);

        String disclaimer = authoritative
                ? "Based on live Equinix Fabric pricing. Actual costs may vary based on contract terms, volume "
                    + "discounts, and promotional offers."
                : "Some or all figures are heuristic or reference estimates (live Fabric pricing was unavailable "
                    + "for part of this plan). Actual costs may vary; contact your Equinix account team for precise quotes.";

        return PlanPricing.builder()
                .monthlyTotal(monthlyTotal)
                .setupTotal(setupTotal)
                .currency(currency)
                .routerMonthlyCost(routerMonthly)
                .providerConnectionMonthlyCost(providerMonthly)
                .backboneMonthlyCost(backboneMonthly)
                .perConnectionCost(perConnectionCost)
                .source(source)
                .disclaimer(disclaimer)
                .build();
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
