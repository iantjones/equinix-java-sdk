package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The value-realization view of a {@link DeploymentPlan}: the monthly cloud-egress
 * saving the plan's private interconnects produce (versus public-internet egress),
 * netted against the plan's actual Equinix interconnect cost.
 *
 * <p>Produced by {@link DeploymentPlan#valueRealization()} after the caller
 * declares their per-provider egress volumes. The plan's monthly cost is the
 * interconnect spend from {@link PlanPricing} (live-priced where available), and a plan that
 * depends on native multicloud links (<b>Beta</b>) also carries their fees in
 * {@code nativeLinkMonthlyCost}; the net figure subtracts both.</p>
 */
@Value
@Builder
public class PlanValueRealization {

    /** The plan's monthly interconnect cost, from {@link PlanPricing} (ZERO when unpriced). */
    BigDecimal planMonthlyCost;

    /** The plan's one-time setup cost, from {@link PlanPricing} (ZERO when unpriced). */
    BigDecimal planSetupCost;

    /**
     * <b>Beta.</b> The monthly fees of the native multicloud links the plan depends on (role
     * {@code REPLACEMENT}), from {@code PlanPricing.getNativeReplacementMonthlyCost()}; billed by
     * the cloud providers and subtracted from the net like {@code planMonthlyCost}. {@code null}
     * when the plan has no such link or their fee is unpriced ({@code nativeLinkUnpriced}). A
     * {@code null} is not a zero cost.
     */
    BigDecimal nativeLinkMonthlyCost;

    /** The ISO 4217 code of {@code nativeLinkMonthlyCost}; {@code null} when that figure is. */
    String nativeLinkCurrency;

    /**
     * <b>Beta.</b> {@code true} when the plan depends on a native multicloud link whose fee is
     * unpriced or spans currencies; the aggregates are then {@code null} because the
     * deployment's cost is not known.
     */
    boolean nativeLinkUnpriced;

    /**
     * The summed monthly egress saving across providers — {@code null} when the per-provider
     * figures span currencies (or mismatch the plan's currency), so a cross-currency total is
     * never fabricated; the per-provider rows remain valid each in its own currency.
     */
    BigDecimal totalMonthlyEgressSavings;

    /**
     * Egress saving net of the plan's monthly cost and of {@code nativeLinkMonthlyCost} —
     * {@code null} whenever the total is, and whenever {@code nativeLinkUnpriced}.
     */
    BigDecimal netMonthlySavings;

    /** Twelve times the net monthly saving — {@code null} whenever the total is. */
    BigDecimal annualNetSavings;

    /** The annual net saving minus the plan's setup cost — {@code null} whenever the total is. */
    BigDecimal firstYearNetSavings;

    /** One row per declared provider, in declaration order — priced or not. */
    List<ProviderEgressSaving> perProvider;

    /** The currency the aggregates are quoted in (the plan's currency when they are omitted). */
    String currency;

    /** Provenance and honesty notes — including why any aggregate figures were omitted. */
    String disclaimer;

    /**
     * The egress-saving row for one declared cloud provider: the internet-egress cost the
     * declared volume would incur, the private-interconnect cost the plan enables instead, and
     * the difference. {@code priced} is {@code false} — with the money fields left at ZERO —
     * when the internet or private rate could not be resolved, or the two rates disagreed on
     * currency (a subtraction across currencies is never fabricated). Rows are per-provider
     * currency-labelled precisely because different clouds/regions can quote different
     * currencies.
     */
    @Value
    @Builder
    public static class ProviderEgressSaving {

        /** The cloud provider this row describes. */
        CloudProviderType provider;

        /**
         * The path {@code privateMonthlyCost} is priced on: {@link EgressPath#PRIVATE} for a cloud
         * the plan reaches through a Fabric connection, {@link EgressPath#MULTICLOUD_INTERCONNECT}
         * (<b>Beta</b>) for a cloud it reaches only through a native multicloud link it depends on.
         * {@code null} on a row built before this field existed.
         */
        EgressPath path;

        /** The declared monthly egress volume in decimal gigabytes. */
        BigDecimal monthlyEgressGb;

        /** What the declared volume costs over public-internet egress. ZERO when not priced. */
        BigDecimal internetMonthlyCost;

        /** What the declared volume costs over {@code path}. ZERO when not priced. */
        BigDecimal privateMonthlyCost;

        /** {@code internetMonthlyCost - privateMonthlyCost}. ZERO when not priced. */
        BigDecimal monthlySavings;

        /**
         * Whether both egress rates resolved in a single currency; {@code false} means the money
         * fields on this row are placeholders (ZERO), not real figures.
         */
        boolean priced;

        /**
         * The currency of this provider's egress figures, which need not match other providers' —
         * different clouds/regions can be priced in different currencies. {@code null} for an
         * unpriced provider. Rendered per-row so a multi-currency breakdown is never mislabelled.
         */
        String currency;
    }

    private static String money(BigDecimal v, String currency) {
        if (v == null) {
            return "n/a";
        }
        return currency + " " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Renders a Markdown summary of the plan's value realization.
     *
     * @return a Markdown report
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Deployment Value Realization\n\n");
        sb.append("| Provider | Egress (GB/mo) | Internet | Private | Saving |\n|---|---|---|---|---|\n");
        for (ProviderEgressSaving p : perProvider) {
            // Each provider's figures are labelled in their own currency (falling back to the summary
            // currency when unset), so a mixed-currency breakdown never mislabels a row.
            String rowCurrency = p.getCurrency() != null ? p.getCurrency() : currency;
            sb.append("| ").append(p.getProvider() == null ? "—" : p.getProvider().name())
                    .append(p.getPath() == EgressPath.MULTICLOUD_INTERCONNECT ? " (native link)" : "")
                    .append(" | ").append(p.getMonthlyEgressGb().setScale(0, RoundingMode.HALF_UP).toPlainString())
                    .append(" | ").append(p.isPriced() ? money(p.getInternetMonthlyCost(), rowCurrency) : "n/a")
                    .append(" | ").append(p.isPriced() ? money(p.getPrivateMonthlyCost(), rowCurrency) : "n/a")
                    .append(" | ").append(p.isPriced() ? money(p.getMonthlySavings(), rowCurrency) : "n/a")
                    .append(" |\n");
        }
        sb.append("\n");
        sb.append("- **Total egress saving:** ").append(money(totalMonthlyEgressSavings, currency)).append("/mo\n");
        sb.append("- Plan interconnect cost: −").append(money(planMonthlyCost, currency)).append("/mo\n");
        if (nativeLinkMonthlyCost != null) {
            sb.append("- Native multicloud link fees the plan depends on (billed by the cloud providers): −")
                    .append(money(nativeLinkMonthlyCost, nativeLinkCurrency != null ? nativeLinkCurrency : currency))
                    .append("/mo\n");
        } else if (nativeLinkUnpriced) {
            sb.append("- Native multicloud link fees the plan depends on: n/a (unpriced; see the note)\n");
        }
        sb.append("- **Net monthly saving:** ").append(money(netMonthlySavings, currency)).append("\n");
        sb.append("- Annual net saving: ").append(money(annualNetSavings, currency)).append("\n");
        sb.append("- First-year net saving (incl. setup): ").append(money(firstYearNetSavings, currency)).append("\n");
        if (disclaimer != null) {
            sb.append("\n_").append(disclaimer).append("_\n");
        }
        return sb.toString();
    }
}
