package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.DataUnit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fluent builder, returned by {@link DeploymentPlan#valueRealization()}, that
 * computes the egress savings a deployment plan produces once the caller declares
 * their per-provider monthly egress volumes, net of the plan's cost.
 *
 * <p><b>Beta.</b> A plan that depends on native multicloud links (role {@code REPLACEMENT})
 * pays their fees as well as the Equinix cost, so {@code assess()} subtracts
 * {@code PlanPricing.getNativeReplacementMonthlyCost()} from the net; when that fee is unpriced
 * the net is withheld. Egress from a cloud the plan reaches only through such a link is priced
 * at the link's per-GB rate ({@code EgressPath.MULTICLOUD_INTERCONNECT}), not at the Fabric
 * private-interconnect rate.</p>
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

        // Native multicloud links the plan depends on (role REPLACEMENT, Beta). The plan omits
        // Equinix connections in their favour, so their fee is part of what the deployment pays
        // and is netted like the plan's own cost. A REPLACEMENT link with no priced fee makes the
        // net unknowable: the aggregates are then withheld, never computed as if the fee were zero.
        List<PlannedMulticloudInterconnect> replacements = new ArrayList<>();
        Set<CloudProviderType> nativeOnlyClouds = EnumSet.noneOf(CloudProviderType.class);
        for (PlannedMulticloudInterconnect link : plan.multicloudLinksOrEmpty()) {
            if (link.replacesEquinixConnections()) {
                replacements.add(link);
                nativeOnlyClouds.add(link.getProviderA());
                nativeOnlyClouds.add(link.getProviderZ());
            }
        }
        // A cloud the plan still reaches through a Fabric connection egresses over PRIVATE; a
        // cloud reached only through a REPLACEMENT link egresses over the native link.
        for (PlannedConnection conn : plan.getProviderConnections() == null
                ? List.<PlannedConnection>of() : plan.getProviderConnections()) {
            if (conn.getZSideCloudType() != null) {
                nativeOnlyClouds.remove(conn.getZSideCloudType());
            }
        }
        BigDecimal nativeMonthly = null;
        String nativeCurrency = null;
        boolean nativeUnpriced = false;
        if (!replacements.isEmpty()) {
            if (pricing != null && pricing.getNativeReplacementMonthlyCost() != null) {
                nativeMonthly = pricing.getNativeReplacementMonthlyCost();
                nativeCurrency = pricing.getNativeReplacementCurrency();
            } else {
                nativeUnpriced = true;
            }
        }

        List<PlanValueRealization.ProviderEgressSaving> perProvider = new ArrayList<>();
        // Per-provider egress savings are only summable when the providers share a currency (different
        // clouds/regions can quote different currencies), and the aggregate is only nettable against
        // the plan's interconnect cost when that too matches. The reconciler tracks both.
        CurrencyReconciler egressRecon = CurrencyReconciler.create();

        for (EgressInput input : egressInputs) {
            EgressPath path = nativeOnlyClouds.contains(input.provider)
                    ? EgressPath.MULTICLOUD_INTERCONNECT : EgressPath.PRIVATE;
            Optional<EgressRate> internet = rc.egress(input.provider, null, EgressPath.INTERNET, term);
            Optional<EgressRate> priv = rc.egress(input.provider, null, path, term);
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
                    .path(path)
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
        // Cross-currency: the native link fees versus the plan cost and the egress savings.
        boolean nativeMismatch = nativeMonthly != null
                && (CurrencyReconciler.knownDifferent(nativeCurrency, planCurrency)
                    || (!egressMixed && CurrencyReconciler.knownDifferent(nativeCurrency, egressCurrency)));
        boolean reconciled = !egressMixed && !crossMismatch && !nativeUnpriced && !nativeMismatch;

        BigDecimal totalSavings;
        BigDecimal net;
        BigDecimal annual;
        BigDecimal firstYear;
        String currency;
        String disclaimer = "Egress savings use indicative/caller-supplied per-GB rates; the plan's interconnect "
                + "cost reflects the plan's pricing (live Fabric pricing where available). Design-time estimate, "
                + "not a quote. Excludes compute, storage, and per-provider free-tier allowances.";
        if (!replacements.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (PlannedMulticloudInterconnect link : replacements) {
                names.add(link.getName());
            }
            disclaimer += " The plan depends on native multicloud link(s) " + names + " (Beta) in place of Equinix "
                    + "connections; their monthly fees, billed by the cloud providers, are netted against the "
                    + "egress saving like the plan's own cost, and egress from a cloud the plan reaches only "
                    + "through such a link is priced at that link's per-GB rate (EgressPath.MULTICLOUD_INTERCONNECT), "
                    + "not at the Fabric private-interconnect rate.";
        }

        if (reconciled) {
            totalSavings = egressRecon.monthlyTotal().orElse(BigDecimal.ZERO);
            net = totalSavings.subtract(planMonthly);
            if (nativeMonthly != null) {
                net = net.subtract(nativeMonthly);
            }
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
            if (nativeUnpriced) {
                List<String> unpriced = pricing != null && pricing.getUnpricedMulticloudLinks() != null
                        ? pricing.getUnpricedMulticloudLinks() : List.of();
                disclaimer += " The fee of a native multicloud link the plan depends on is unpriced"
                        + (unpriced.isEmpty() ? "" : " (" + String.join(", ", unpriced) + ")")
                        + " or spans currencies, so the deployment's cost is not known and the total, net, annual "
                        + "and first-year figures are omitted rather than computed as if that fee were zero. "
                        + "Supply the missing rate through CustomRateCard.Builder.multicloudLinkHourlyRate(...) "
                        + "and reprice the plan.";
            } else if (nativeMismatch) {
                disclaimer += " The native multicloud link fees are in " + nativeCurrency + " but the plan's "
                        + "interconnect cost is in " + planCurrency + " and the egress savings in " + egressCurrency
                        + "; a net saving cannot be computed across currencies without an FX rate, so the total, "
                        + "net, annual, and first-year figures are omitted.";
            } else if (egressMixed) {
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
                .nativeLinkMonthlyCost(nativeMonthly)
                .nativeLinkCurrency(nativeCurrency)
                .nativeLinkUnpriced(nativeUnpriced)
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
