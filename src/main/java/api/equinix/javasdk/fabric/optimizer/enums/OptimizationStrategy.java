package api.equinix.javasdk.fabric.optimizer.enums;

import lombok.Getter;

/**
 * Pre-defined optimization strategies that set default scoring weights.
 * Users can further customize weights via
 * {@link api.equinix.javasdk.fabric.optimizer.model.ScoringWeights}.
 *
 * @see api.equinix.javasdk.fabric.optimizer.model.ScoringWeights
 */
@Getter
public enum OptimizationStrategy {

    BALANCED(0.30, 0.25, 0.20, 0.15, 0.10),
    LATENCY_FIRST(0.50, 0.20, 0.10, 0.10, 0.10),
    COST_FIRST(0.15, 0.15, 0.45, 0.15, 0.10),
    REDUNDANCY_FIRST(0.20, 0.15, 0.15, 0.40, 0.10),
    PROVIDER_COVERAGE_FIRST(0.20, 0.45, 0.15, 0.10, 0.10);

    private final double latencyWeight;
    private final double providerCoverageWeight;
    private final double costWeight;
    private final double redundancyWeight;
    private final double complianceWeight;

    OptimizationStrategy(double latencyWeight, double providerCoverageWeight,
                         double costWeight, double redundancyWeight, double complianceWeight) {
        this.latencyWeight = latencyWeight;
        this.providerCoverageWeight = providerCoverageWeight;
        this.costWeight = costWeight;
        this.redundancyWeight = redundancyWeight;
        this.complianceWeight = complianceWeight;
    }
}
