package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.optimizer.enums.ScoreCategory;
import lombok.Builder;
import lombok.Value;

/**
 * A single dimension of a metro's composite score.
 */
@Value
@Builder
public class ScoreComponent {

    /** The dimension this component grades. */
    ScoreCategory category;

    /** The unweighted score for this dimension, 0&ndash;100. */
    double score;

    /** The dimension's normalized weight (the five components' weights sum to 1). */
    double weight;

    /** Human-readable statement of why the dimension scored what it did. */
    String explanation;

    /**
     * The weighted contribution of this component to the composite score.
     */
    public double weightedScore() {
        return score * weight;
    }
}
