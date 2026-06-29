package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The value-realization view of a {@link DeploymentPlan}: the monthly cloud-egress
 * saving the plan's private interconnects unlock (versus public-internet egress),
 * netted against the plan's actual Equinix interconnect cost.
 *
 * <p>Produced by {@link DeploymentPlan#valueRealization()} after the caller
 * declares their per-provider egress volumes. The plan's monthly cost is the real
 * (live-priced) interconnect spend from {@link PlanPricing}, so the net figure is
 * an honest "is this deployment worth it" number rather than a double-count.</p>
 */
@Value
@Builder
public class PlanValueRealization {

    BigDecimal planMonthlyCost;

    BigDecimal planSetupCost;

    BigDecimal totalMonthlyEgressSavings;

    BigDecimal netMonthlySavings;

    BigDecimal annualNetSavings;

    BigDecimal firstYearNetSavings;

    List<ProviderEgressSaving> perProvider;

    String currency;

    String disclaimer;

    @Value
    @Builder
    public static class ProviderEgressSaving {
        CloudProviderType provider;
        BigDecimal monthlyEgressGb;
        BigDecimal internetMonthlyCost;
        BigDecimal privateMonthlyCost;
        BigDecimal monthlySavings;
        boolean priced;
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
            sb.append("| ").append(p.getProvider() == null ? "—" : p.getProvider().name())
                    .append(" | ").append(p.getMonthlyEgressGb().setScale(0, RoundingMode.HALF_UP).toPlainString())
                    .append(" | ").append(p.isPriced() ? money(p.getInternetMonthlyCost(), currency) : "n/a")
                    .append(" | ").append(p.isPriced() ? money(p.getPrivateMonthlyCost(), currency) : "n/a")
                    .append(" | ").append(p.isPriced() ? money(p.getMonthlySavings(), currency) : "n/a")
                    .append(" |\n");
        }
        sb.append("\n");
        sb.append("- **Total egress saving:** ").append(money(totalMonthlyEgressSavings, currency)).append("/mo\n");
        sb.append("- Plan interconnect cost: −").append(money(planMonthlyCost, currency)).append("/mo\n");
        sb.append("- **Net monthly saving:** ").append(money(netMonthlySavings, currency)).append("\n");
        sb.append("- Annual net saving: ").append(money(annualNetSavings, currency)).append("\n");
        sb.append("- First-year net saving (incl. setup): ").append(money(firstYearNetSavings, currency)).append("\n");
        if (disclaimer != null) {
            sb.append("\n_").append(disclaimer).append("_\n");
        }
        return sb.toString();
    }
}
