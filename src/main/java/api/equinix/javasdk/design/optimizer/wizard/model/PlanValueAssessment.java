package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.design.value.CurrencyReconciler;
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

    /**
     * Declares a cloud provider's monthly egress volume. Repeatable — call once per provider;
     * each declared provider becomes one row of the assessment.
     *
     * @param provider the cloud provider the egress leaves
     * @param amount the monthly egress volume, in {@code unit}s
     * @param unit the unit of {@code amount} (SI decimal: 1 TB = 1000 GB)
     * @return this assessment for method chaining
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    public PlanValueAssessment egress(CloudProviderType provider, double amount, DataUnit unit) {
        if (amount < 0) {
            throw new IllegalArgumentException("egress amount must be non-negative: " + amount);
        }
        egressInputs.add(new EgressInput(provider, unit.toGigabytes(BigDecimal.valueOf(amount))));
        return this;
    }

    /**
     * Declares a cloud provider's monthly egress volume in terabytes. Shorthand for
     * {@link #egress(CloudProviderType, double, DataUnit) egress(provider, terabytes, DataUnit.TERABYTE)}.
     *
     * @param provider the cloud provider the egress leaves
     * @param terabytes the monthly egress volume in terabytes (SI decimal)
     * @return this assessment for method chaining
     * @throws IllegalArgumentException if {@code terabytes} is negative
     */
    public PlanValueAssessment egressTerabytes(CloudProviderType provider, double terabytes) {
        return egress(provider, terabytes, DataUnit.TERABYTE);
    }

    /**
     * Sets the rate card the egress rates are resolved from. When omitted, {@code assess()}
     * defaults to {@code RateCard.standardChain(plan's fabric)} — live Equinix pricing layered
     * over the bundled reference figures.
     *
     * @param rateCard the rate card to resolve egress rates from
     * @return this assessment for method chaining
     */
    public PlanValueAssessment rateCard(RateCard rateCard) {
        this.rateCard = rateCard;
        return this;
    }

    /**
     * Sets the commitment term used when resolving egress rates. Defaults to
     * {@link Term#MONTH_12}.
     *
     * @param term the commitment term
     * @return this assessment for method chaining
     */
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
        String planCurrency = pricing != null && pricing.getCurrency() != null ? pricing.getCurrency() : "USD";

        List<PlanValueRealization.ProviderEgressSaving> perProvider = new ArrayList<>();
        // Per-provider egress savings are only summable when the providers share a currency (different
        // clouds/regions can quote different currencies), and the aggregate is only nettable against
        // the plan's interconnect cost when that too matches. The reconciler tracks both.
        CurrencyReconciler egressRecon = CurrencyReconciler.create();

        for (EgressInput input : egressInputs) {
            Optional<EgressRate> internet = rc.egress(input.provider, null, EgressPath.INTERNET, term);
            Optional<EgressRate> priv = rc.egress(input.provider, null, EgressPath.PRIVATE, term);
            java.util.Currency internetCur = internet.map(EgressRate::getCurrency).orElse(null);
            java.util.Currency privCur = priv.map(EgressRate::getCurrency).orElse(null);
            // internet − private is a subtraction, so this provider is only priced when both rates
            // resolve AND agree on a currency.
            boolean priced = internet.isPresent() && priv.isPresent()
                    && !CurrencyReconciler.knownDifferent(internetCur, privCur);

            BigDecimal internetCost = BigDecimal.ZERO;
            BigDecimal privateCost = BigDecimal.ZERO;
            BigDecimal savings = BigDecimal.ZERO;
            String providerCurrency = null;
            if (priced) {
                internetCost = internet.get().costFor(input.gigabytes);
                privateCost = priv.get().costFor(input.gigabytes);
                savings = internetCost.subtract(privateCost);
                providerCurrency = internetCur != null ? internetCur.getCurrencyCode()
                        : (privCur != null ? privCur.getCurrencyCode() : null);
                egressRecon.add(providerCurrency, savings, BigDecimal.ZERO);
            }

            perProvider.add(PlanValueRealization.ProviderEgressSaving.builder()
                    .provider(input.provider)
                    .monthlyEgressGb(input.gigabytes)
                    .internetMonthlyCost(internetCost)
                    .privateMonthlyCost(privateCost)
                    .monthlySavings(savings)
                    .priced(priced)
                    .currency(providerCurrency)
                    .build());
        }

        boolean egressMixed = egressRecon.isMixed();
        String egressCurrency = egressRecon.soleCurrency();
        // Cross-currency: egress savings versus the plan interconnect cost.
        boolean crossMismatch = !egressMixed && CurrencyReconciler.knownDifferent(egressCurrency, planCurrency);
        boolean reconciled = !egressMixed && !crossMismatch;

        BigDecimal totalSavings;
        BigDecimal net;
        BigDecimal annual;
        BigDecimal firstYear;
        String currency;
        String disclaimer = "Egress savings use indicative/caller-supplied per-GB rates; the plan's interconnect "
                + "cost reflects the plan's pricing (live Fabric pricing where available). Design-time estimate, "
                + "not a quote. Excludes compute, storage, and per-provider free-tier allowances.";

        if (reconciled) {
            totalSavings = egressRecon.monthlyTotal().orElse(BigDecimal.ZERO);
            net = totalSavings.subtract(planMonthly);
            annual = net.multiply(BigDecimal.valueOf(12));
            firstYear = annual.subtract(planSetup);
            // Egress currency when any provider priced, else the plan's currency (both equal here).
            currency = egressCurrency != null ? egressCurrency : planCurrency;
        } else {
            // Do not fabricate a cross-currency total or net. Keep the per-provider figures (each in
            // its own currency) and the plan cost (in the plan's currency), and null the aggregates so
            // they render as "n/a" rather than a wrong number.
            totalSavings = null;
            net = null;
            annual = null;
            firstYear = null;
            currency = planCurrency;
            if (egressMixed) {
                disclaimer += " Provider egress savings are quoted in multiple currencies ("
                        + egressRecon.describeCurrencies() + "): " + egressRecon.describeMonthlySubtotals()
                        + " per month. A single total and net saving cannot be formed across currencies without an "
                        + "FX rate, so they are omitted; the per-provider figures above are each in their own currency,"
                        + " and the plan cost is in " + planCurrency + ".";
            } else {
                disclaimer += " Egress savings are in " + egressCurrency + " but the plan's interconnect cost is in "
                        + planCurrency + "; a net saving cannot be computed across currencies without an FX rate, so"
                        + " the total, net, annual, and first-year figures are omitted (the per-provider egress"
                        + " figures above remain valid in " + egressCurrency + ").";
            }
        }

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
