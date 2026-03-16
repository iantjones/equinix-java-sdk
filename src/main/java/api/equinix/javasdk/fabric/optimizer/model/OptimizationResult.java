package api.equinix.javasdk.fabric.optimizer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
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
     */
    public MetroRecommendation primaryMetro() {
        return recommendations.isEmpty() ? null : recommendations.get(0);
    }

    /**
     * Returns the top N metro recommendations.
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
                .append(" (").append(primary.getMetroCode()).append(")")
                .append(" with a score of ").append(String.format("%.1f", primary.getScore().getComposite()))
                .append("/100.\n");

        if (recommendations.size() > 1) {
            sb.append("Additional metros: ");
            sb.append(recommendations.subList(1, recommendations.size()).stream()
                    .map(r -> r.getMetroName() + " (" + r.getMetroCode() + ")")
                    .collect(Collectors.joining(", ")));
            sb.append(".\n");
        }

        if (riskAssessment != null && !riskAssessment.critical().isEmpty()) {
            sb.append("WARNING: ").append(riskAssessment.critical().size())
                    .append(" critical risk(s) identified.\n");
        }

        if (costEstimate != null) {
            sb.append("Estimated monthly cost: $").append(costEstimate.getMonthlyTotal())
                    .append(" ").append(costEstimate.getCurrency());
            if (!costEstimate.isWithinBudget()) {
                sb.append(" (OVER BUDGET)");
            }
            sb.append(".\n");
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
                    .append(rec.getMetroName()).append(" (").append(rec.getMetroCode()).append(")\n\n");
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

        // Cost Estimate
        if (costEstimate != null) {
            md.append("## Cost Estimate\n\n");
            md.append("| Metro | Monthly | Setup |\n");
            md.append("|-------|--------:|------:|\n");
            for (MetroCostBreakdown mcb : costEstimate.getPerMetro()) {
                md.append("| ").append(mcb.getMetroCode())
                        .append(" | $").append(mcb.getMonthlyRecurring())
                        .append(" | $").append(mcb.getNonRecurring()).append(" |\n");
            }
            md.append("| **Total** | **$").append(costEstimate.getMonthlyTotal())
                    .append("** | **$").append(costEstimate.getSetupTotal()).append("** |\n\n");
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
     * Serializes the result to JSON for programmatic consumption.
     */
    @JsonIgnore
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(this);
        }
        catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
