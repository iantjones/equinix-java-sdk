package com.eqixiac.equinix.design.value.tco;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.DataUnit;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fluent calculator that compares the total cost of ownership of a workload across
 * the {@link DeploymentArchetype}s — public cloud over the internet, on-prem,
 * Equinix-interconnected, and (for a cloud-to-cloud comparison) the providers' native
 * multicloud link — and reports which is cheapest over the commitment
 * term (monthly recurring × term months + one-time setup) and by how much.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * TcoComparison tco = fabric.tcoComparison()
 *     .egress(100, DataUnit.TERABYTE)
 *     .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
 *     .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
 *     .term(Term.MONTH_36)
 *     .compare();
 *
 * System.out.println(tco.toMarkdown());
 * }</pre>
 *
 * <p>The Equinix-interconnect archetype uses live Fabric pricing and private-egress rates
 * where available; the public-cloud-internet and on-prem archetypes use indicative reference
 * figures, and the on-prem inputs are overridable.</p>
 *
 * <h3>Cloud-to-cloud comparison</h3>
 * <p><b>Beta.</b> Declaring a peer cloud with {@code toCloud(...)} changes the question from
 * "how should this cloud's egress leave" to "how should these two clouds be joined", and the
 * comparison becomes two-sided so that no archetype is priced on less traffic or fewer
 * components than another:</p>
 *
 * <table>
 *   <caption>What each archetype prices when a peer cloud is set</caption>
 *   <tr><th>Archetype</th><th>Components</th></tr>
 *   <tr><td>{@code PUBLIC_CLOUD_INTERNET}</td><td>internet egress from both clouds</td></tr>
 *   <tr><td>{@code EQUINIX_INTERCONNECT}</td><td>private egress from both clouds, two Fabric
 *       virtual connections, the CSP interconnect-port reference figure for both clouds, plus
 *       the optional Cloud Router and colocation primitives as before</td></tr>
 *   <tr><td>{@code NATIVE_MULTICLOUD_INTERCONNECT}</td><td>one flat fee per provider and the
 *       link's per-GB charge (zero where both providers publish zero)</td></tr>
 *   <tr><td>{@code ON_PREM}</td><td>not priced. Its inputs (carrier transit, hardware, one
 *       cross-connect, power) describe one site and contain no egress from either cloud, so a
 *       total built from them would rank against the other archetypes on less traffic. It leaves
 *       the default archetype set once a peer cloud is set; when requested explicitly it is
 *       reported unpriced with that reason and is never recommended.</td></tr>
 * </table>
 *
 * <pre>{@code
 * TcoComparison tco = fabric.tcoComparison()
 *     .egress(100, DataUnit.TERABYTE)                       // AWS -> Google Cloud, per month
 *     .reverseEgress(40, DataUnit.TERABYTE)                 // Google Cloud -> AWS, per month
 *     .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
 *     .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
 *     .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
 *     .pathTier(1)
 *     .compare();
 * }</pre>
 *
 * <p>Without {@code toCloud(...)} the comparison, its defaults and its output are the
 * single-cloud ones described above.</p>
 */
public final class TcoCalculator {

    private TcoCalculator() {}

    /**
     * Creates a TCO calculator builder over the given Fabric client.
     *
     * @param fabric the Fabric client (or any {@link FabricGateway}) used for live pricing
     * @return a new {@link Builder}
     */
    public static Builder builder(FabricGateway fabric) {
        return new Builder(fabric);
    }

