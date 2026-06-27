package api.equinix.javasdk.design.optimizer.wizard;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.mcp.bridge.McpBridge;
import api.equinix.javasdk.mcp.bridge.McpConnectionBridge;
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

        // Phase 1: Plan Cloud Routers
        List<PlannedCloudRouter> cloudRouters = planCloudRouters(config, metros);

        // Phase 2: Plan Provider Connections
        List<PlannedConnection> providerConnections = planProviderConnections(config, metros, result);

        // Phase 2b: MCP Validation (optional)
        mcpValidateConnections(config.getMcpBridge(), providerConnections, validationErrors);

        // Phase 3: Plan Backbone Links
        List<PlannedBackboneLink> backboneLinks = planBackboneLinks(config, metros);

        // Phase 4: Plan Routing Protocols
        List<PlannedRoutingProtocol> routingProtocols = planRoutingProtocols(config, providerConnections, backboneLinks);

        // Phase 5: Estimate Pricing
        PlanPricing pricing = estimatePricing(config, cloudRouters, providerConnections, backboneLinks);

        // Validate
        validate(cloudRouters, providerConnections, backboneLinks, validationErrors);

        return DeploymentPlan.builder()
                .sourceOptimization(result)
                .cloudRouters(cloudRouters)
                .providerConnections(providerConnections)
                .backboneLinks(backboneLinks)
                .routingProtocols(routingProtocols)
                .pricing(pricing)
                .valid(validationErrors.isEmpty())
                .validationErrors(validationErrors)
                .fabric(config.getFabric())
                .build();
    }

    // ══════════════════════════════════════════════
    //  Phase 1: Cloud Routers
    // ══════════════════════════════════════════════

    private static List<PlannedCloudRouter> planCloudRouters(
            DeploymentWizard.Builder config, List<MetroRecommendation> metros) {

        String notificationEmail = config.getNotificationEmails().isEmpty()
                ? null : config.getNotificationEmails().get(0);

        return metros.stream()
                .map(metro -> PlannedCloudRouter.builder()
                        .metroCode(metro.getMetroCode())
                        .name(config.getRouterNamePrefix() + "-" + metro.getMetroCode())
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
            DeploymentWizard.Builder config, List<MetroRecommendation> metros, OptimizationResult result) {

        List<PlannedConnection> connections = new ArrayList<>();
        String notificationEmail = config.getNotificationEmails().isEmpty()
                ? null : config.getNotificationEmails().get(0);

        for (MetroRecommendation metro : metros) {
            if (metro.getAvailableProviders() == null) continue;

            List<ProviderAvailability> availableProviders = metro.getAvailableProviders().stream()
                    .filter(ProviderAvailability::isAvailable)
                    .collect(Collectors.toList());

            for (ProviderAvailability provider : availableProviders) {
                BandwidthAllocation bandwidth = computeBandwidth(
                        config, metro, provider, result);

                if (bandwidth.getTotalMbps() <= 0) continue;

                String connName = config.getRouterNamePrefix() + "-" + metro.getMetroCode()
                        + "-to-" + sanitizeName(provider.getProviderLabel());

                String sellerRegion = provider.getSellerRegions() != null && !provider.getSellerRegions().isEmpty()
                        ? provider.getSellerRegions().get(0) : null;

                connections.add(PlannedConnection.builder()
                        .name(connName)
                        .connectionType(config.getProviderConnectionType())
                        .purpose(ConnectionPurpose.PROVIDER)
                        .bandwidthMbps(bandwidth.getTotalMbps())
                        .bandwidthAllocation(bandwidth)
                        .aSideMetro(metro.getMetroCode())
                        .aSideRouterName(config.getRouterNamePrefix() + "-" + metro.getMetroCode())
                        .zSideServiceProfileUuid(provider.getServiceProfileUuid())
                        .zSideProviderLabel(provider.getProviderLabel())
                        .zSideSellerRegion(sellerRegion)
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
            String key = metro.getMetroCode() + "-" + provider.getProviderLabel();
            int customBw = config.getCustomBandwidthMap().getOrDefault(key, 1000);
            return BandwidthAllocation.builder()
                    .totalMbps(customBw)
                    .perWorkload(Collections.singletonMap("custom", customBw))
                    .reasoning("Custom bandwidth map: " + customBw + " Mbps")
                    .build();
        }

        // Determine workloads at this metro
        List<WorkloadPlacement> placements = result.getTopology() != null
                ? result.getTopology().forMetro(metro.getMetroCode())
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
                ? "Sum of dependent workload bandwidths at " + metro.getMetroCode()
                : "Aggregated bandwidth for all workloads at " + metro.getMetroCode();

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
            DeploymentWizard.Builder config, List<MetroRecommendation> metros) {

        if (metros.size() < 2) return Collections.emptyList();

        List<PlannedBackboneLink> links = new ArrayList<>();
        BackboneTopology topology = config.getBackboneTopology();
        String notificationEmail = config.getNotificationEmails().isEmpty()
                ? null : config.getNotificationEmails().get(0);

        List<MetroCode> metroCodes = metros.stream()
                .map(MetroRecommendation::getMetroCode)
                .collect(Collectors.toList());

        List<int[]> pairs = computeTopologyPairs(topology, metroCodes.size());

        for (int[] pair : pairs) {
            MetroCode metroA = metroCodes.get(pair[0]);
            MetroCode metroZ = metroCodes.get(pair[1]);
            String linkName = config.getRouterNamePrefix() + "-" + metroA + "-to-" + metroZ;

            PlannedConnection connection = PlannedConnection.builder()
                    .name(linkName)
                    .connectionType(config.getBackboneConnectionType())
                    .purpose(ConnectionPurpose.BACKBONE)
                    .bandwidthMbps(config.getBackboneBandwidthMbps())
                    .aSideMetro(metroA)
                    .aSideRouterName(config.getRouterNamePrefix() + "-" + metroA)
                    .zSideMetro(metroZ)
                    .zSideRouterName(config.getRouterNamePrefix() + "-" + metroZ)
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
            List<PlannedBackboneLink> backboneLinks) {

        List<PlannedRoutingProtocol> protocols = new ArrayList<>();

        // Protocols for provider connections
        for (PlannedConnection conn : providerConnections) {
            protocols.addAll(createProtocolPair(config, conn.getName()));
        }

        // Protocols for backbone links
        for (PlannedBackboneLink link : backboneLinks) {
            protocols.addAll(createProtocolPair(config, link.getConnection().getName()));
        }

        return protocols;
    }

    private static List<PlannedRoutingProtocol> createProtocolPair(
            DeploymentWizard.Builder config, String connectionName) {

        List<PlannedRoutingProtocol> pair = new ArrayList<>(2);
        String subnet = nextSubnet();

        // DIRECT protocol (IP assignment)
        pair.add(PlannedRoutingProtocol.builder()
                .type(RoutingProtocolType.DIRECT)
                .name(connectionName + "-DIRECT")
                .connectionName(connectionName)
                .equinixIfaceIpv4(subnet + ".1/30")
                .bfdEnabled(false)
                .bfdInterval(0)
                .build());

        // BGP protocol (dynamic routing)
        pair.add(PlannedRoutingProtocol.builder()
                .type(RoutingProtocolType.BGP)
                .name(connectionName + "-BGP")
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

        // Cloud Routers
        BigDecimal routerMonthly = BigDecimal.ZERO;
        BigDecimal routerSetup = BigDecimal.ZERO;
        for (PlannedCloudRouter router : routers) {
            PriceQuote quote = priceRouter(rateCard, router, term);
            routerMonthly = routerMonthly.add(quote.getMonthlyRecurring());
            routerSetup = routerSetup.add(quote.getNonRecurring());
            currency = firstNonNull(currency, quote.getCurrency());
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
        }

        if (currency == null) {
            currency = Currency.getInstance("USD");
        }

        BigDecimal monthlyTotal = routerMonthly.add(providerMonthly).add(backboneMonthly);
        BigDecimal setupTotal = routerSetup.add(providerSetup).add(backboneSetup);

        return PlanPricing.builder()
                .monthlyTotal(monthlyTotal)
                .setupTotal(setupTotal)
                .currency(currency.getCurrencyCode())
                .routerMonthlyCost(routerMonthly)
                .providerConnectionMonthlyCost(providerMonthly)
                .backboneMonthlyCost(backboneMonthly)
                .perConnectionCost(perConnectionCost)
                .build();
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
     * Prices a single connection via the rate card, falling back to the legacy
     * tiered heuristic (tagged {@link PriceSource#ESTIMATE}) when the card
     * cannot resolve a price.
     */
    private static PriceQuote priceConnection(RateCard rateCard, ConnectionType type,
                                              int bandwidthMbps, MetroCode metro, Term term) {
        if (rateCard != null) {
            Optional<PriceQuote> quote = rateCard.connection(type, bandwidthMbps, metro, term);
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
            Optional<PriceQuote> quote = rateCard.cloudRouter(router.getPackageCode(), router.getMetroCode(), term);
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

    // ══════════════════════════════════════════════
    //  Validation
    // ══════════════════════════════════════════════

    private static void validate(
            List<PlannedCloudRouter> routers,
            List<PlannedConnection> connections,
            List<PlannedBackboneLink> links,
            List<String> errors) {

        if (routers.isEmpty()) {
            errors.add("No Cloud Routers planned — at least one metro recommendation is required");
        }

        // Verify all connections reference valid router names
        Set<String> routerNames = routers.stream()
                .map(PlannedCloudRouter::getName)
                .collect(Collectors.toSet());

        for (PlannedConnection conn : connections) {
            if (!routerNames.contains(conn.getASideRouterName())) {
                errors.add("Connection '" + conn.getName() + "' references unknown router: " + conn.getASideRouterName());
            }
        }

        for (PlannedBackboneLink link : links) {
            if (!routerNames.contains(link.getConnection().getASideRouterName())) {
                errors.add("Backbone link '" + link.getName() + "' references unknown A-side router: "
                        + link.getConnection().getASideRouterName());
            }
            if (!routerNames.contains(link.getConnection().getZSideRouterName())) {
                errors.add("Backbone link '" + link.getName() + "' references unknown Z-side router: "
                        + link.getConnection().getZSideRouterName());
            }
        }
    }

    private static String sanitizeName(String input) {
        return input.replaceAll("[^a-zA-Z0-9_-]", "-").toLowerCase();
    }

    /**
     * Validates planned connections against the MCP server if available.
     * Any validation failures are added as warnings to the validation errors list.
     */
    private static void mcpValidateConnections(McpBridge mcpBridge,
                                                List<PlannedConnection> connections,
                                                List<String> validationErrors) {
        if (mcpBridge == null || connections == null || connections.isEmpty()) {
            return;
        }

        try {
            for (PlannedConnection conn : connections) {
                try {
                    Map<String, Object> spec = new HashMap<>();
                    spec.put("type", conn.getConnectionType() != null ? conn.getConnectionType().toString() : "EVPL_VC");
                    spec.put("name", conn.getName());
                    spec.put("bandwidth", conn.getBandwidthMbps());

                    McpConnectionBridge.McpConnectionValidation validation =
                            mcpBridge.connections().validateConnection(spec);

                    if (!validation.isValid()) {
                        validationErrors.add("MCP validation warning for '" + conn.getName()
                                + "': " + validation.getMessage());
                    }
                } catch (Exception e) {
                    // Individual connection validation failures should not block the plan
                }
            }
        } catch (Exception e) {
            // MCP validation is optional; continue without it
        }
    }
}
