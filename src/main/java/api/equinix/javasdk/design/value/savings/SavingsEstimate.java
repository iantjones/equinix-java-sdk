package api.equinix.javasdk.design.value.savings;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The result of a {@link SavingsCalculator} run: the modelled monthly cost of
 * egressing data over the public internet versus over an Equinix-provided private
 * interconnect, the Equinix interconnect cost, and the resulting net savings and
 * break-even points.
 *
 * <p>All amounts are in {@link #currency}. The {@code *Priced} flags indicate
 * which inputs the rate card could actually resolve; when a flag is {@code false}
 * the corresponding figures are zero and {@link #netMonthlySavings} should be read
 * as incomplete (see {@link #complete}).</p>
 */
@Value
@Builder
public class SavingsEstimate {

    BigDecimal monthlyEgressGb;

    CloudProviderType provider;

    String region;

    MetroCode metro;

    BigDecimal internetRatePerGb;

    BigDecimal privateRatePerGb;

    BigDecimal internetEgressMonthlyCost;

    BigDecimal privateEgressMonthlyCost;

    BigDecimal monthlyEgressSavings;

    BigDecimal equinixMonthlyCost;

    BigDecimal equinixSetupCost;

    BigDecimal netMonthlySavings;

    BigDecimal annualNetSavings;

    BigDecimal firstYearNetSavings;

    BigDecimal breakEvenGbPerMonth;

    BigDecimal paybackMonths;

    String currency;

    boolean egressPriced;

    boolean equinixPriced;

    boolean complete;

    String disclaimer;

    private static String money(BigDecimal v, String currency) {
        if (v == null) {
            return "n/a";
        }
        return currency + " " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Renders a human-readable Markdown summary of the savings analysis, suitable
     * for a report or a console dump.
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
        if (!complete) {
            sb.append("\n> ⚠ Incomplete: ");
            if (!egressPriced) {
                sb.append("egress rates were not available from the rate card. ");
            }
            if (!equinixPriced) {
                sb.append("the Equinix interconnect cost was not available from the rate card. ");
            }
            sb.append("\n");
        }
        if (disclaimer != null) {
            sb.append("\n_").append(disclaimer).append("_\n");
        }
        return sb.toString();
    }
}