    /**
     * Fluent configuration for a TCO comparison, terminated by {@link #compare()}.
     * Setters validate eagerly (fail-fast {@link IllegalArgumentException}s), so a
     * mistake surfaces at the call site rather than as a wrong number later.
     *
     * <p>Defaults: 1000&nbsp;Mbps bandwidth, {@code EVPL_VC} connection type,
     * {@link Term#MONTH_12}, egress volume 0&nbsp;GB, the three single-cloud archetypes
     * (once a peer cloud is set: {@code PUBLIC_CLOUD_INTERNET}, {@code EQUINIX_INTERCONNECT} and
     * {@code NATIVE_MULTICLOUD_INTERCONNECT}, without {@code ON_PREM}), no Cloud
     * Router, 1 cabinet, 1 cross-connect, 5.0&nbsp;kW power draw, on-prem inputs from
     * the bundled reference midpoints, and the standard layered rate-card chain
     * (live Equinix pricing, then bundled reference figures). Cloud-to-cloud defaults:
     * no peer cloud, path tier 1, AWS free tier not applied, reverse volume equal to the
     * forward volume.</p>
     *
     * <p>Constraints that span two levers are checked at {@link #compare()}, because levers
     * may be set in any order: the native archetype, {@code toRegion}, {@code pathTier},
     * {@code reverseEgress} and {@code useAwsFreeTier(true)} each require {@code toCloud};
     * {@code toCloud} requires {@code fromCloud} and must differ from it;
     * {@code useAwsFreeTier(true)} requires AWS as one of the two clouds. Each violation is
     * an {@link IllegalArgumentException} naming the lever.</p>
     *
     * <p>Currency contract for the {@code onPrem*} overrides: caller-supplied values are
     * taken to be in the <em>comparison</em> currency (the currency the egress rates
     * resolved to). The bundled midpoints are published in the reference data's own
     * currency (USD), so when the comparison currency is known to differ, an on-prem
     * archetype still relying on <em>any</em> midpoint is reported unpriced (with the
     * reason) rather than relabelled — supply all four overrides in the comparison
     * currency to price it.</p>
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
        // null = the default set, resolved in getArchetypes() once the peer cloud is known.
        private Set<DeploymentArchetype> archetypes;

        // Cloud-to-cloud levers (null/false = not set; the comparison is then single-cloud).
        private CloudProviderType peerProvider;
        private String peerRegion;
        private Integer pathTier;
        private boolean useAwsFreeTier;
        private Double reverseEgressAmount;
        private DataUnit reverseEgressUnit;

        // Colocation quantities for the Equinix-interconnect archetype (per-unit quotes scale
        // by these, the way POWER_PER_KW scales by powerKw).
        private int cabinets = 1;
        private int crossConnects = 1;

        // On-prem assumptions (null -> use bundled reference midpoints).
        private double powerKw = 5.0;
        private BigDecimal onPremTransitPerMbpsMonth;
        private BigDecimal onPremHardwareMonthly;
        private BigDecimal onPremCrossConnectMonthly;
        private BigDecimal onPremPowerPerKwMonth;

        // Arbitrary user-supplied monthly costs (e.g. compute, storage, software) folded into
        // every priced archetype's breakdown so the absolute totals reflect the full deployment.
        private final Map<String, BigDecimal> additionalLineItems = new LinkedHashMap<>();

        Builder(FabricGateway fabric) {
            this.fabric = fabric;
        }

        /**
         * Sets the monthly cloud data-egress volume the comparison is built around
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

        /** Shorthand for {@code egress(terabytes, DataUnit.TERABYTE)} (1 TB = 1000 GB). */
        public Builder egressTerabytes(double terabytes) {
            return egress(terabytes, DataUnit.TERABYTE);
        }

