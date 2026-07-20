package api.equinix.javasdk.design.optimizer.wizard;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entry point for the Deployment Wizard. Takes an {@link OptimizationResult} and produces
 * an executable {@link DeploymentPlan} with Cloud Routers, provider connections, inter-metro
 * backbone links, and routing protocol configurations — all with bandwidth sizing that
 * drives accurate pricing.
 *
 * <h3>Example Usage</h3>
 * <pre>{@code
 * DeploymentPlan plan = fabric.deploymentWizard(optimizationResult)
 *     .routerPackage("STANDARD")
 *     .routerNamePrefix("FCR")
 *     .providerConnectionType(ConnectionType.EVPL_VC)
 *     .backboneBandwidthMbps(10_000)
 *     .backboneTopology(BackboneTopology.FULL_MESH)
 *     .bandwidthStrategy(BandwidthStrategy.PER_WORKLOAD)
 *     .customerAsn(65100L)
 *     .withBFD(true, 300)
 *     .accountNumber(272010L)
 *     .projectId("your-project-uuid")
 *     .notifications("noc@example.com")
 *     .plan();
 *
 * System.out.println(plan.toMarkdown());
 * plan.dryRun();
 * DeploymentOutcome outcome = plan.execute();
 * }</pre>
 *
 * @see DeploymentPlan
 * @see DeploymentWizardEngine
 */
public final class DeploymentWizard {

    private DeploymentWizard() {}

    /**
     * Creates a new deployment wizard builder from an optimization result.
     *
     * @param fabric the authenticated Fabric client for API access and execution
     * @param optimizationResult the optimization result to transform into a deployment plan
     * @return a new {@link Builder} instance
     */
    public static Builder builder(FabricGateway fabric, OptimizationResult optimizationResult) {
        return new Builder(fabric, optimizationResult);
    }

    /**
     * Fluent builder for configuring a deployment plan. Provides methods to set
     * Cloud Router package, connection types, backbone topology, bandwidth strategy,
     * routing protocol settings, and account details.
     */
    public static final class Builder {

        private final FabricGateway fabric;
        private final OptimizationResult optimizationResult;

        /** The deployable package tiers — every {@link GatewayPackageCode} except {@code UNKNOWN}. */
        private static final String VALID_PACKAGE_CODES = Stream.of(GatewayPackageCode.values())
                .filter(code -> code != GatewayPackageCode.UNKNOWN)
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        // Cloud Router settings
        private GatewayPackageCode routerPackage = GatewayPackageCode.STANDARD;
        private String routerNamePrefix = "FCR";

        // Connection settings
        private ConnectionType providerConnectionType = ConnectionType.EVPL_VC;
        private ConnectionType backboneConnectionType = ConnectionType.EVPL_VC;

        // Backbone settings
        private int backboneBandwidthMbps = 10_000;
        private BackboneTopology backboneTopology = BackboneTopology.FULL_MESH;

        // Bandwidth strategy
        private BandwidthStrategy bandwidthStrategy = BandwidthStrategy.PER_WORKLOAD;
        private Map<String, Integer> customBandwidthMap;

        // Routing protocol settings
        private Long customerAsn = 65100L;
        private boolean bfdEnabled = true;
        private int bfdInterval = 300;

        // Account settings
        private Long accountNumber;
        private String projectId;
        private List<String> notificationEmails = new ArrayList<>();

        // Pricing
        private RateCard rateCard;
        private Term term = Term.MONTH_12;

        Builder(FabricGateway fabric, OptimizationResult optimizationResult) {
            this.fabric = fabric;
            this.optimizationResult = optimizationResult;
        }

        // ── Cloud Router Settings ──

        /**
         * Sets the Cloud Router package code from its string form, leniently: the value is
         * trimmed and upper-cased before being resolved (so {@code "standard"} and
         * {@code " Premium "} both work). Defaults to {@link GatewayPackageCode#STANDARD}.
         *
         * <p>The code is validated here — once, at plan time — so a bad value fails fast
         * instead of surfacing as an {@code IllegalArgumentException} mid-deployment in
         * {@link DeploymentPlan#execute()} after some routers have already been provisioned.</p>
         *
         * @param packageCode the router package code (case-insensitive)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the code does not name a deployable package tier
         */
        public Builder routerPackage(String packageCode) {
            return routerPackage(resolvePackageCode(packageCode));
        }

