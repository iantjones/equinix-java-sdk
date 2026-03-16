package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.fabric.optimizer.enums.ScoreCategory;
import lombok.Value;

/**
 * A single dimension of a metro's composite score.
 */
@Value
public class ScoreComponent {

    ScoreCategory category;
    double score;
    double weight;
    String explanation;

    /**
     * The weighted contribution of this component to the composite score.
     */
    public double weightedScore() {
        return score * weight;
    }
}
