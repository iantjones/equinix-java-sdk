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

        // Phase 2: Plan Provider Connections
        List<PlannedConnection> providerConnections = planProviderConnections(config, metros, result, names, routerNames);

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
            PlanNames names, Map<MetroId, String> routerNames) {

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

                String sellerRegion = provider.getSellerRegions() != null && !provider.getSellerRegions().isEmpty()
                        ? provider.getSellerRegions().get(0) : null;

                connections.add(PlannedConnection.builder()
                        .name(connName)
                        .connectionType(config.getProviderConnectionType())
                        .purpose(ConnectionPurpose.PROVIDER)
                        .bandwidthMbps(bandwidth.getTotalMbps())
                        .bandwidthAllocation(bandwidth)
                        .aSideMetro(metro.getMetroId())
                        .aSideRouterName(routerName)
                        .zSideServiceProfileUuid(provider.getServiceProfileUuid())
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

    private static BandwidthAllocation computeBandwidth(
            DeploymentWizard.Builder config,
            MetroRecommendation metro,
            ProviderAvailability provider,
            OptimizationResult result) {

        BandwidthStrategy strategy = config.getBandwidthStrategy();
        Map<String, Integer> perWorkload = new LinkedHashMap<>();

        if (strategy == BandwidthStrategy.CUSTOM && config.getCustomBandwidthMap() != null) {
            String key = metro.getMetroId() + "-" + provider.getProviderLabel();
            int customBw = config.getCustomBandwidthMap().getOrDefault(key, 1000);
            return BandwidthAllocation.builder()
                    .totalMbps(customBw)
                    .perWorkload(Collections.singletonMap("custom", customBw))
                    .reasoning("Custom bandwidth map: " + customBw + " Mbps")
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

            int bw = spec.getBandwidthMbps() > 0 ? spec.getBandwidthMbps() : 1000;
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
        Currency currency = null;
        Set<PriceSource> sources = new HashSet<>();

        // Cloud Routers
        BigDecimal routerMonthly = BigDecimal.ZERO;
        BigDecimal routerSetup = BigDecimal.ZERO;
        for (PlannedCloudRouter router : routers) {
            PriceQuote quote = priceRouter(rateCard, router, term);
            routerMonthly = routerMonthly.add(quote.getMonthlyRecurring());
            routerSetup = routerSetup.add(quote.getNonRecurring());
            currency = firstNonNull(currency, quote.getCurrency());
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
            currency = firstNonNull(currency, quote.getCurrency());
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
            currency = firstNonNull(currency, quote.getCurrency());
            sources.add(quote.getSource());
        }

        if (currency == null) {
            currency = Currency.getInstance("USD");
        }

        BigDecimal monthlyTotal = routerMonthly.add(providerMonthly).add(backboneMonthly);
        BigDecimal setupTotal = routerSetup.add(providerSetup).add(backboneSetup);

        PriceSource source = dominantSource(sources);
        boolean authoritative = source == PriceSource.EQUINIX_LIVE || source == PriceSource.CUSTOM;
        String disclaimer = authoritative
                ? "Based on live Equinix Fabric pricing. Actual costs may vary based on contract terms, volume "
                    + "discounts, and promotional offers."
                : "Some or all figures are heuristic or reference estimates (live Fabric pricing was unavailable "
                    + "for part of this plan). Actual costs may vary; contact your Equinix account team for precise quotes.";

        return PlanPricing.builder()
                .monthlyTotal(monthlyTotal)
                .setupTotal(setupTotal)
                .currency(currency.getCurrencyCode())
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

    private static Currency firstNonNull(Currency current, Currency candidate) {
        return current != null ? current : candidate;
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