        /**
         * Sets the Cloud Router package code. Defaults to {@link GatewayPackageCode#STANDARD}.
         *
         * @param packageCode the router package tier
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the code is {@code null} or
         *         {@link GatewayPackageCode#UNKNOWN} (a placeholder that must never be sent to the API)
         */
        public Builder routerPackage(GatewayPackageCode packageCode) {
            if (packageCode == null || packageCode == GatewayPackageCode.UNKNOWN) {
                throw new IllegalArgumentException("Invalid Cloud Router package code: " + packageCode
                        + ". Valid codes: " + VALID_PACKAGE_CODES + ".");
            }
            this.routerPackage = packageCode;
            return this;
        }

        /**
         * Resolves a free-form package-code string to its {@link GatewayPackageCode} constant,
         * tolerating whitespace and any casing. {@code UNKNOWN} (a deserialization placeholder,
         * never a deployable tier) and unrecognized values are rejected with a message naming
         * the valid codes.
         */
        private static GatewayPackageCode resolvePackageCode(String packageCode) {
            if (packageCode != null) {
                String normalized = packageCode.trim().toUpperCase(Locale.ROOT);
                for (GatewayPackageCode candidate : GatewayPackageCode.values()) {
                    if (candidate != GatewayPackageCode.UNKNOWN && candidate.name().equals(normalized)) {
                        return candidate;
                    }
                }
            }
            throw new IllegalArgumentException("Unknown Cloud Router package code '" + packageCode
                    + "'. Valid codes: " + VALID_PACKAGE_CODES + ".");
        }

        /**
         * Sets the naming prefix for Cloud Routers. Each router will be named
         * "{prefix}-{metroCode}" (e.g., "FCR-DC"). Defaults to "FCR".
         *
         * @param prefix the router name prefix
         * @return this builder for method chaining
         */
        public Builder routerNamePrefix(String prefix) {
            this.routerNamePrefix = prefix;
            return this;
        }

        // ── Connection Settings ──

        /**
         * Sets the Fabric connection type for provider connections. Defaults to EVPL_VC.
         *
         * @param type the connection type for provider connections
         * @return this builder for method chaining
         */
        public Builder providerConnectionType(ConnectionType type) {
            this.providerConnectionType = type;
            return this;
        }

        /**
         * Sets the Fabric connection type for backbone links. Defaults to EVPL_VC.
         *
         * @param type the connection type for backbone links
         * @return this builder for method chaining
         */
        public Builder backboneConnectionType(ConnectionType type) {
            this.backboneConnectionType = type;
            return this;
        }

        // ── Backbone Settings ──

        /**
         * Sets the bandwidth for inter-metro backbone links in Mbps. Defaults to 10,000 Mbps.
         *
         * @param mbps the backbone bandwidth in megabits per second
         * @return this builder for method chaining
         */
        public Builder backboneBandwidthMbps(int mbps) {
            this.backboneBandwidthMbps = mbps;
            return this;
        }

        /**
         * Sets the backbone topology for inter-metro connections.
         * Defaults to {@link BackboneTopology#FULL_MESH}.
         *
         * @param topology the inter-metro backbone topology
         * @return this builder for method chaining
         * @see BackboneTopology
         */
        public Builder backboneTopology(BackboneTopology topology) {
            this.backboneTopology = topology;
            return this;
        }

        // ── Bandwidth Strategy ──

        /**
         * Sets the bandwidth sizing strategy for provider connections.
         * Defaults to {@link BandwidthStrategy#PER_WORKLOAD}.
         *
         * @param strategy the bandwidth sizing strategy
         * @return this builder for method chaining
         * @see BandwidthStrategy
         */
        public Builder bandwidthStrategy(BandwidthStrategy strategy) {
            this.bandwidthStrategy = strategy;
            return this;
        }

        /**
         * Sets a custom bandwidth map for {@link BandwidthStrategy#CUSTOM}.
         * Keys are connection names or provider labels, values are bandwidth in Mbps.
         *
         * @param bandwidthMap the custom bandwidth mapping
         * @return this builder for method chaining
         */
        public Builder customBandwidthMap(Map<String, Integer> bandwidthMap) {
            this.customBandwidthMap = new HashMap<>(bandwidthMap);
            this.bandwidthStrategy = BandwidthStrategy.CUSTOM;
            return this;
        }