        /**
         * Sets the cloud provider the egress leaves — the provider whose internet and
         * private egress rates are looked up.
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
         * Declares the peer cloud and makes the comparison cloud-to-cloud (see the
         * {@link TcoCalculator} class documentation): the internet, Equinix and native archetypes
         * are priced two-sided, {@code NATIVE_MULTICLOUD_INTERCONNECT} joins the default archetype
         * set and {@code ON_PREM} leaves it (it carries no cloud egress and is reported unpriced
         * when requested explicitly). <b>Beta.</b>
         *
         * @param provider the cloud at the other end; must differ from {@code fromCloud(...)}
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the provider is null, or equals a
         *                                  {@code fromCloud(...)} already set (the same check
         *                                  runs again at {@link #compare()})
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
         * region it prices a Google Cloud side at the North America location and says so in the
         * figure's provenance.
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
         * 3 continental, 4 long-haul, 5 maximum scope. AWS assigns the tier from the Region paths
         * the interconnect serves and publishes no path-to-tier table, so it is an input, never
         * derived. Providers without a tier concept ignore it.
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
         * Opts in to the AWS free tier for the native link: one free tier-1 500&nbsp;Mbps
         * interconnect per AWS Region per generally-available provider. Off by default and never
         * inferred, because the SDK cannot know whether the account already uses its free
         * interconnect for the Region and provider. When set and the request is eligible
         * (bandwidth at or below 500&nbsp;Mbps, tier 1, eligible peer) the AWS side is priced at
         * zero; when set but ineligible, the listed rate applies and the provenance says why.
         *
         * @param useAwsFreeTier {@code true} to apply the free tier where eligible
         * @return this builder for method chaining
         */
        public Builder useAwsFreeTier(boolean useAwsFreeTier) {
            this.useAwsFreeTier = useAwsFreeTier;
            return this;
        }

        /**
         * Sets the monthly volume flowing from the peer cloud back to {@code fromCloud} — the
         * traffic the peer cloud bills as egress. When not set, a cloud-to-cloud comparison
         * assumes it equals the forward volume set with {@link #egress(double, DataUnit)} and
         * states that assumption in {@code TcoComparison.getTrafficNote()}.
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
         * Sets the Equinix metro the interconnect lands in, used for metro-sensitive
         * rate lookups. Omit the call entirely for a metro-agnostic comparison.
         *
         * @param metro the Equinix metro
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the metro is null (omit the call instead)
         */
        public Builder viaMetro(MetroCode metro) {
            if (metro == null) {
                throw new IllegalArgumentException(
                        "metro must not be null (omit viaMetro(...) for a metro-agnostic comparison)");
            }
            this.metro = metro;
            return this;
        }

        /**
         * Sets the interconnect bandwidth in Mbps (default 1000). Drives the Fabric
         * connection price, the CSP interconnect-port fee, and the on-prem carrier-transit
         * line.
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
         * Sets the Fabric connection type priced for the Equinix archetype (default
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
         * Adds a Fabric Cloud Router of the given package to the Equinix archetype
         * (none is included by default). If the rate card cannot price the package, the
         * archetype is reported as <em>not fully priced</em> with a note naming the
         * missing router — its partial figures stay visible but it is excluded from the
         * recommendation rather than silently under-priced.
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
         * Sets the commitment term (default {@link Term#MONTH_12}). It feeds the
         * term-aware rate lookups and defines the horizon archetypes are ranked over
         * ({@code MRC × months + setup}).
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
         * Sets the rate card that prices every component of the comparison. When omitted, the
         * standard layered chain ({@code RateCard.standardChain(fabric)}) applies: live Equinix
         * Fabric pricing first, then the bundled {@code ReferenceRateCard}, which also supplies
         * indicative cloud-egress rates. A supplied card <em>replaces</em> that chain entirely;
         * layer it yourself (e.g. {@code RateCard.layered(custom, RateCard.standardChain(fabric))})
         * to keep the defaults as a fallback.
         *
         * @param rateCard the rate card to price with, or {@code null} to use the standard chain
         * @return this builder for method chaining
         */
        public Builder rateCard(RateCard rateCard) {
            this.rateCard = rateCard;
            return this;
        }

