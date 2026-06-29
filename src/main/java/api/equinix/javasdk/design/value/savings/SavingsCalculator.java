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
 * <p>Equinix interconnect costs default to live Fabric pricing; egress rates come
 * from the supplied {@link RateCard} (a {@code ReferenceRateCard} or a
 * {@link api.equinix.javasdk.design.value.ratecard.CustomRateCard} with your own
 * figures). All outputs are design-time estimates, not quotes.</p>
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

        public Builder egress(double amount, DataUnit unit) {
            if (amount < 0) {
                throw new IllegalArgumentException("egress amount must be non-negative: " + amount);
            }
            this.egressAmount = amount;
            this.egressUnit = unit;
            return this;
        }

        public Builder egressGigabytes(double gigabytes) {
            return egress(gigabytes, DataUnit.GIGABYTE);
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

        /**
         * Sets the rate card. When omitted, Equinix interconnect costs come from
         * live Fabric pricing; supply a card that provides egress rates (a
         * {@code ReferenceRateCard} or {@link api.equinix.javasdk.design.value.ratecard.CustomRateCard})
         * to compute egress savings.
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
