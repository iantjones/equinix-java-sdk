package com.eqixiac.equinix.design.value.tco;

import com.eqixiac.equinix.design.value.ratecard.Term;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * The result of a {@link TcoCalculator} run: a side-by-side cost comparison of the
 * requested {@link DeploymentArchetype}s, the recommended approach - the lowest
 * <em>total cost over the commitment term</em> ({@code MRC × months + NRC}), so
 * one-time setup charges count - and the saving of the recommended approach versus
 * the public-cloud-over-internet baseline, both per month and over the term.
 *
 * <p>A cloud-to-cloud comparison ({@code TcoCalculator.Builder.toCloud(...)}) is two-sided:
 * every archetype prices traffic in both directions, the native multicloud archetype joins the
 * comparison, and on-prem (whose inputs carry no cloud egress) leaves the default set and is
 * reported unpriced when requested. {@code getTrafficNote()} states the volumes and
 * assumptions; {@code CostBreakdown.getProvenance()} carries per-figure sources.</p>
 *
 * <p>All figures are design-time estimates; cloud-egress and on-prem inputs are
 * indicative reference figures while Equinix interconnect costs use live pricing
 * where available. See {@code getDisclaimer()}. Each breakdown carries the
 * currency its own components reconciled to (see {@code CostBreakdown.getCurrency()}),
 * which may differ from the comparison-wide {@code getCurrency()}; savings are only
 * computed when baseline and recommended share a currency.</p>
 */
@Value
@Builder
public class TcoComparison {

    /** One breakdown per requested archetype, in comparison order. */
    List<CostBreakdown> breakdowns;

    /**
     * The archetype with the lowest total cost over the term among those that priced - 
     * {@code null} when no archetype could be priced at all.
     */
    DeploymentArchetype recommended;

    /** The savings baseline (always {@code PUBLIC_CLOUD_INTERNET}). */
    DeploymentArchetype baseline;

    /**
     * Monthly saving of the recommended archetype versus the baseline. {@code null} when
     * either side is unpriced or the two are priced in different currencies (no FX rate
     * is ever fabricated) - guard before dereferencing.
     */
    BigDecimal monthlySavingsVsBaseline;

    /**
     * {@code getMonthlySavingsVsBaseline() × 12}; {@code null} under the same conditions.
     */
    BigDecimal annualSavingsVsBaseline;

    /**
     * Saving of the recommended archetype versus the baseline over the full commitment
     * term ({@code baseline totalOverTerm - recommended totalOverTerm}), which accounts
     * for one-time setup charges. Null when either side is unpriced or the currencies differ.
     */
    BigDecimal savingsOverTermVsBaseline;

    /** The commitment term the comparison was priced over. */
    Term term;

    String currency;

    String disclaimer;

    String asOf;

    /**
     * For a cloud-to-cloud comparison ({@code toCloud(...)} set): the two providers and regions,
     * the monthly volume in each direction (and whether the reverse volume was assumed equal to
     * the forward volume), the link size in Mbps and the path tier. {@code null} for a
     * single-cloud comparison.
     */
    String trafficNote;

    /**
     * How the recommendation relates to incomplete archetypes: {@code null} when every requested archetype
     * priced; otherwise names each incomplete one and whether its priced part already exceeds the
     * recommendation (so it cannot win) or not (so the recommendation is provisional).
     */
    String recommendationNote;

    /** The fingerprint of the spec this comparison was derived from ({@code forSpec}), else {@code null}. */
    String specFingerprint;

    /** What {@code forSpec} assumed where the spec was silent; empty otherwise. */
    @lombok.Builder.Default
    List<String> assumptions = List.of();

    /**
     * The breakdown for a specific archetype.
     *
     * @param archetype the archetype to look up
     * @return its breakdown, or empty when the archetype was not part of this comparison
     */
    public Optional<CostBreakdown> breakdown(DeploymentArchetype archetype) {
        return breakdowns.stream().filter(b -> b.getArchetype() == archetype).findFirst();
    }

    /**
     * The recommended archetype's breakdown.
     *
     * @return the breakdown of {@code getRecommended()}, or empty when nothing could be priced
     */
    public Optional<CostBreakdown> recommendedBreakdown() {
        return recommended == null ? Optional.empty() : breakdown(recommended);
    }

