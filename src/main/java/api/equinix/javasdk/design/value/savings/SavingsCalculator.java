package api.equinix.javasdk.design.value.savings;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;

/**
 * Fluent calculator that estimates how much routing cloud egress over an Equinix
 * private interconnect saves versus the public internet.
 *
 * <p>It compares the modelled monthly cost of egressing a given data volume over
 * the public internet against the same volume over a dedicated interconnect (AWS
 * Direct Connect / Azure ExpressRoute / Google Cloud Interconnect reached via
 * Equinix Fabric), nets off the Equinix interconnect cost, and reports the saving
 * plus break-even points.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * SavingsEstimate s = fabric.savingsCalculator()
 *     .egress(50, DataUnit.TERABYTE)
 *     .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
 *     .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
 *     .term(Term.MONTH_12)
 *     .calculate();
 *
 * System.out.println(s.toMarkdown());
 * }</pre>
 *
 * <p>When no {@link RateCard} is supplied, the standard layered chain applies —
 * live Equinix Fabric pricing first, then the bundled
 * {@code ReferenceRateCard}, which also supplies indicative cloud-egress rates —
 * so the calculator works out of the box. Supply a card (e.g. a
 * {@link api.equinix.javasdk.design.value.ratecard.CustomRateCard} with your own
 * figures) to replace that chain. All outputs are design-time estimates, not
 * quotes.</p>
 */
public final class SavingsCalculator {

    private SavingsCalculator() {}

    /**
     * Creates a savings calculator builder over the given Fabric client.
     *
     * @param fabric the Fabric client (or any {@link FabricGateway}) used for live pricing
     * @return a new {@link Builder}
     */
    public static Builder builder(FabricGateway fabric) {
        return new Builder(fabric);
    }

    /**
     * Fluent configuration for a savings estimate, terminated by {@link #calculate()}.
     * Setters validate eagerly (fail-fast {@link IllegalArgumentException}s), so a
     * mistake surfaces at the call site rather than as a wrong number later.
     *
     * <p>Defaults: 1000&nbsp;Mbps bandwidth, {@code EVPL_VC} connection type,
     * {@link Term#MONTH_12}, egress volume 0&nbsp;GB, no Cloud Router, and the standard
     * layered rate-card chain (live Equinix pricing, then bundled reference figures).</p>
     */
    public static final class Builder {

        private final FabricGateway fabric;

        private double egressAmount;
        private DataUnit egressUnit = DataUnit.GIGABYTE;
        private CloudProviderType provider;
        private String region;
        private MetroCode metro;
        private int bandwidthMbps = 1_000;
        private ConnectionType connectionType = ConnectionType.EVPL_VC;
        private boolean includeRouter;
        private String routerPackage = "STANDARD";
        private Term term = Term.MONTH_12;
        private RateCard rateCard;

        Builder(FabricGateway fabric) {
            this.fabric = fabric;
        }

        /**
         * Sets the monthly cloud data-egress volume the estimate is built around
         * (default 0).
         *
         * @param amount the volume in {@code unit}s per month
         * @param unit   the unit the amount is expressed in (decimal/SI conversions)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the amount is negative or not finite, or the unit is null
         */
        public Builder egress(double amount, DataUnit unit) {
            if (amount < 0 || !Double.isFinite(amount)) {
                throw new IllegalArgumentException("egress amount must be a non-negative finite number: " + amount);
            }
            if (unit == null) {
                throw new IllegalArgumentException("egress unit must not be null");
            }
            this.egressAmount = amount;
            this.egressUnit = unit;
            return this;
        }

        /** Shorthand for {@code egress(gigabytes, DataUnit.GIGABYTE)}. */
        public Builder egressGigabytes(double gigabytes) {
            return egress(gigabytes, DataUnit.GIGABYTE);
        }

        /** Shorthand for {@code egress(terabytes, DataUnit.TERABYTE)} (1 TB = 1000 GB). */
        public Builder egressTerabytes(double terabytes) {
            return egress(terabytes, DataUnit.TERABYTE);
        }

        /**
         * Sets the cloud provider the egress leaves — the provider whose internet and
         * private egress rates are compared.
         *
         * @param provider the cloud provider
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the provider is null
         */
        public Builder fromCloud(CloudProviderType provider) {
            if (provider == null) {
                throw new IllegalArgumentException("cloud provider must not be null");
            }
            this.provider = provider;
            return this;
        }

