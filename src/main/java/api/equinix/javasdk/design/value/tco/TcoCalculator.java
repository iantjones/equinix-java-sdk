package api.equinix.javasdk.design.value.tco;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.design.value.savings.DataUnit;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fluent calculator that compares the total cost of ownership of a workload across
 * the three {@link DeploymentArchetype}s — public cloud over the internet, on-prem,
 * and Equinix-interconnected — and reports which is cheapest and by how much.
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
 * <p>The Equinix-interconnect archetype is priced rigorously (live Fabric pricing +
 * verified private-egress rates); the public-cloud-internet and on-prem archetypes
 * lean on indicative reference figures and the on-prem inputs are overridable.</p>
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
        private Set<DeploymentArchetype> archetypes = EnumSet.allOf(DeploymentArchetype.class);

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

        public Builder egress(double amount, DataUnit unit) {
            if (amount < 0) {
                throw new IllegalArgumentException("egress amount must be non-negative: " + amount);
            }
            this.egressAmount = amount;
            this.egressUnit = unit;
            return this;
        }

        public Builder egressTerabytes(double terabytes) {
            return egress(terabytes, DataUnit.TERABYTE);
        }

        public Builder fromCloud(CloudProviderType provider) {
            this.provider = provider;
            return this;
        }

        public Builder inRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder viaMetro(MetroCode metro) {
            this.metro = metro;
            return this;
        }

        public Builder bandwidthMbps(int bandwidthMbps) {
            this.bandwidthMbps = bandwidthMbps;
            return this;
        }

        public Builder connectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;
            return this;
        }

        public Builder includeCloudRouter(String packageCode) {
            this.includeRouter = true;
            this.routerPackage = packageCode;
            return this;
        }

        public Builder term(Term term) {
            this.term = term;
            return this;
        }

        public Builder rateCard(RateCard rateCard) {
            this.rateCard = rateCard;
            return this;
        }

        public Builder archetypes(DeploymentArchetype... archetypes) {
            this.archetypes = EnumSet.noneOf(DeploymentArchetype.class);
            for (DeploymentArchetype a : archetypes) {
                this.archetypes.add(a);
            }
            return this;
        }

        public Builder powerKw(double powerKw) {
            this.powerKw = powerKw;
            return this;
        }

        public Builder onPremTransitPerMbpsMonth(BigDecimal value) {
            this.onPremTransitPerMbpsMonth = value;
            return this;
        }

        public Builder onPremHardwareMonthly(BigDecimal value) {
            this.onPremHardwareMonthly = value;
            return this;
        }

        public Builder onPremCrossConnectMonthly(BigDecimal value) {
            this.onPremCrossConnectMonthly = value;
            return this;
        }

        public Builder onPremPowerPerKwMonth(BigDecimal value) {
            this.onPremPowerPerKwMonth = value;
            return this;
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
            if (label != null && monthly != null) {
                additionalLineItems.put(label, monthly);
            }
            return this;
        }

        /**
         * Runs the comparison.
         *
         * @return the TCO comparison across the requested archetypes
         */
        public TcoComparison compare() {
            return TcoEngine.compute(this);
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
        Set<DeploymentArchetype> getArchetypes() { return archetypes; }
        double getPowerKw() { return powerKw; }
        BigDecimal getOnPremTransitPerMbpsMonth() { return onPremTransitPerMbpsMonth; }
        BigDecimal getOnPremHardwareMonthly() { return onPremHardwareMonthly; }
        BigDecimal getOnPremCrossConnectMonthly() { return onPremCrossConnectMonthly; }
        BigDecimal getOnPremPowerPerKwMonth() { return onPremPowerPerKwMonth; }
        Map<String, BigDecimal> getAdditionalLineItems() { return additionalLineItems; }
    }
}