        /**
         * Restricts the comparison to the given archetypes. The default is
         * {@code PUBLIC_CLOUD_INTERNET}, {@code ON_PREM} and {@code EQUINIX_INTERCONNECT}; when a
         * peer cloud is set with {@code toCloud(...)} it is {@code PUBLIC_CLOUD_INTERNET},
         * {@code EQUINIX_INTERCONNECT} and {@code NATIVE_MULTICLOUD_INTERCONNECT}. Requesting the
         * native archetype without a peer cloud fails at {@link #compare()}. Requesting
         * {@code ON_PREM} with a peer cloud is accepted; the breakdown is reported unpriced with
         * the reason and is never recommended.
         *
         * @param archetypes the archetypes to compare
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the array is null, empty, or contains null
         */
        public Builder archetypes(DeploymentArchetype... archetypes) {
            if (archetypes == null || archetypes.length == 0) {
                throw new IllegalArgumentException("at least one archetype must be requested");
            }
            Set<DeploymentArchetype> requested = EnumSet.noneOf(DeploymentArchetype.class);
            for (DeploymentArchetype a : archetypes) {
                if (a == null) {
                    throw new IllegalArgumentException("archetypes must not contain null");
                }
                requested.add(a);
            }
            this.archetypes = requested;
            return this;
        }

        /**
         * Sets the number of colocation cabinets for the Equinix-interconnect archetype
         * (default 1). The per-cabinet {@code ColocationItem.CABINET} quote — when the rate card
         * prices one — is multiplied by this count.
         */
        public Builder cabinets(int cabinets) {
            if (cabinets < 0) {
                throw new IllegalArgumentException("cabinets must be non-negative: " + cabinets);
            }
            this.cabinets = cabinets;
            return this;
        }

        /**
         * Sets the number of Equinix cross-connects for the Equinix-interconnect archetype
         * (default 1). The per-unit {@code ColocationItem.CROSS_CONNECT} quote (or the reference
         * cross-connect figure when no colocation rate is supplied) is multiplied by this count;
         * zero omits cross-connects entirely.
         */
        public Builder crossConnects(int crossConnects) {
            if (crossConnects < 0) {
                throw new IllegalArgumentException("crossConnects must be non-negative: " + crossConnects);
            }
            this.crossConnects = crossConnects;
            return this;
        }

        /**
         * Sets the power draw in kW (default 5.0). Note the <em>dual use</em>: it scales
         * both the on-prem power/space line (per-kW midpoint × kW) <strong>and</strong> the
         * Equinix archetype's colocation power line when the rate card prices
         * {@code ColocationItem.POWER_PER_KW} — tuning it for one side changes the other.
         *
         * @param powerKw the power draw in kW
         * @return this builder for method chaining
         * @throws IllegalArgumentException if negative or not finite
         */
        public Builder powerKw(double powerKw) {
            if (powerKw < 0 || !Double.isFinite(powerKw)) {
                throw new IllegalArgumentException("powerKw must be a non-negative finite number: " + powerKw);
            }
            this.powerKw = powerKw;
            return this;
        }

        /** Overrides the on-prem carrier-transit rate per Mbps per month, in the comparison currency. */
        public Builder onPremTransitPerMbpsMonth(BigDecimal value) {
            this.onPremTransitPerMbpsMonth = requireNonNegative(value, "onPremTransitPerMbpsMonth");
            return this;
        }

        /** Overrides the amortized on-prem hardware monthly cost, in the comparison currency. */
        public Builder onPremHardwareMonthly(BigDecimal value) {
            this.onPremHardwareMonthly = requireNonNegative(value, "onPremHardwareMonthly");
            return this;
        }

        /** Overrides the on-prem cross-connect monthly cost, in the comparison currency. */
        public Builder onPremCrossConnectMonthly(BigDecimal value) {
            this.onPremCrossConnectMonthly = requireNonNegative(value, "onPremCrossConnectMonthly");
            return this;
        }

        /** Overrides the on-prem power/space rate per kW per month, in the comparison currency. */
        public Builder onPremPowerPerKwMonth(BigDecimal value) {
            this.onPremPowerPerKwMonth = requireNonNegative(value, "onPremPowerPerKwMonth");
            return this;
        }

