package api.equinix.javasdk.fabric.optimizer.enums;

/**
 * Categories used to decompose a metro's composite score into
 * individually inspectable dimensions.
 *
 * @see api.equinix.javasdk.fabric.optimizer.model.MetroScore
 * @see api.equinix.javasdk.fabric.optimizer.model.ScoreComponent
 */
public enum ScoreCategory {

    LATENCY,
    PROVIDER_COVERAGE,
    COST,
    REDUNDANCY,
    COMPLIANCE
}
