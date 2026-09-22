package com.eqixiac.equinix.design.value.savings;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * The result of a {@link SavingsCalculator} run: the modelled monthly cost of
 * egressing data over the public internet versus over an Equinix-provided private
 * interconnect, the Equinix interconnect cost, and the resulting net savings and
 * break-even points.
 *
 * <p>All amounts are in {@code getCurrency()}. The {@code *Priced} flags indicate
 * which inputs the rate card could actually resolve; when a flag is {@code false}
 * the corresponding figures are either zero or explicitly partial — the
 * {@code getDisclaimer()} text names the exact component that could not be priced
 * (or was excluded for being in another currency) — and the estimate should be
 * read as incomplete (see {@code isComplete()}).</p>
 *
 * <p>When the calculator was given a peer cloud, {@code getMulticloudComparison()} carries the
 * cloud-to-cloud section (three paths, both directions, and the break-even sustained rate). The
 * fields above it are unaffected by the peer cloud: they describe the {@code getProvider()}
 * direction only and are computed exactly as for a single-cloud estimate, and
 * {@code isComplete()} does not consider the cloud-to-cloud section.</p>
 */
@Value
@Builder
public class SavingsEstimate {

    /** The requested monthly egress volume, normalized to decimal gigabytes. */
    BigDecimal monthlyEgressGb;

    /** The cloud provider the egress leaves, or {@code null} when none was declared. */
    CloudProviderType provider;

    /** The provider region the egress originates in, or {@code null} when region-agnostic. */
    String region;

    /** The Equinix metro the interconnect lands in, or {@code null} when metro-agnostic. */
    MetroCode metro;

    /** Per-GB internet rate; zero when {@code isEgressPriced()} is false (not a real free rate). */
    BigDecimal internetRatePerGb;

    /** Per-GB private-interconnect rate; zero when {@code isEgressPriced()} is false. */
    BigDecimal privateRatePerGb;

    /** Monthly cost of the volume over the public internet; zero when egress is unpriced. */
    BigDecimal internetEgressMonthlyCost;

    /** Monthly cost of the same volume over the private interconnect; zero when egress is unpriced. */
    BigDecimal privateEgressMonthlyCost;

    /** Internet minus private egress cost; zero when egress is unpriced. */
    BigDecimal monthlyEgressSavings;

    /**
     * Monthly Equinix interconnect cost (connection, plus Cloud Router when included and
     * summable). Zero — with the real figure quoted in the disclaimer — when the
     * interconnect resolved in a different currency from the egress figures and was
     * therefore excluded rather than mislabelled.
     */
    BigDecimal equinixMonthlyCost;

    /** One-time Equinix setup cost; excluded (zero) under the same cross-currency rule. */
    BigDecimal equinixSetupCost;

    /**
     * Egress saving net of the Equinix interconnect cost. {@code null} when the two sides
     * are in different currencies (the subtraction is never fabricated) — guard before
     * dereferencing.
     */
    BigDecimal netMonthlySavings;

    /** {@code netMonthlySavings × 12}; {@code null} under the same conditions. */
    BigDecimal annualNetSavings;

    /** Annual net saving minus the one-time setup; {@code null} under the same conditions. */
    BigDecimal firstYearNetSavings;

    /**
     * The monthly egress volume (GB) at which the interconnect pays for itself.
     * {@code null} when egress is unpriced, the per-GB delta is not positive, or the
     * currencies differ.
     */
    BigDecimal breakEvenGbPerMonth;

    /**
     * Months for the net saving to recoup the one-time setup. {@code null} when there is
     * no positive net saving or no setup charge.
     */
    BigDecimal paybackMonths;

    /** The single currency every non-excluded amount is expressed in (ISO 4217 code). */
    String currency;

    /** Whether both egress rates resolved in one currency (the egress figures are real). */
    boolean egressPriced;

    /** Whether the Equinix interconnect cost is fully priced (not partial or excluded). */
    boolean equinixPriced;

    /** {@code true} only when every component priced and reconciled to one currency. */
    boolean complete;

