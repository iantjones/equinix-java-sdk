package api.equinix.javasdk.design.optimizer.enums;

/**
 * Categories used to decompose a metro's composite score into
 * individually inspectable dimensions.
 *
 * @see api.equinix.javasdk.design.optimizer.model.MetroScore
 * @see api.equinix.javasdk.design.optimizer.model.ScoreComponent
 */
public enum ScoreCategory {

    LATENCY,
    PROVIDER_COVERAGE,
    COST,
    REDUNDANCY,
    COMPLIANCE
}