        // ── Routing Protocol Settings ──

        /**
         * Sets the customer ASN for BGP peering. Defaults to 65100.
         *
         * @param asn the customer autonomous system number
         * @return this builder for method chaining
         */
        public Builder customerAsn(long asn) {
            this.customerAsn = asn;
            return this;
        }

        /**
         * Configures BFD (Bidirectional Forwarding Detection) for routing protocols.
         * Defaults to enabled with 300ms interval.
         *
         * @param enabled whether BFD is enabled
         * @param intervalMs the BFD interval in milliseconds
         * @return this builder for method chaining
         */
        public Builder withBFD(boolean enabled, int intervalMs) {
            this.bfdEnabled = enabled;
            this.bfdInterval = intervalMs;
            return this;
        }

        // ── Account Settings ──

        /**
         * Sets the account number for billing. Applied to all created resources.
         *
         * @param accountNumber the Equinix account number
         * @return this builder for method chaining
         */
        public Builder accountNumber(long accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        /**
         * Sets the project UUID for resource grouping. Applied to all created resources.
         *
         * @param projectId the Equinix project UUID
         * @return this builder for method chaining
         */
        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        /**
         * Adds notification email addresses for provisioning updates.
         *
         * @param emails one or more email addresses
         * @return this builder for method chaining
         */
        public Builder notifications(String... emails) {
            for (String email : emails) {
                this.notificationEmails.add(email);
            }
            return this;
        }

        // ── Pricing ──

        /**
         * Sets the rate card used to price the plan. When omitted, the wizard
         * defaults to live Equinix pricing ({@code EquinixRateCard.of(fabric)}),
         * falling back to a built-in heuristic for any resource the live
         * catalogue cannot price.
         *
         * <p>Supply a {@link api.equinix.javasdk.design.value.ratecard.CustomRateCard}
         * (or a {@link RateCard#layered layered} card) to price against your own
         * negotiated rates instead.</p>
         *
         * @param rateCard the rate card to price the plan with
         * @return this builder for method chaining
         */
        public Builder rateCard(RateCard rateCard) {
            this.rateCard = rateCard;
            return this;
        }

        /**
         * Sets the commitment term used when resolving rates. Defaults to
         * {@link Term#MONTH_12}.
         *
         * @param term the commitment term
         * @return this builder for method chaining
         */
        public Builder term(Term term) {
            this.term = term;
            return this;
        }

        // ── Build ──

        /**
         * Generates the deployment plan from the optimization result using the configured settings.
         * The plan can be reviewed via {@link DeploymentPlan#toMarkdown()}, validated with
         * {@link DeploymentPlan#dryRun()}, or executed with {@link DeploymentPlan#execute()}.
         *
         * @return the complete deployment plan
         */
        public DeploymentPlan plan() {
            return DeploymentWizardEngine.generatePlan(this);
        }

        // Package-private accessors for the engine

        FabricGateway getFabric() { return fabric; }
        OptimizationResult getOptimizationResult() { return optimizationResult; }
        GatewayPackageCode getRouterPackage() { return routerPackage; }
        String getRouterNamePrefix() { return routerNamePrefix; }
        ConnectionType getProviderConnectionType() { return providerConnectionType; }
        ConnectionType getBackboneConnectionType() { return backboneConnectionType; }
        int getBackboneBandwidthMbps() { return backboneBandwidthMbps; }
        BackboneTopology getBackboneTopology() { return backboneTopology; }
        BandwidthStrategy getBandwidthStrategy() { return bandwidthStrategy; }
        Map<String, Integer> getCustomBandwidthMap() { return customBandwidthMap; }
        Long getCustomerAsn() { return customerAsn; }
        boolean isBfdEnabled() { return bfdEnabled; }
        int getBfdInterval() { return bfdInterval; }
        Long getAccountNumber() { return accountNumber; }
        String getProjectId() { return projectId; }
        List<String> getNotificationEmails() { return notificationEmails; }
        RateCard getRateCard() { return rateCard; }
        Term getTerm() { return term; }
    }
}