    /** Estimate caveats — names any component that could not be priced or was excluded. */
    String disclaimer;

    /**
     * The cloud-to-cloud section: internet, Equinix and native-multicloud path totals for the
     * traffic in both directions, and the break-even sustained rate. {@code null} unless a peer
     * cloud was declared with {@code SavingsCalculator.Builder.toCloud(...)}. <b>Beta.</b>
     */
    MulticloudPathComparison multicloudComparison;

    /**
     * The sustained rate (Mbps, both directions summed, traffic split evenly) at which the
     * flat-fee native multicloud link and the per-GB Equinix path cost the same per month.
     * Formula, units and the conditions under which it is empty are documented on
     * {@code MulticloudPathComparison.breakEvenSustainedMbps(...)}.
     *
     * @return the break-even rate; empty when no peer cloud was declared, any input is unpriced,
     *         the currencies differ, or no break-even exists
     */
    public Optional<BigDecimal> breakEvenSustainedMbps() {
        return multicloudComparison == null ? Optional.empty() : multicloudComparison.breakEvenSustainedMbps();
    }

    private static String money(BigDecimal v, String currency) {
        if (v == null) {
            return "n/a";
        }
        return currency + " " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String moneyOrUnpriced(BigDecimal v, String currency) {
        return v == null ? "_unpriced_" : money(v, currency);
    }

    /**
     * Appends the cloud-to-cloud section: one row per path (fixed, data transfer, total), the
     * break-even sustained rate, the lowest-cost path, and the notes carrying each figure's
     * provenance and the 730 h/month conversion. Adds nothing for a single-cloud estimate.
     */
    private void appendMulticloudComparison(StringBuilder sb) {
        MulticloudPathComparison c = multicloudComparison;
        if (c == null) {
            return;
        }
        String cur = c.getCurrency() != null ? c.getCurrency() : currency;
        sb.append("\n### Cloud-to-cloud paths: ").append(c.getProviderA());
        if (c.getRegionA() != null) {
            sb.append(" (").append(c.getRegionA()).append(")");
        }
        sb.append(" <-> ").append(c.getProviderZ());
        if (c.getRegionZ() != null) {
            sb.append(" (").append(c.getRegionZ()).append(")");
        }
        sb.append("\n\n");
        sb.append("Traffic per month: ")
                .append(c.getForwardEgressGb().setScale(0, RoundingMode.HALF_UP).toPlainString()).append(" GB ")
                .append(c.getProviderA()).append(" -> ").append(c.getProviderZ()).append(", ")
                .append(c.getReverseEgressGb().setScale(0, RoundingMode.HALF_UP).toPlainString()).append(" GB ")
                .append(c.getProviderZ()).append(" -> ").append(c.getProviderA())
                .append(c.isReverseEgressAssumedSymmetric()
                        ? " (reverse volume not supplied; assumed equal to the forward volume)" : "")
                .append(". Link size ").append(c.getBandwidthMbps()).append(" Mbps, path tier ")
                .append(c.getPathTier()).append(". The table above covers the ").append(c.getProviderA())
                .append(" direction only.\n\n");

        sb.append("| Path | Fixed monthly | Data transfer monthly | Total monthly |\n|---|---|---|---|\n");
        sb.append("| Public internet | ").append(money(BigDecimal.ZERO, cur)).append(" | ")
                .append(moneyOrUnpriced(c.getInternetMonthlyCost(), cur)).append(" | ")
                .append(moneyOrUnpriced(c.getInternetMonthlyCost(), cur)).append(" |\n");
        sb.append("| Equinix Fabric private interconnect (two-sided) | ")
                .append(moneyOrUnpriced(c.getEquinixFixedMonthlyCost(), cur)).append(" | ")
                .append(moneyOrUnpriced(c.getEquinixEgressMonthlyCost(), cur)).append(" | ")
                .append(moneyOrUnpriced(c.getEquinixMonthlyCost(), cur)).append(" |\n");
        sb.append("| Native multicloud interconnect | ")
                .append(moneyOrUnpriced(c.getNativeFixedMonthlyCost(), cur)).append(" | ")
                .append(moneyOrUnpriced(c.getNativeDataTransferMonthlyCost(), cur)).append(" | ")
                .append(moneyOrUnpriced(c.getNativeMonthlyCost(), cur)).append(" |\n\n");

        if (c.getBreakEvenSustainedMbps() != null) {
            BigDecimal eachWay = c.getBreakEvenSustainedMbps().divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
            sb.append("- Break-even sustained rate (native vs Equinix): ")
                    .append(c.getBreakEvenSustainedMbps().toPlainString()).append(" Mbps summed over both "
                            + "directions (").append(eachWay.toPlainString()).append(" Mbps each way). ")
                    .append(c.isNativeCheaperAboveBreakEven()
                            ? "Below it the Equinix path costs less per month; above it the native link does.\n"
                            : "Below it the native link costs less per month; above it the Equinix path does.\n");
        } else {
            sb.append("- Break-even sustained rate (native vs Equinix): n/a (see notes)\n");
        }
        if (c.getLowestCostPath() != null) {
            sb.append("- Lowest monthly cost at the declared volumes: ").append(c.getLowestCostPath()).append("\n");
        }
        if (c.getNotes() != null) {
            for (String note : c.getNotes()) {
                sb.append("- ").append(note).append("\n");
            }
        }
    }

    /**
     * Renders a human-readable Markdown summary of the savings analysis, suitable
     * for a report or a console dump. A cloud-to-cloud estimate adds a section with one row per
     * path, the break-even sustained rate, and each figure's provenance.
     *
     * @return a Markdown report
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Egress Savings Estimate\n\n");
        sb.append("**Workload:** ").append(monthlyEgressGb.setScale(0, RoundingMode.HALF_UP).toPlainString())
                .append(" GB/mo egress");
        if (provider != null) {
            sb.append(" from ").append(provider.name());
        }
        if (region != null) {
            sb.append(" (").append(region).append(")");
        }
        if (metro != null) {
            sb.append(" via metro ").append(metro.name());
        }
        sb.append("\n\n");

        sb.append("| Line item | Monthly |\n|---|---|\n");
        sb.append("| Egress over public internet | ").append(money(internetEgressMonthlyCost, currency)).append(" |\n");
        sb.append("| Egress over private interconnect | ").append(money(privateEgressMonthlyCost, currency)).append(" |\n");
        sb.append("| **Egress saving** | **").append(money(monthlyEgressSavings, currency)).append("** |\n");
        sb.append("| Equinix interconnect (recurring) | −").append(money(equinixMonthlyCost, currency)).append(" |\n");
        sb.append("| **Net monthly saving** | **").append(money(netMonthlySavings, currency)).append("** |\n");

        sb.append("\n");
        sb.append("- One-time Equinix setup: ").append(money(equinixSetupCost, currency)).append("\n");
        sb.append("- Annual net saving (steady state): ").append(money(annualNetSavings, currency)).append("\n");
        sb.append("- First-year net saving (incl. setup): ").append(money(firstYearNetSavings, currency)).append("\n");
        if (breakEvenGbPerMonth != null) {
            sb.append("- Break-even egress volume: ")
                    .append(breakEvenGbPerMonth.setScale(0, RoundingMode.HALF_UP).toPlainString())
                    .append(" GB/mo\n");
        }
        if (paybackMonths != null) {
            sb.append("- Setup payback: ").append(paybackMonths.setScale(1, RoundingMode.HALF_UP).toPlainString())
                    .append(" months\n");
        }
        appendMulticloudComparison(sb);
        if (!complete) {
            sb.append("\n> ⚠ Incomplete: ");
            if (!egressPriced) {
                sb.append("egress rates were not available from the rate card. ");
            }
            if (!equinixPriced) {
                sb.append("the Equinix interconnect cost could not be fully priced — "
                        + "the disclaimer names the missing or excluded component. ");
            }
            sb.append("\n");
        }
        if (disclaimer != null) {
            sb.append("\n_").append(disclaimer).append("_\n");
        }
        return sb.toString();
    }
}