    private static String money(BigDecimal v, String currency) {
        if (v == null) {
            return "n/a";
        }
        return currency + " " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Appends the line items and price provenance of a breakdown that carries provenance (the
     * native multicloud archetype and the two-sided form of the others). A breakdown without
     * provenance adds nothing, so a single-cloud report is unchanged.
     */
    private void appendProvenance(StringBuilder sb, CostBreakdown b) {
        if (b.getProvenance() == null || b.getProvenance().isEmpty()) {
            return;
        }
        String rowCurrency = b.getCurrency() != null ? b.getCurrency() : currency;
        sb.append("\n### ").append(b.getArchetype().getDisplayName()).append(": line items and provenance\n\n");
        if (b.getLineItems() != null && !b.getLineItems().isEmpty()) {
            sb.append("| Line item | Monthly |\n|---|---|\n");
            b.getLineItems().forEach((label, amount) ->
                    sb.append("| ").append(label).append(" | ").append(money(amount, rowCurrency)).append(" |\n"));
            sb.append("\n");
        }
        if (!b.isPriced() && b.getNote() != null) {
            sb.append("- Not fully priced: ").append(b.getNote()).append("\n");
        }
        for (String line : b.getProvenance()) {
            sb.append("- ").append(line).append("\n");
        }
    }

    /**
     * Renders a Markdown report comparing the archetypes and stating the recommendation.
     * A cloud-to-cloud comparison adds a traffic line and, per archetype, a line-item table with
     * the provenance of each figure, including the 730 h/month hourly-to-monthly conversion used
     * for native multicloud link rates.
     *
     * @return a Markdown report
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Total Cost of Ownership: Deployment Comparison\n\n");
        if (term != null) {
            sb.append("**Term:** ").append(term.months())
                    .append(term.months() == 1 ? " month" : " months")
                    .append(" (archetypes are ranked by total cost over the term, including one-time setup)\n\n");
        }
        if (trafficNote != null) {
            sb.append("**Traffic:** ").append(trafficNote).append("\n\n");
        }
        sb.append("| Approach | Monthly | One-time | Total over term | |\n|---|---|---|---|---|\n");
        for (CostBreakdown b : breakdowns) {
            // Each row renders in the currency its own components reconciled to, so a
            // breakdown priced in another currency is never mislabelled with the
            // comparison-wide one.
            String rowCurrency = b.getCurrency() != null ? b.getCurrency() : currency;
            sb.append("| ").append(b.getArchetype().getDisplayName()).append(" | ");
            if (b.isPriced()) {
                sb.append(money(b.getMonthlyTotal(), rowCurrency)).append(" | ")
                        .append(money(b.getSetupTotal(), rowCurrency)).append(" | ")
                        .append(money(b.getTotalOverTerm(), rowCurrency)).append(" | ");
            } else {
                sb.append("_incomplete_ | n/a | n/a | ");
            }
            sb.append(b.getArchetype() == recommended ? "recommended" : "").append(" |\n");
        }
        sb.append("\n");
        if (recommendationNote != null) {
            sb.append("**Completeness:** ").append(recommendationNote).append("\n\n");
        }
        boolean wrote = false;
        for (CostBreakdown b : breakdowns) {
            if (b.getUnpriced() != null && !b.getUnpriced().isEmpty()) {
                sb.append("- ").append(b.getArchetype().getDisplayName()).append(" is incomplete:\n");
                for (String u : b.getUnpriced()) {
                    sb.append("  - unpriced: ").append(u).append("\n");
                }
                wrote = true;
            }
        }
        if (specFingerprint != null) {
            sb.append("\n_Derived from spec fingerprint `").append(specFingerprint, 0, Math.min(12, specFingerprint.length()))
                    .append("`_\n");
            for (String a : assumptions) {
                sb.append("- Assumed: ").append(a).append("\n");
            }
            wrote = true;
        }
        if (wrote) {
            sb.append("\n");
        }
        if (recommended != null && monthlySavingsVsBaseline != null) {
            sb.append("**Recommended:** ").append(recommended.getDisplayName()).append("\n\n");
            sb.append("- Monthly saving vs. ").append(baseline.getDisplayName()).append(": ")
                    .append(money(monthlySavingsVsBaseline, currency)).append("\n");
            sb.append("- Annual saving vs. ").append(baseline.getDisplayName()).append(": ")
                    .append(money(annualSavingsVsBaseline, currency)).append("\n");
            if (savingsOverTermVsBaseline != null && term != null) {
                sb.append("- Saving over the ").append(term.months()).append("-month term vs. ")
                        .append(baseline.getDisplayName()).append(": ")
                        .append(money(savingsOverTermVsBaseline, currency)).append("\n");
            }
        } else if (recommended != null) {
            sb.append("**Recommended:** ").append(recommended.getDisplayName()).append("\n");
        }
        for (CostBreakdown b : breakdowns) {
            appendProvenance(sb, b);
        }
        if (disclaimer != null) {
            sb.append("\n_").append(disclaimer);
            if (asOf != null) {
                sb.append(" (reference data as of ").append(asOf).append(")");
            }
            sb.append("_\n");
        }
        return sb.toString();
    }
}
