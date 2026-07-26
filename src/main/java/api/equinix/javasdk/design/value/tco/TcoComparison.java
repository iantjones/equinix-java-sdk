package api.equinix.javasdk.design.value.tco;

import api.equinix.javasdk.design.value.ratecard.Term;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * The result of a {@link TcoCalculator} run: a side-by-side cost comparison of the
 * requested {@link DeploymentArchetype}s, the recommended approach — the lowest
 * <em>total cost over the commitment term</em> ({@code MRC × months + NRC}), so
 * one-time setup charges count — and the saving of the recommended approach versus
 * the public-cloud-over-internet baseline, both per month and over the term.
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
     * The archetype with the lowest total cost over the term among those that priced —
     * {@code null} when no archetype could be priced at all.
     */
    DeploymentArchetype recommended;

    /** The savings baseline (always {@code PUBLIC_CLOUD_INTERNET}). */
    DeploymentArchetype baseline;

    /**
     * Monthly saving of the recommended archetype versus the baseline. {@code null} when
     * either side is unpriced or the two are priced in different currencies (no FX rate
     * is ever fabricated) — guard before dereferencing.
     */
    BigDecimal monthlySavingsVsBaseline;

    /**
     * {@code getMonthlySavingsVsBaseline() × 12}; {@code null} under the same conditions.
     */
    BigDecimal annualSavingsVsBaseline;

    /**
     * Saving of the recommended archetype versus the baseline over the full commitment
     * term ({@code baseline totalOverTerm − recommended totalOverTerm}), which accounts
     * for one-time setup charges. Null when either side is unpriced or the currencies differ.
     */
    BigDecimal savingsOverTermVsBaseline;

    /** The commitment term the comparison was priced over. */
    Term term;

    String currency;

    String disclaimer;

    String asOf;

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
     * Renders a Markdown report comparing the archetypes and stating the recommendation.
     *
     * @return a Markdown report
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Total Cost of Ownership — Deployment Comparison\n\n");
        if (term != null) {
            sb.append("**Term:** ").append(term.months())
                    .append(term.months() == 1 ? " month" : " months")
                    .append(" (archetypes are ranked by total cost over the term, including one-time setup)\n\n");
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
                sb.append("_unavailable_ | — | — | ");
            }
            sb.append(b.getArchetype() == recommended ? "✅ recommended" : "").append(" |\n");
        }
        sb.append("\n");
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
