package api.equinix.javasdk.design.value.savings;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;

/**
 * Fluent calculator for the headline value-realization question: <em>how much does
 * routing cloud egress over an Equinix private interconnect save versus the public
 * internet?</em>
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

    /** Fluent builder for a savings calculation. */
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

        /** Sets the monthly egress volume in the given unit. */
        public Builder egress(double amount, DataUnit unit) {
            if (amount < 0) {
                throw new IllegalArgumentException("egress amount must be non-negative: " + amount);
            }
            this.egressAmount = amount;
            this.egressUnit = unit;
            return this;
        }

        /** Sets the monthly egress volume in gigabytes. */
        public Builder egressGigabytes(double gigabytes) {
            return egress(gigabytes, DataUnit.GIGABYTE);
        }

        /** Sets the monthly egress volume in terabytes. */
        public Builder egressTerabytes(double terabytes) {
            return egress(terabytes, DataUnit.TERABYTE);
        }

        /** Sets the cloud provider the data egresses from. */
        public Builder fromCloud(CloudProviderType provider) {
            this.provider = provider;
            return this;
        }

        /** Sets the cloud provider region (e.g. {@code "us-east-1"}). */
        public Builder inRegion(String region) {
            this.region = region;
            return this;
        }

        /** Sets the Equinix metro the interconnect lands in. */
        public Builder viaMetro(MetroCode metro) {
            this.metro = metro;
            return this;
        }

        /** Sets the Fabric connection bandwidth in Mbps. Defaults to 1000. */
        public Builder bandwidthMbps(int bandwidthMbps) {
            this.bandwidthMbps = bandwidthMbps;
            return this;
        }

        /** Sets the Fabric connection type. Defaults to {@code EVPL_VC}. */
        public Builder connectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;
            return this;
        }

        /** Includes a Fabric Cloud Router of the given package in the Equinix cost. */
        public Builder includeCloudRouter(String packageCode) {
            this.includeRouter = true;
            this.routerPackage = packageCode;
            return this;
        }

        /** Sets the commitment term used for rate lookups. Defaults to {@link Term#MONTH_12}. */
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