        private static BigDecimal requireNonNegative(BigDecimal value, String name) {
            if (value != null && value.signum() < 0) {
                throw new IllegalArgumentException(name + " must be non-negative: " + value);
            }
            return value;
        }

        /**
         * Adds an arbitrary named monthly cost — e.g. compute, storage, or software the rate cards
         * do not price — folded into <em>every</em> priced archetype's breakdown and total. Because
         * it is applied uniformly, it makes the absolute TCO figures realistic without changing
         * which archetype is cheapest (the savings comparison is unaffected). Call multiple times
         * for multiple line items.
         *
         * @param label   the line-item label shown in each breakdown
         * @param monthly the monthly amount (in the comparison currency)
         * @return this builder for method chaining
         */
        public Builder additionalLineItem(String label, BigDecimal monthly) {
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("additional line-item label must not be null or blank");
            }
            if (monthly == null) {
                throw new IllegalArgumentException("additional line-item monthly amount must not be null");
            }
            additionalLineItems.put(label, monthly);
            return this;
        }

        /**
         * Runs the comparison.
         *
         * @return the TCO comparison across the requested archetypes
         * @throws IllegalArgumentException if levers conflict: the native archetype,
         *                                  {@code toRegion}, {@code pathTier},
         *                                  {@code reverseEgress} or {@code useAwsFreeTier(true)}
         *                                  without {@code toCloud}; {@code toCloud} without, or
         *                                  equal to, {@code fromCloud}; or
         *                                  {@code useAwsFreeTier(true)} when neither cloud is AWS
         */
        public TcoComparison compare() {
            validateCloudToCloudLevers();
            return TcoEngine.compute(this);
        }

        private void validateCloudToCloudLevers() {
            if (peerProvider == null) {
                if (archetypes != null && archetypes.contains(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT)) {
                    throw new IllegalArgumentException(
                            "NATIVE_MULTICLOUD_INTERCONNECT requires a peer cloud: call toCloud(...)");
                }
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
                throw new IllegalArgumentException(lever + " applies to a cloud-to-cloud comparison only: "
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
        Set<DeploymentArchetype> getArchetypes() {
            if (archetypes != null) {
                return archetypes;
            }
            if (peerProvider != null) {
                // ON_PREM carries no cloud egress, so in a two-sided comparison it would be ranked
                // on less traffic than every other archetype; it is not a default here.
                return EnumSet.of(DeploymentArchetype.PUBLIC_CLOUD_INTERNET,
                        DeploymentArchetype.EQUINIX_INTERCONNECT,
                        DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);
            }
            return EnumSet.of(DeploymentArchetype.PUBLIC_CLOUD_INTERNET,
                    DeploymentArchetype.ON_PREM, DeploymentArchetype.EQUINIX_INTERCONNECT);
        }
        CloudProviderType getPeerProvider() { return peerProvider; }
        String getPeerRegion() { return peerRegion; }
        int getPathTier() { return pathTier == null ? MulticloudLinkQuote.DEFAULT_PATH_TIER : pathTier; }
        boolean isUseAwsFreeTier() { return useAwsFreeTier; }
        boolean isReverseEgressSet() { return reverseEgressAmount != null; }
        double getReverseEgressAmount() { return reverseEgressAmount == null ? 0 : reverseEgressAmount; }
        DataUnit getReverseEgressUnit() { return reverseEgressUnit; }
        int getCabinets() { return cabinets; }
        int getCrossConnects() { return crossConnects; }
        double getPowerKw() { return powerKw; }
        BigDecimal getOnPremTransitPerMbpsMonth() { return onPremTransitPerMbpsMonth; }
        BigDecimal getOnPremHardwareMonthly() { return onPremHardwareMonthly; }
        BigDecimal getOnPremCrossConnectMonthly() { return onPremCrossConnectMonthly; }
        BigDecimal getOnPremPowerPerKwMonth() { return onPremPowerPerKwMonth; }
        Map<String, BigDecimal> getAdditionalLineItems() { return additionalLineItems; }
    }
}
