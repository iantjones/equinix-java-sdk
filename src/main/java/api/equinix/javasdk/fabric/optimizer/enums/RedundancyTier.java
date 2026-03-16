package api.equinix.javasdk.fabric.optimizer.enums;

import lombok.Getter;

/**
 * Redundancy requirements for metro placement. Higher tiers provide greater
 * geographic diversity and fault isolation at increased cost.
 *
 * @see api.equinix.javasdk.fabric.optimizer.model.OptimizationConstraints
 */
@Getter
public enum RedundancyTier {

    NONE(1, "Single metro, no redundancy"),
    N_PLUS_1(2, "Two metros in the same region"),
    MULTI_METRO(2, "Two or more metros across regions"),
    MULTI_REGION(3, "Three or more metros spanning multiple regions");

    private final int minimumMetros;
    private final String description;

    RedundancyTier(int minimumMetros, String description) {
        this.minimumMetros = minimumMetros;
        this.description = description;
    }
}
