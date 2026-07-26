package com.eqixiac.equinix.design.optimizer.enums;

import lombok.Getter;

/**
 * Pre-defined optimization strategies that set default scoring weights.
 * Users can further customize weights via
 * {@link com.eqixiac.equinix.design.optimizer.model.ScoringWeights}.
 *
 * <p>Each constant carries a weight per scoring dimension, in the order latency,
 * provider coverage, cost, redundancy, compliance. The engine sums the effective
 * weights (after any {@code ScoringWeights} overrides) and renormalizes them to 1,
 * so the values below are relative shares, not absolute multipliers.</p>
 *
 * @see com.eqixiac.equinix.design.optimizer.model.ScoringWeights
 */
@Getter
public enum OptimizationStrategy {

    /** Even-handed default: latency 0.30, provider coverage 0.25, cost 0.20, redundancy 0.15, compliance 0.10. */
    BALANCED(0.30, 0.25, 0.20, 0.15, 0.10),

    /** Prioritizes proximity to user sites: latency 0.50, provider coverage 0.20, cost 0.10, redundancy 0.10, compliance 0.10. */
    LATENCY_FIRST(0.50, 0.20, 0.10, 0.10, 0.10),

    /** Prioritizes estimated monthly cost: latency 0.15, provider coverage 0.15, cost 0.45, redundancy 0.15, compliance 0.10. */
    COST_FIRST(0.15, 0.15, 0.45, 0.15, 0.10),

    /** Prioritizes geographic diversity: latency 0.20, provider coverage 0.15, cost 0.15, redundancy 0.40, compliance 0.10. */
    REDUNDANCY_FIRST(0.20, 0.15, 0.15, 0.40, 0.10),

    /** Prioritizes cloud/service-profile reach: latency 0.20, provider coverage 0.45, cost 0.15, redundancy 0.10, compliance 0.10. */
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
