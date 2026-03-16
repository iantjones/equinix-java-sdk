package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.fabric.optimizer.enums.OptimizationStrategy;
import lombok.Builder;
import lombok.Value;

/**
 * User-overridable scoring weights and thresholds for the optimization engine.
 * When provided alongside an {@link OptimizationStrategy}, non-null fields in
 * this object take precedence over the strategy's defaults.
 */
@Value
@Builder
public class ScoringWeights {

    Double latencyWeight;
    Double providerCoverageWeight;
    Double costWeight;
    Double redundancyWeight;
    Double complianceWeight;

    Double latencyExcellentMs;
    Double latencyGoodMs;
    Double latencyAcceptableMs;
    Double latencyPoorMs;

    Double requiredProviderWeight;
    Double costTolerancePercent;

    /**
     * Resolves the effective weight for a scoring category by overlaying
     * user overrides onto strategy defaults.
     */
    public double resolveLatencyWeight(OptimizationStrategy strategy) {
        return latencyWeight != null ? latencyWeight : strategy.getLatencyWeight();
    }

    public double resolveProviderCoverageWeight(OptimizationStrategy strategy) {
        return providerCoverageWeight != null ? providerCoverageWeight : strategy.getProviderCoverageWeight();
    }

    public double resolveCostWeight(OptimizationStrategy strategy) {
        return costWeight != null ? costWeight : strategy.getCostWeight();
    }

    public double resolveRedundancyWeight(OptimizationStrategy strategy) {
        return redundancyWeight != null ? redundancyWeight : strategy.getRedundancyWeight();
    }

    public double resolveComplianceWeight(OptimizationStrategy strategy) {
        return complianceWeight != null ? complianceWeight : strategy.getComplianceWeight();
    }

    public double resolveLatencyExcellentMs() {
        return latencyExcellentMs != null ? latencyExcellentMs : 10.0;
    }

    public double resolveLatencyGoodMs() {
        return latencyGoodMs != null ? latencyGoodMs : 30.0;
    }

    public double resolveLatencyAcceptableMs() {
        return latencyAcceptableMs != null ? latencyAcceptableMs : 80.0;
    }

    public double resolveLatencyPoorMs() {
        return latencyPoorMs != null ? latencyPoorMs : 150.0;
    }

    public double resolveRequiredProviderWeight() {
        return requiredProviderWeight != null ? requiredProviderWeight : 2.0;
    }

    public double resolveCostTolerancePercent() {
        return costTolerancePercent != null ? costTolerancePercent : 30.0;
    }

    public static ScoringWeights defaults() {
        return ScoringWeights.builder().build();
    }
}
