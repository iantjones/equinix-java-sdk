package api.equinix.javasdk.design.optimizer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The complete result of a metro optimization run, containing ranked
 * recommendations, deployment topology, latency data, risk assessment,
 * cost estimates, and formatted report outputs.
 */
@Value
@Builder
public class OptimizationResult {

    OptimizationRequest request;
    List<MetroRecommendation> recommendations;
    DeploymentTopology topology;
    ProviderConnectivityMap providerMap;
    LatencyMatrix latencyMatrix;
    RiskAssessment riskAssessment;
    CostEstimate costEstimate;
    OptimizationExplanation explanation;
    Instant computedAt;
    long computeTimeMs;

    /**
     * Returns the top-ranked metro recommendation.
     *
     * @return the rank-1 recommendation, or {@code null} when no metro was viable
     *         (empty recommendation list)
     */
    public MetroRecommendation primaryMetro() {
        return recommendations.isEmpty() ? null : recommendations.get(0);
    }

    /**
     * Returns the top N metro recommendations — a truncation of the already-ranked list,
     * never a re-scoring.
     *
     * @param n the maximum number of recommendations to return
     * @return the first {@code n} recommendations, or all of them when fewer exist
     */
    public List<MetroRecommendation> top(int n) {
        return recommendations.stream().limit(n).collect(Collectors.toList());
    }

    /**
     * Generates a concise plain-text summary of the optimization result.
     */
    public String toSummary() {
        if (recommendations.isEmpty()) {
            return "No viable metros found matching the given constraints.";
        }

        StringBuilder sb = new StringBuilder();
        MetroRecommendation primary = primaryMetro();
        sb.append("Recommended primary metro: ").append(primary.getMetroName())
                .append(" (").append(primary.getMetroId()).append(")")
                .append(" with a score of ").append(String.format("%.1f", primary.getScore().getComposite()))
                .append("/100.\n");

        if (recommendations.size() > 1) {
            sb.append("Additional metros: ");
            sb.append(recommendations.subList(1, recommendations.size()).stream()
                    .map(r -> r.getMetroName() + " (" + r.getMetroId() + ")")
                    .collect(Collectors.joining(", ")));
            sb.append(".\n");
        }

        if (riskAssessment != null && !riskAssessment.critical().isEmpty()) {
            sb.append("WARNING: ").append(riskAssessment.critical().size())
                    .append(" critical risk(s) identified.\n");
        }

        if (costEstimate != null) {
            if (costEstimate.getMonthlyTotal() != null) {
                sb.append("Estimated monthly cost: ")
                        .append(money(costEstimate.getMonthlyTotal(), costEstimate.getCurrency()));
                if (!costEstimate.isWithinBudget()) {
                    sb.append(" (OVER BUDGET)");
                }
                sb.append(".\n");
            } else {
                // The metros span currencies, so there is no single total to state — show the
                // per-currency subtotals rather than a fabricated cross-currency figure.
                sb.append("Estimated monthly cost spans multiple currencies: ")
                        .append(describeByCurrency(costEstimate.getMonthlyByCurrency()))
                        .append(" (no single-currency total).\n");
            }
        }

        sb.append("Computed in ").append(computeTimeMs).append("ms.");
        return sb.toString();
    }

    /**
     * Generates a full markdown-formatted deployment report.
     */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder();
        md.append("# Metro Optimization Report\n\n");
        md.append("_Generated: ").append(computedAt).append(" (").append(computeTimeMs).append("ms)_\n\n");

        // Executive Summary
        md.append("## Executive Summary\n\n");
        md.append(toSummary()).append("\n\n");

        // Recommendations
        md.append("## Ranked Recommendations\n\n");
        for (MetroRecommendation rec : recommendations) {
            md.append("### #").append(rec.getRank()).append(": ")
                    .append(rec.getMetroName()).append(" (").append(rec.getMetroId()).append(")\n\n");
            md.append("- **Region**: ").append(rec.getRegion()).append("\n");
            md.append("- **Composite Score**: ").append(String.format("%.1f", rec.getScore().getComposite())).append("/100\n");
            md.append("- **Score Breakdown**:\n");
            for (ScoreComponent comp : rec.getScore().getComponents()) {
                md.append("  - ").append(comp.getCategory()).append(": ")
                        .append(String.format("%.1f", comp.getScore()))
                        .append(" (weight: ").append(String.format("%.0f%%", comp.getWeight() * 100))
                        .append(") — ").append(comp.getExplanation()).append("\n");
            }
            md.append("- **Reasons**: ").append(String.join("; ", rec.getReasons())).append("\n");

            if (rec.getSiteLatencies() != null && !rec.getSiteLatencies().isEmpty()) {
                md.append("- **Site Latencies**:\n");
                rec.getSiteLatencies().forEach((site, latency) ->
                        md.append("  - ").append(site).append(": ")
                                .append(String.format("%.1fms", latency)).append("\n"));
            }

            if (rec.getAvailableProviders() != null && !rec.getAvailableProviders().isEmpty()) {
                md.append("- **Providers Available**: ");
                md.append(rec.getAvailableProviders().stream()
                        .filter(ProviderAvailability::isAvailable)
                        .map(ProviderAvailability::getProviderLabel)
                        .collect(Collectors.joining(", ")));
                md.append("\n");
            }
            md.append("\n");
        }

