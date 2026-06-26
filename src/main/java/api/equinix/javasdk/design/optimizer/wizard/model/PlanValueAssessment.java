package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.EgressRate;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.design.value.savings.DataUnit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fluent builder, returned by {@link DeploymentPlan#valueRealization()}, that
 * computes the egress savings a deployment plan unlocks once the caller declares
 * their per-provider monthly egress volumes.
 *
 * <pre>{@code
 * PlanValueRealization vr = plan.valueRealization()
 *     .egress(CloudProviderType.AWS, 50, DataUnit.TERABYTE)
 *     .egress(CloudProviderType.AZURE, 20, DataUnit.TERABYTE)
 *     .assess();
 *
 * System.out.println(vr.toMarkdown());
 * }</pre>
 */
public final class PlanValueAssessment {

    private final DeploymentPlan plan;
    private final List<EgressInput> egressInputs = new ArrayList<>();
    private RateCard rateCard;
    private Term term = Term.MONTH_12;

    PlanValueAssessment(DeploymentPlan plan) {
        this.plan = plan;
    }

    /** Declares a monthly egress volume for a cloud provider reached by this plan. */
    public PlanValueAssessment egress(CloudProviderType provider, double amount, DataUnit unit) {
        if (amount < 0) {
            throw new IllegalArgumentException("egress amount must be non-negative: " + amount);
        }
        egressInputs.add(new EgressInput(provider, unit.toGigabytes(BigDecimal.valueOf(amount))));
        return this;
    }

    /** Declares a monthly egress volume in terabytes for a cloud provider. */
    public PlanValueAssessment egressTerabytes(CloudProviderType provider, double terabytes) {
        return egress(provider, terabytes, DataUnit.TERABYTE);
    }

    /** Overrides the rate card used for egress rates. Defaults to live Equinix + bundled reference. */
    public PlanValueAssessment rateCard(RateCard rateCard) {
        this.rateCard = rateCard;
        return this;
    }

    /** Sets the commitment term used for rate lookups. Defaults to {@link Term#MONTH_12}. */
    public PlanValueAssessment term(Term term) {
        this.term = term;
        return this;
    }

    /**
     * Computes the plan's value realization.
     *
     * @return the egress savings netted against the plan's interconnect cost
     */
    public PlanValueRealization assess() {
        RateCard rc = rateCard != null ? rateCard : RateCard.standardChain(plan.getFabric());

        PlanPricing pricing = plan.getPricing();
        BigDecimal planMonthly = pricing != null && pricing.getMonthlyTotal() != null
                ? pricing.getMonthlyTotal() : BigDecimal.ZERO;
        BigDecimal planSetup = pricing != null && pricing.getSetupTotal() != null
                ? pricing.getSetupTotal() : BigDecimal.ZERO;
        String currency = pricing != null && pricing.getCurrency() != null ? pricing.getCurrency() : "USD";

        List<PlanValueRealization.ProviderEgressSaving> perProvider = new ArrayList<>();
        BigDecimal totalSavings = BigDecimal.ZERO;

        for (EgressInput input : egressInputs) {
            Optional<EgressRate> internet = rc.egress(input.provider, null, EgressPath.INTERNET, term);
            Optional<EgressRate> priv = rc.egress(input.provider, null, EgressPath.PRIVATE, term);
            boolean priced = internet.isPresent() && priv.isPresent();

            BigDecimal internetCost = BigDecimal.ZERO;
            BigDecimal privateCost = BigDecimal.ZERO;
            BigDecimal savings = BigDecimal.ZERO;
            if (priced) {
                internetCost = internet.get().costFor(input.gigabytes);
                privateCost = priv.get().costFor(input.gigabytes);
                savings = internetCost.subtract(privateCost);
                totalSavings = totalSavings.add(savings);
                if (internet.get().getCurrency() != null) {
                    currency = internet.get().getCurrency().getCurrencyCode();
                }
            }

            perProvider.add(PlanValueRealization.ProviderEgressSaving.builder()
                    .provider(input.provider)
                    .monthlyEgressGb(input.gigabytes)
                    .internetMonthlyCost(internetCost)
                    .privateMonthlyCost(privateCost)
                    .monthlySavings(savings)
                    .priced(priced)
                    .build());
        }

        BigDecimal net = totalSavings.subtract(planMonthly);
        BigDecimal annual = net.multiply(BigDecimal.valueOf(12));
        BigDecimal firstYear = annual.subtract(planSetup);

        String disclaimer = "Egress savings use indicative/caller-supplied per-GB rates; the plan's interconnect "
                + "cost reflects the plan's pricing (live Fabric pricing where available). Design-time estimate, "
                + "not a quote. Excludes compute, storage, and per-provider free-tier allowances.";

        return PlanValueRealization.builder()
                .planMonthlyCost(planMonthly)
                .planSetupCost(planSetup)
                .totalMonthlyEgressSavings(totalSavings)
                .netMonthlySavings(net)
                .annualNetSavings(annual)
                .firstYearNetSavings(firstYear)
                .perProvider(perProvider)
                .currency(currency)
                .disclaimer(disclaimer)
                .build();
    }

    private static final class EgressInput {
        final CloudProviderType provider;
        final BigDecimal gigabytes;

        EgressInput(CloudProviderType provider, BigDecimal gigabytes) {
            this.provider = provider;
            this.gigabytes = gigabytes;
        }
    }
}
