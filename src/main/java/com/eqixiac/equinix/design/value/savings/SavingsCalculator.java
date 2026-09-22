package com.eqixiac.equinix.design.value.savings;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;

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
 * {@link com.eqixiac.equinix.design.value.ratecard.CustomRateCard} with your own
 * figures) to replace that chain. All outputs are design-time estimates, not
 * quotes.</p>
 *
 * <h3>Cloud-to-cloud comparison</h3>
 * <p><b>Beta.</b> Declaring a peer cloud with {@code toCloud(...)} adds a
 * {@link MulticloudPathComparison} to the estimate: the monthly cost of the traffic between the
 * two clouds over the public internet, over Equinix Fabric (two-sided: private egress both ways,
 * two virtual connections, both CSP ports) and over the providers' native multicloud link, plus
 * the sustained rate at which the flat-fee native link and the per-GB Equinix path cost the
 * same. The single-cloud figures on the estimate are computed exactly as without a peer cloud
 * and describe the {@code fromCloud} direction only.</p>
 *
 * <pre>{@code
 * SavingsEstimate s = fabric.savingsCalculator()
 *     .egress(100, DataUnit.TERABYTE)
 *     .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
 *     .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
 *     .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
 *     .calculate();
 *
 * s.breakEvenSustainedMbps().ifPresent(mbps -> System.out.println(mbps + " Mbps"));
 * }</pre>
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
     * layered rate-card chain (live Equinix pricing, then bundled reference figures).
     * Cloud-to-cloud defaults: no peer cloud, path tier 1, AWS free tier not applied, reverse
     * volume equal to the forward volume.</p>
     *
     * <p>Constraints that span two levers are checked at {@link #calculate()}, because levers
     * may be set in any order: {@code toRegion}, {@code pathTier}, {@code reverseEgress} and
     * {@code useAwsFreeTier(true)} each require {@code toCloud}; {@code toCloud} requires
     * {@code fromCloud} and must differ from it; {@code useAwsFreeTier(true)} requires AWS as
     * one of the two clouds. Each violation is an {@link IllegalArgumentException} naming the
     * lever.</p>
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

        // Cloud-to-cloud levers (null/false = not set; the estimate is then single-cloud).
        private CloudProviderType peerProvider;
        private String peerRegion;
        private Integer pathTier;
        private boolean useAwsFreeTier;
        private Double reverseEgressAmount;
        private DataUnit reverseEgressUnit;

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
         * @throws IllegalArgumentException if the provider is null, or equals a
         *                                  {@code toCloud(...)} already set
         */
        public Builder fromCloud(CloudProviderType provider) {
            if (provider == null) {
                throw new IllegalArgumentException("cloud provider must not be null");
            }
            if (provider == this.peerProvider) {
                throw new IllegalArgumentException(
                        "fromCloud must differ from toCloud: both are " + provider);
            }
            this.provider = provider;
            return this;
        }

        /**
         * Declares the peer cloud. The estimate then carries a {@link MulticloudPathComparison}
         * (internet, Equinix and native paths, both directions) and a break-even sustained
         * rate. <b>Beta.</b>
         *
         * @param provider the cloud at the other end; must differ from {@code fromCloud(...)}
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the provider is null, or equals a
         *                                  {@code fromCloud(...)} already set (the same check
         *                                  runs again at {@link #calculate()})
         */
        public Builder toCloud(CloudProviderType provider) {
            if (provider == null) {
                throw new IllegalArgumentException("peer cloud provider must not be null");
            }
            if (provider == this.provider) {
                throw new IllegalArgumentException(
                        "toCloud must differ from fromCloud: both are " + provider);
            }
            this.peerProvider = provider;
            return this;
        }

        /**
         * Sets the peer cloud's region in its own notation (e.g. {@code "us-east4"}). Optional.
         * The bundled reference data uses it to select Google's transport location; with no
         * region it prices a Google Cloud side at the North America location and says so.
         *
         * @param region the peer region identifier (may be null)
         * @return this builder for method chaining
         */
        public Builder toRegion(String region) {
            this.peerRegion = region;
            return this;
        }

        /**
         * Sets the AWS connectivity-scope tier of the native link: 1 local (default), 2 regional,
         * 3 continental, 4 long-haul, 5 maximum scope. An input, never derived: AWS publishes no
         * path-to-tier table. Providers without a tier concept ignore it.
         *
         * @param pathTier the tier, 1-5
         * @return this builder for method chaining
         * @throws IllegalArgumentException if outside 1-5
         */
        public Builder pathTier(int pathTier) {
            if (pathTier < MulticloudLinkQuote.MIN_PATH_TIER || pathTier > MulticloudLinkQuote.MAX_PATH_TIER) {
                throw new IllegalArgumentException("pathTier must be between "
                        + MulticloudLinkQuote.MIN_PATH_TIER + " and " + MulticloudLinkQuote.MAX_PATH_TIER
                        + ": " + pathTier);
            }
            this.pathTier = pathTier;
            return this;
        }

        /**
         * Opts in to the AWS free tier for the native link (one free tier-1 500&nbsp;Mbps
         * interconnect per AWS Region per generally-available provider). Off by default and
         * never inferred. When set but the request is ineligible, the listed rate applies and
         * the comparison's notes say why.
         *
         * @param useAwsFreeTier {@code true} to apply the free tier where eligible
         * @return this builder for method chaining
         */
        public Builder useAwsFreeTier(boolean useAwsFreeTier) {
            this.useAwsFreeTier = useAwsFreeTier;
            return this;
        }

        /**
         * Sets the monthly volume flowing from the peer cloud back to {@code fromCloud}. When
         * not set, the cloud-to-cloud comparison assumes it equals the forward volume and says
         * so in its notes.
         *
         * @param amount the volume in {@code unit}s per month
         * @param unit   the unit the amount is expressed in (decimal/SI conversions)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the amount is negative or not finite, or the unit is null
         */
        public Builder reverseEgress(double amount, DataUnit unit) {
            if (amount < 0 || !Double.isFinite(amount)) {
                throw new IllegalArgumentException(
                        "reverse egress amount must be a non-negative finite number: " + amount);
            }
            if (unit == null) {
                throw new IllegalArgumentException("reverse egress unit must not be null");
            }
            this.reverseEgressAmount = amount;
            this.reverseEgressUnit = unit;
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
         * @throws IllegalArgumentException if levers conflict: {@code toRegion},
         *                                  {@code pathTier}, {@code reverseEgress} or
         *                                  {@code useAwsFreeTier(true)} without {@code toCloud};
         *                                  {@code toCloud} without, or equal to,
         *                                  {@code fromCloud}; or {@code useAwsFreeTier(true)}
         *                                  when neither cloud is AWS
         */
        public SavingsEstimate calculate() {
            validateCloudToCloudLevers();
            return SavingsCalculatorEngine.compute(this);
        }

        private void validateCloudToCloudLevers() {
            if (peerProvider == null) {
                requireUnsetWithoutPeer(peerRegion != null, "toRegion");
                requireUnsetWithoutPeer(pathTier != null, "pathTier");
                requireUnsetWithoutPeer(reverseEgressAmount != null, "reverseEgress");
                requireUnsetWithoutPeer(useAwsFreeTier, "useAwsFreeTier(true)");
                return;
            }
            if (provider == null) {
                throw new IllegalArgumentException("toCloud(...) requires fromCloud(...)");
            }
            if (peerProvider == provider) {
                throw new IllegalArgumentException("toCloud must differ from fromCloud: both are " + provider);
            }
            if (useAwsFreeTier && provider != CloudProviderType.AWS && peerProvider != CloudProviderType.AWS) {
                throw new IllegalArgumentException("useAwsFreeTier(true) requires AWS as one of the two clouds: "
                        + provider + " <-> " + peerProvider);
            }
        }

        private static void requireUnsetWithoutPeer(boolean set, String lever) {
            if (set) {
                throw new IllegalArgumentException(lever + " applies to a cloud-to-cloud estimate only: "
                        + "call toCloud(...) or remove it");
            }
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
        CloudProviderType getPeerProvider() { return peerProvider; }
        String getPeerRegion() { return peerRegion; }
        int getPathTier() { return pathTier == null ? MulticloudLinkQuote.DEFAULT_PATH_TIER : pathTier; }
        boolean isUseAwsFreeTier() { return useAwsFreeTier; }
        boolean isReverseEgressSet() { return reverseEgressAmount != null; }
        double getReverseEgressAmount() { return reverseEgressAmount == null ? 0 : reverseEgressAmount; }
        DataUnit getReverseEgressUnit() { return reverseEgressUnit; }
    }
}