        // Latency Matrix
        if (latencyMatrix != null && !latencyMatrix.getMetros().isEmpty()) {
            md.append("## Latency Matrix\n\n");
            md.append("```\n").append(latencyMatrix.toTableString()).append("\n```\n\n");
        }

        // Deployment Topology
        if (topology != null && !topology.getPlacements().isEmpty()) {
            md.append("## Deployment Topology\n\n");
            md.append(topology.summary()).append("\n");
        }

        // Risk Assessment
        if (riskAssessment != null && !riskAssessment.getFindings().isEmpty()) {
            md.append("## Risk Assessment\n\n");
            md.append("Resiliency Score: ").append(String.format("%.1f", riskAssessment.getResiliencyScore()))
                    .append("/100\n\n");
            for (RiskFinding finding : riskAssessment.getFindings()) {
                md.append("- **[").append(finding.getSeverity()).append("]** ")
                        .append(finding.getDescription());
                if (finding.getRecommendation() != null) {
                    md.append("\n  _Recommendation: ").append(finding.getRecommendation()).append("_");
                }
                md.append("\n");
            }
            md.append("\n");
        }

        // Cost Estimate. Every figure carries its own currency: per-metro rows render the row's
        // currency (live Fabric pricing legitimately quotes EUR in Frankfurt next to USD in
        // Ashburn), and the total renders the aggregate currency. A hardcoded "$" against a
        // non-USD figure misstated the amount by whatever the exchange rate happened to be.
        if (costEstimate != null) {
            md.append("## Cost Estimate\n\n");
            md.append("| Metro | Monthly | Setup |\n");
            md.append("|-------|--------:|------:|\n");
            for (MetroCostBreakdown mcb : costEstimate.getPerMetro()) {
                String rowCurrency = mcb.getCurrency() != null
                        ? mcb.getCurrency() : costEstimate.getCurrency();
                md.append("| ").append(mcb.getMetroId())
                        .append(" | ").append(money(mcb.getMonthlyRecurring(), rowCurrency))
                        .append(" | ").append(money(mcb.getNonRecurring(), rowCurrency)).append(" |\n");
            }
            if (costEstimate.getMonthlyTotal() != null) {
                md.append("| **Total** | **")
                        .append(money(costEstimate.getMonthlyTotal(), costEstimate.getCurrency()))
                        .append("** | **")
                        .append(money(costEstimate.getSetupTotal(), costEstimate.getCurrency()))
                        .append("** |\n\n");
            } else {
                md.append("| **Total** | **")
                        .append(describeByCurrency(costEstimate.getMonthlyByCurrency()))
                        .append("** | _multiple currencies_ |\n\n");
            }
            md.append("_").append(costEstimate.getCostDisclaimer()).append("_\n\n");
        }

        // Explanation
        if (explanation != null) {
            md.append("## Methodology\n\n");
            md.append(explanation.getMethodology()).append("\n\n");
            if (explanation.getAssumptions() != null && !explanation.getAssumptions().isEmpty()) {
                md.append("**Assumptions**:\n");
                explanation.getAssumptions().forEach(a -> md.append("- ").append(a).append("\n"));
            }
        }

        return md.toString();
    }

    /**
     * Renders a monetary amount with the symbol of its actual currency ({@code "$2300.00"},
     * {@code "€1800.00"}), falling back to {@code "<amount> <code>"} when the code has no distinct
     * symbol or is unknown, and to the bare amount when no currency was stated at all. Replaces the
     * former hardcoded {@code "$"}, which asserted US dollars against figures that live Fabric
     * pricing legitimately quotes in other currencies.
     */
    private static String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return "unavailable";
        }
        if (currency == null || currency.isBlank()) {
            return amount.toPlainString();
        }
        try {
            String symbol = java.util.Currency.getInstance(currency).getSymbol(java.util.Locale.US);
            if (!symbol.equals(currency)) {
                return symbol + amount.toPlainString();
            }
        }
        catch (IllegalArgumentException notIso4217) {
            // Not an ISO 4217 code: fall through to the "<amount> <code>" form below.
        }
        return amount.toPlainString() + " " + currency;
    }

    /**
     * Renders a per-currency monthly breakdown, e.g. {@code "USD 2300.00, EUR 1800.00"}, used when
     * the metros span currencies and no single aggregate total exists.
     */
    private static String describeByCurrency(Map<String, BigDecimal> monthlyByCurrency) {
        if (monthlyByCurrency == null || monthlyByCurrency.isEmpty()) {
            return "unavailable";
        }
        return monthlyByCurrency.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue().toPlainString())
                .collect(Collectors.joining(", "));
    }

    /**
     * Serializes the result to JSON for programmatic consumption.
     */
    @JsonIgnore
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            // Register the JavaTime module explicitly so the Instant computedAt field always
            // serializes, independent of module auto-discovery on the runtime classpath, and as a
            // readable ISO-8601 string rather than a numeric timestamp.
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(this);
        }
        catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
