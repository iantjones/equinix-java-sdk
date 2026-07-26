package api.equinix.javasdk.design.optimizer.enums;

import lombok.Getter;

/**
 * Redundancy requirements for metro placement. Higher tiers provide greater
 * geographic diversity and fault isolation at increased cost.
 *
 * <p>{@code minimumMetros} is the tier's <em>floor</em>, not the recommendation size: when no
 * explicit {@code maxMetros} constraint is set, the engine recommends up to
 * {@code max(3, minimumMetros + 1)} metros regardless of tier — so an intentionally single-metro
 * deployment needs {@code maxMetros(1)} in addition to (or instead of) {@code NONE}. The
 * {@code MULTI_METRO} and {@code MULTI_REGION} tiers additionally switch selection to
 * region-diversity-aware round-robin (best metro per region), so geographic spread is a hard
 * outcome rather than a scoring nudge; a selected set that still falls short of the tier raises a
 * {@code REDUNDANCY_GAP} risk finding.</p>
 *
 * @see api.equinix.javasdk.design.optimizer.model.OptimizationConstraints
 */
@Getter
public enum RedundancyTier {

    /** No redundancy requirement: a single metro satisfies the tier. */
    NONE(1, "Single metro, no redundancy"),

    /** At least two metros; the same region is acceptable (component-level, not geographic, redundancy). */
    N_PLUS_1(2, "Two metros in the same region"),

    /** At least two metros with region-diversity-aware selection across regions. */
    MULTI_METRO(2, "Two or more metros across regions"),

    /** At least three metros spanning multiple regions, selected region-diversity-aware. */
    MULTI_REGION(3, "Three or more metros spanning multiple regions");

    private final int minimumMetros;
    private final String description;

    RedundancyTier(int minimumMetros, String description) {
        this.minimumMetros = minimumMetros;
        this.description = description;
    }
}
