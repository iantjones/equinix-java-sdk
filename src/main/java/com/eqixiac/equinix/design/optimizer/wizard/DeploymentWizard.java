package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BackboneTopology;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BandwidthStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlanPricing;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;

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
 *     .providerConnectionType(ConnectionType.IP_VC)
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
 *
 * // Plans are immutable: dryRun() returns a NEW plan with refreshed validation state,
 * // so reassign the result — calling it without using the return value validates nothing
 * // the caller can see.
 * plan = plan.dryRun();
 *
 * // A brand-new customer gathers the per-connection authorization the plan enumerated
 * // (plan.getRequiredInputs()) and supplies it at execution time; each provider connection is
 * // then dry-run against its now-real Cloud Router before it is created for real:
 * DeploymentOutcome outcome = plan.execute(ExecutionInputs.builder()
 *     .authenticationKey("FCR-DC-to-aws", "123456789012")
 *     .vlanTag("FCR-DC-to-aws", 1001)
 *     .build());
 * }</pre>
 *
 * <p>Nothing is provisioned until {@code execute()} is called: {@code plan()} and
 * {@code dryRun()} create no resources, so a plan can be reviewed, exported, and re-planned
 * freely. Plan generation itself is delegated to a package-private engine.</p>
 *
 * @see DeploymentPlan
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

        // Connection settings. Every wizard-planned connection originates on a Fabric Cloud Router
        // A-side, and Fabric accepts an FCR-originated virtual connection only as IP_VC — the old
        // EVPL_VC default flowed a port-only type into every create/dry-run body and the exported
        // HCL (FCR A-side => IP_VC).
        private ConnectionType providerConnectionType = ConnectionType.IP_VC;
        private ConnectionType backboneConnectionType = ConnectionType.IP_VC;

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

        // Peering-subnet addressing
        private String subnetBase = "10.100.0.0";

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
         * Sets the naming prefix for Cloud Routers and the connection / routing-protocol names derived
         * from them. Each router is named {@code "{prefix}-{metroCode}"} (e.g. {@code "FCR-DC"}) and a
         * provider connection {@code "{prefix}-{metroCode}-to-{token}"} (e.g. {@code "FCR-DC-to-aws"}).
         * Defaults to {@code "FCR"}.
         *
         * <p>The prefix is the stem of every generated name, and Fabric rejects any name of 24 or more
         * characters. The prefix is therefore trimmed, reduced to name-safe characters, and bounded to
         * a short budget at plan time: a prefix longer than that budget is truncated (the composed
         * names always stay within the limit), and a blank prefix is rejected with an
         * {@link IllegalArgumentException} when {@link #plan()} runs, because there is no stem to build
         * on.</p>
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
         * Sets the Fabric connection type for provider connections. Defaults to {@code IP_VC} — the
         * only type Fabric accepts for a connection whose A-side is a Cloud Router, which every
         * wizard-planned provider connection has. Setting a type incompatible with an FCR A-side
         * (e.g. the port-based {@code EVPL_VC}) is flagged by the plan's Layer-1 validation
         * (FCR A-side =&gt; IP_VC) rather than 400ing live.
         *
         * @param type the connection type for provider connections
         * @return this builder for method chaining
         */
        public Builder providerConnectionType(ConnectionType type) {
            this.providerConnectionType = type;
            return this;
        }

        /**
         * Sets the Fabric connection type for backbone links. Defaults to {@code IP_VC} — a backbone
         * link is Cloud Router to Cloud Router, and Fabric accepts an FCR-originated virtual
         * connection only as {@code IP_VC}. An incompatible type is flagged by the plan's Layer-1
         * validation (FCR A-side =&gt; IP_VC).
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
         * Sets a custom bandwidth map for {@link BandwidthStrategy#CUSTOM}. Values are bandwidth in
         * Mbps; keys are matched per (metro, provider) most-specific-first as either
         * {@code "<metroId>-<providerLabel>"} (a per-metro-per-provider override, e.g.
         * {@code "DC-Amazon Web Services"}) or {@code "<providerLabel>"} (that provider in every
         * metro, e.g. {@code "Amazon Web Services"}). A (metro, provider) connection with no matching
         * key is sized by the normal per-workload aggregation instead of a fabricated default.
         *
         * @param bandwidthMap the custom bandwidth mapping
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the map is {@code null} or any of its values is
         *         {@code null} — a null bandwidth cannot size a connection, so it fails fast here
         *         naming the offending key instead of surfacing as an NPE mid-plan
         */
        public Builder customBandwidthMap(Map<String, Integer> bandwidthMap) {
            if (bandwidthMap == null) {
                throw new IllegalArgumentException("customBandwidthMap must not be null — omit the "
                        + "call (or use bandwidthStrategy) when no custom sizing is wanted.");
            }
            for (Map.Entry<String, Integer> entry : bandwidthMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw new IllegalArgumentException("customBandwidthMap value for key '"
                            + entry.getKey() + "' is null — every entry must carry a bandwidth in Mbps.");
                }
            }
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
         * Sets the base network the plan's /30 peering subnets are allocated from. Defaults to
         * {@code "10.100.0.0"}: the first connection peers on {@code 10.100.0.1/30} /
         * {@code 10.100.0.2/30}, and each further connection advances the third octet.
         *
         * <p><strong>Cross-plan reuse caveat:</strong> allocation is scoped to a single plan, so
         * every plan restarts from this base. Plans executed into the <em>same</em> project would
         * therefore reuse the same peering addresses — give each such plan a distinct base (e.g.
         * {@code .subnetBase("10.200.0.0")}) to keep their /30s disjoint.</p>
         *
         * @param subnetBase an IPv4 dotted-quad base network, e.g. {@code "10.200.0.0"} (the fourth
         *                   octet is ignored; allocation advances the third)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the value is not a valid IPv4 dotted-quad
         */
        public Builder subnetBase(String subnetBase) {
            this.subnetBase = validateSubnetBase(subnetBase);
            return this;
        }

        private static String validateSubnetBase(String subnetBase) {
            if (subnetBase != null) {
                String[] octets = subnetBase.trim().split("\\.");
                if (octets.length == 4) {
                    boolean valid = true;
                    for (String octet : octets) {
                        try {
                            int value = Integer.parseInt(octet);
                            if (value < 0 || value > 255) {
                                valid = false;
                            }
                        } catch (NumberFormatException e) {
                            valid = false;
                        }
                    }
                    if (valid) {
                        return subnetBase.trim();
                    }
                }
            }
            throw new IllegalArgumentException("subnetBase must be an IPv4 dotted-quad like "
                    + "\"10.200.0.0\", got '" + subnetBase + "'.");
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
         * Adds notification email addresses for provisioning updates. <strong>Every</strong> address
         * is applied to every planned Cloud Router and connection (Fabric mandates at least one
         * recipient per Cloud Router, error {@code EQ-3040013}) — all of them are sent on the wire
         * bodies and rendered into exported HCL, never just the first.
         *
         * <p>Repeated calls accumulate — addresses from every call are kept, none replaced.
         * A plan built with no recipient at all fails Layer-1 validation.</p>
         *
         * @param emails one or more email addresses
         * @return this builder for method chaining
         * @throws IllegalArgumentException if an address is {@code null} or blank
         * @throws NullPointerException if the array itself is {@code null}
         */
        public Builder notifications(String... emails) {
            for (String email : emails) {
                if (email == null || email.isBlank()) {
                    throw new IllegalArgumentException("Notification email addresses must be non-blank.");
                }
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
         * <p>Supply a {@link com.eqixiac.equinix.design.value.ratecard.CustomRateCard}
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

        /**
         * Reprices a plan whose connections changed (e.g. an MCP profile choice altered a billable
         * tier) and returns the result as a <em>new</em> {@link DeploymentPlan}: the argument is
         * never mutated (plans are immutable values), so callers must use the returned copy — whose
         * monthly and setup totals match the plan's <em>current</em> connections rather than the
         * tiers they were originally priced at.
         *
         * <p>The plan's current Cloud Routers, provider connections and backbone links are re-priced
         * against <em>this</em> builder's rate card and commitment term — the same configuration the plan
         * was built with — through the wizard's single pricing authority
         * ({@code DeploymentWizardEngine.estimatePricing}), so per-metro currency reconciliation is
         * preserved exactly as at plan time. Only the plan's {@code pricing} is replaced; every other
         * field is carried through unchanged. Repricing a plan whose connections did not change yields
         * the same figures (a no-op-equivalent).</p>
         *
         * @param plan the plan whose pricing is stale relative to its current connections; left
         *             untouched by this call
         * @return a new copy of the plan with pricing recomputed from its current connections
         */
        public DeploymentPlan reprice(DeploymentPlan plan) {
            PlanPricing pricing = DeploymentWizardEngine.estimatePricing(
                    this, plan.getCloudRouters(), plan.getProviderConnections(), plan.getBackboneLinks());
            return plan.toBuilder().pricing(pricing).build();
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
        String getSubnetBase() { return subnetBase; }
        RateCard getRateCard() { return rateCard; }
        Term getTerm() { return term; }
    }
}
