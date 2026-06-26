package api.equinix.javasdk.design.value.tco;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * The result of a {@link TcoCalculator} run: a side-by-side cost comparison of the
 * requested {@link DeploymentArchetype}s, the recommended (lowest-cost) approach,
 * and the saving of the recommended approach versus the public-cloud-over-internet
 * baseline.
 *
 * <p>All figures are design-time estimates; cloud-egress and on-prem inputs are
 * indicative reference figures while Equinix interconnect costs use live pricing
 * where available. See {@link #getDisclaimer()}.</p>
 */
@Value
@Builder
public class TcoComparison {

    /** Per-archetype cost breakdowns, in the order requested. */
    List<CostBreakdown> breakdowns;

    /** The lowest-monthly-cost archetype among those that could be priced. */
    DeploymentArchetype recommended;

    /** The baseline archetype savings are measured against (public cloud over internet). */
    DeploymentArchetype baseline;

    /** Monthly saving of the recommended archetype versus the baseline; {@code null} if not computable. */
    BigDecimal monthlySavingsVsBaseline;

    /** Annual saving of the recommended archetype versus the baseline; {@code null} if not computable. */
    BigDecimal annualSavingsVsBaseline;

    /** Currency code (ISO 4217). */
    String currency;

    /** Provenance/limitations disclaimer. */
    String disclaimer;

    /** The reference-data as-of month (e.g. {@code "2026-06"}). */
    String asOf;

    /** Returns the breakdown for a given archetype, if it was computed. */
    public Optional<CostBreakdown> breakdown(DeploymentArchetype archetype) {
        return breakdowns.stream().filter(b -> b.getArchetype() == archetype).findFirst();
    }

    /** Returns the breakdown for the recommended archetype, if any. */
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
        sb.append("| Approach | Monthly | One-time | |\n|---|---|---|---|\n");
        for (CostBreakdown b : breakdowns) {
            sb.append("| ").append(b.getArchetype().getDisplayName()).append(" | ");
            if (b.isPriced()) {
                sb.append(money(b.getMonthlyTotal(), currency)).append(" | ")
                        .append(money(b.getSetupTotal(), currency)).append(" | ");
            } else {
                sb.append("_unavailable_ | — | ");
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