        /**
         * Sets the provider region the egress originates in, in the provider's own
         * region notation (e.g. {@code "us-east-1"}, {@code "westeurope"}). Optional:
         * region-agnostic rate cards ignore it, but the provider-API adapters need it.
         *
         * @param region the provider region identifier (may be null for provider-wide rates)
         * @return this builder for method chaining
         */
        public Builder inRegion(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the Equinix metro the interconnect lands in, used for metro-sensitive
         * rate lookups. Omit the call entirely for a metro-agnostic estimate.
         *
         * @param metro the Equinix metro
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the metro is null (omit the call instead)
         */
        public Builder viaMetro(MetroCode metro) {
            if (metro == null) {
                throw new IllegalArgumentException(
                        "metro must not be null (omit viaMetro(...) for a metro-agnostic estimate)");
            }
            this.metro = metro;
            return this;
        }

        /**
         * Sets the interconnect bandwidth in Mbps (default 1000), which drives the Fabric
         * connection price the egress saving is netted against.
         *
         * @param bandwidthMbps the bandwidth in Mbps
         * @return this builder for method chaining
         * @throws IllegalArgumentException if not positive
         */
        public Builder bandwidthMbps(int bandwidthMbps) {
            if (bandwidthMbps <= 0) {
                throw new IllegalArgumentException("bandwidthMbps must be positive: " + bandwidthMbps);
            }
            this.bandwidthMbps = bandwidthMbps;
            return this;
        }

        /**
         * Sets the Fabric connection type priced for the interconnect (default
         * {@code EVPL_VC}).
         *
         * @param connectionType the connection type
         * @return this builder for method chaining
         * @throws IllegalArgumentException if null
         */
        public Builder connectionType(ConnectionType connectionType) {
            if (connectionType == null) {
                throw new IllegalArgumentException("connectionType must not be null");
            }
            this.connectionType = connectionType;
            return this;
        }

        /**
         * Adds a Fabric Cloud Router of the given package to the interconnect cost
         * (none is included by default). If the rate card cannot price the package — or
         * prices it in a different currency from the connection — the interconnect
         * figures stay <em>partial</em> (connection only), {@code isEquinixPriced()}
         * flips false, and the estimate's disclaimer names the excluded router; the
         * router is never silently dropped from an estimate reported as complete.
         *
         * @param packageCode the router package code (e.g. {@code "STANDARD"})
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the package code is null or blank
         */
        public Builder includeCloudRouter(String packageCode) {
            if (packageCode == null || packageCode.isBlank()) {
                throw new IllegalArgumentException("Cloud Router package code must not be null or blank");
            }
            this.includeRouter = true;
            this.routerPackage = packageCode;
            return this;
        }

        /**
         * Sets the commitment term used for the term-aware rate lookups (default
         * {@link Term#MONTH_12}).
         *
         * @param term the commitment term
         * @return this builder for method chaining
         * @throws IllegalArgumentException if null
         */
        public Builder term(Term term) {
            if (term == null) {
                throw new IllegalArgumentException("term must not be null");
            }
            this.term = term;
            return this;
        }

        /**
         * Sets the rate card that prices both egress and Equinix interconnect resources. When
         * omitted, the standard layered chain ({@code RateCard.standardChain(fabric)}) applies:
         * live Equinix Fabric pricing first, then the bundled {@code ReferenceRateCard}, which
         * also supplies indicative cloud-egress rates — so egress savings are computed out of the
         * box with no card supplied. A supplied card <em>replaces</em> that chain entirely (it is
         * not layered over the defaults); to keep the defaults as a fallback behind your own
         * figures, pass {@code RateCard.layered(customCard, RateCard.standardChain(fabric))}.
         *
         * @param rateCard the rate card to price with, or {@code null} to use the standard chain
         * @return this builder for method chaining
         */
        public Builder rateCard(RateCard rateCard) {
            this.rateCard = rateCard;
            return this;
        }

        /**
         * Runs the calculation.
         *
         * @return the savings estimate
         */
        public SavingsEstimate calculate() {
            return SavingsCalculatorEngine.compute(this);
        }

        // Package-private accessors for the engine.

        FabricGateway getFabric() { return fabric; }
        double getEgressAmount() { return egressAmount; }
        DataUnit getEgressUnit() { return egressUnit; }
        CloudProviderType getProvider() { return provider; }
        String getRegion() { return region; }
        MetroCode getMetro() { return metro; }
        int getBandwidthMbps() { return bandwidthMbps; }
        ConnectionType getConnectionType() { return connectionType; }
        boolean isIncludeRouter() { return includeRouter; }
        String getRouterPackage() { return routerPackage; }
        Term getTerm() { return term; }
        RateCard getRateCard() { return rateCard; }
    }
}
