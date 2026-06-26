package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.design.optimizer.enums.ScoreCategory;
import lombok.Value;

import java.util.List;

/**
 * Composite score for a metro recommendation, decomposed into individually
 * inspectable {@link ScoreComponent} dimensions.
 */
@Value
public class MetroScore {

    double composite;
    List<ScoreComponent> components;

    public double scoreFor(ScoreCategory category) {
        return components.stream()
                .filter(c -> c.getCategory() == category)
                .findFirst()
                .map(ScoreComponent::getScore)
                .orElse(0.0);
    }

    public double latencyScore() {
        return scoreFor(ScoreCategory.LATENCY);
    }

    public double providerScore() {
        return scoreFor(ScoreCategory.PROVIDER_COVERAGE);
    }

    public double costScore() {
        return scoreFor(ScoreCategory.COST);
    }

    public double redundancyScore() {
        return scoreFor(ScoreCategory.REDUNDANCY);
    }

    public double complianceScore() {
        return scoreFor(ScoreCategory.COMPLIANCE);
    }
}
