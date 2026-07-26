package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.optimizer.enums.OptimizationStrategy;
import lombok.Builder;
import lombok.Value;

/**
 * User-overridable scoring weights and thresholds for the optimization engine.
 * When provided alongside an {@link OptimizationStrategy}, non-null fields in
 * this object take precedence over the strategy's defaults.
 *
 * <p><strong>Resolution contract</strong>: every field is a nullable box, and every
 * {@code resolve*} method applies the same rule — an explicitly set (non-null) value wins,
 * a {@code null} field falls back to the default (the strategy's weight for the five category
 * weights, a fixed built-in for the thresholds). {@link #defaults()} therefore yields pure
 * strategy/built-in behavior.</p>
 *
 * <p><strong>Weights are relative shares</strong>: the engine sums the five resolved category
 * weights and renormalizes them to 1 before scoring, so only their ratios matter. Setting all
 * five to zero degrades ranking to catalog order — no validation prevents it.</p>
 *
 * <p><strong>Latency thresholds are curve anchors in milliseconds</strong>: the latency score is a
 * continuous piecewise-linear curve through 100 at 0&nbsp;ms, 95 at <em>excellent</em>, 75 at
 * <em>good</em>, 50 at <em>acceptable</em>, and 0 at <em>poor</em> and beyond. The four thresholds
 * must be strictly ascending for the curve to be well-formed; this is a caller obligation — no
 * validation enforces it.</p>
 *
 * <p>Every field here is a live lever: each is read by the engine's scoring or
 * grading paths. A former {@code costTolerancePercent} field was removed because
 * nothing consumed it &mdash; it was accepted, echoed back by the methodology as
 * an "active override", and then ignored, which misrepresented what the run had
 * optimized for.</p>
 */
@Value
@Builder
public class ScoringWeights {

    /** Relative share for the latency dimension; {@code null} = the strategy's default. */
    Double latencyWeight;

    /** Relative share for the provider-coverage dimension; {@code null} = the strategy's default. */
    Double providerCoverageWeight;

    /** Relative share for the cost dimension; {@code null} = the strategy's default. */
    Double costWeight;

    /** Relative share for the redundancy dimension; {@code null} = the strategy's default. */
    Double redundancyWeight;

    /** Relative share for the compliance dimension; {@code null} = the strategy's default. */
    Double complianceWeight;

    /** Latency curve anchor scoring 95, in ms; {@code null} = 10&nbsp;ms. */
    Double latencyExcellentMs;

    /** Latency curve anchor scoring 75, in ms; {@code null} = 30&nbsp;ms. */
    Double latencyGoodMs;

    /** Latency curve anchor scoring 50, in ms; {@code null} = 80&nbsp;ms. */
    Double latencyAcceptableMs;

    /** Latency curve anchor scoring 0, in ms; {@code null} = 150&nbsp;ms. */
    Double latencyPoorMs;

    /**
     * How much more a <em>required</em> provider counts than a preferred one in the
     * provider-coverage score; {@code null} = 2.0 (required counts double).
     */
    Double requiredProviderWeight;

    /**
     * The effective latency weight: this object's value if set, else the strategy's.
     *
     * @param strategy the strategy supplying the default
     * @return the effective relative share (renormalized with its peers before use)
     */
    public double resolveLatencyWeight(OptimizationStrategy strategy) {
        return latencyWeight != null ? latencyWeight : strategy.getLatencyWeight();
    }

    /**
     * The effective provider-coverage weight: this object's value if set, else the strategy's.
     *
     * @param strategy the strategy supplying the default
     * @return the effective relative share
     */
    public double resolveProviderCoverageWeight(OptimizationStrategy strategy) {
        return providerCoverageWeight != null ? providerCoverageWeight : strategy.getProviderCoverageWeight();
    }

    /**
     * The effective cost weight: this object's value if set, else the strategy's.
     *
     * @param strategy the strategy supplying the default
     * @return the effective relative share
     */
    public double resolveCostWeight(OptimizationStrategy strategy) {
        return costWeight != null ? costWeight : strategy.getCostWeight();
    }

    /**
     * The effective redundancy weight: this object's value if set, else the strategy's.
     *
     * @param strategy the strategy supplying the default
     * @return the effective relative share
     */
    public double resolveRedundancyWeight(OptimizationStrategy strategy) {
        return redundancyWeight != null ? redundancyWeight : strategy.getRedundancyWeight();
    }

    /**
     * The effective compliance weight: this object's value if set, else the strategy's.
     *
     * @param strategy the strategy supplying the default
     * @return the effective relative share
     */
    public double resolveComplianceWeight(OptimizationStrategy strategy) {
        return complianceWeight != null ? complianceWeight : strategy.getComplianceWeight();
    }

    /** @return the effective <em>excellent</em> latency anchor in ms (default 10.0) */
    public double resolveLatencyExcellentMs() {
        return latencyExcellentMs != null ? latencyExcellentMs : 10.0;
    }

    /** @return the effective <em>good</em> latency anchor in ms (default 30.0) */
    public double resolveLatencyGoodMs() {
        return latencyGoodMs != null ? latencyGoodMs : 30.0;
    }

    /** @return the effective <em>acceptable</em> latency anchor in ms (default 80.0) */
    public double resolveLatencyAcceptableMs() {
        return latencyAcceptableMs != null ? latencyAcceptableMs : 80.0;
    }

    /** @return the effective <em>poor</em> latency anchor in ms (default 150.0) */
    public double resolveLatencyPoorMs() {
        return latencyPoorMs != null ? latencyPoorMs : 150.0;
    }

    /** @return the effective required-provider multiplier (default 2.0) */
    public double resolveRequiredProviderWeight() {
        return requiredProviderWeight != null ? requiredProviderWeight : 2.0;
    }

    /**
     * An all-null instance: every {@code resolve*} call falls through to the strategy default or
     * the built-in threshold.
     *
     * @return weights that impose no overrides
     */
    public static ScoringWeights defaults() {
        return ScoringWeights.builder().build();
    }
}
