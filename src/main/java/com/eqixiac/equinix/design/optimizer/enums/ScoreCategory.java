package com.eqixiac.equinix.design.optimizer.enums;

/**
 * Categories used to decompose a metro's composite score into
 * individually inspectable dimensions.
 *
 * @see com.eqixiac.equinix.design.optimizer.model.MetroScore
 * @see com.eqixiac.equinix.design.optimizer.model.ScoreComponent
 */
public enum ScoreCategory {

    /** Weighted-mean estimated latency from the metro to the user sites, graded on the configured thresholds. */
    LATENCY,

    /** Fraction of required/preferred providers reachable at the metro (required providers weigh double by default). */
    PROVIDER_COVERAGE,

    /** Relative cost position of the metro (higher score = cheaper). */
    COST,

    /** The metro's contribution to geographic diversity of the selected set. */
    REDUNDANCY,

    /** Fraction of requested compliance zones the metro's region is allowed by. */
    COMPLIANCE
}
