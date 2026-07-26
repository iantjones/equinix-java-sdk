package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.optimizer.enums.ScoreCategory;
import lombok.Value;

import java.util.List;

/**
 * Composite score for a metro recommendation, decomposed into individually
 * inspectable {@link ScoreComponent} dimensions.
 *
 * <p>All scores — the composite and every per-category component — are on a 0&ndash;100
 * scale, higher is better. The composite is the sum of each component's score multiplied
 * by its normalized weight.</p>
 */
@Value
public class MetroScore {

    /** The weighted composite score, 0&ndash;100. */
    double composite;

    /** One entry per scoring dimension the engine graded. */
    List<ScoreComponent> components;

    /**
     * The 0&ndash;100 score for one category.
     *
     * <p><strong>Absent-category caveat</strong>: a category with no component returns
     * {@code 0.0}, which is indistinguishable from a genuine zero score. Engine-produced
     * scores always carry all five {@link ScoreCategory} components, so the ambiguity only
     * arises on hand-built instances.</p>
     *
     * @param category the dimension to read
     * @return the category's score, or {@code 0.0} when no such component exists
     */
    public double scoreFor(ScoreCategory category) {
        return components.stream()
                .filter(c -> c.getCategory() == category)
                .findFirst()
                .map(ScoreComponent::getScore)
                .orElse(0.0);
    }

    /** @return the {@link ScoreCategory#LATENCY} score, 0&ndash;100 */
    public double latencyScore() {
        return scoreFor(ScoreCategory.LATENCY);
    }

    /** @return the {@link ScoreCategory#PROVIDER_COVERAGE} score, 0&ndash;100 */
    public double providerScore() {
        return scoreFor(ScoreCategory.PROVIDER_COVERAGE);
    }

    /** @return the {@link ScoreCategory#COST} score, 0&ndash;100 */
    public double costScore() {
        return scoreFor(ScoreCategory.COST);
    }

    /** @return the {@link ScoreCategory#REDUNDANCY} score, 0&ndash;100 */
    public double redundancyScore() {
        return scoreFor(ScoreCategory.REDUNDANCY);
    }

    /** @return the {@link ScoreCategory#COMPLIANCE} score, 0&ndash;100 */
    public double complianceScore() {
        return scoreFor(ScoreCategory.COMPLIANCE);
    }
}
