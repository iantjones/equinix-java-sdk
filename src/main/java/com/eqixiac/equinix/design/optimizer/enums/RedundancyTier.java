package com.eqixiac.equinix.design.optimizer.enums;

import lombok.Getter;

/**
 * The least redundancy a design accepts. Each tier is an explicit, checkable rule on the SET of
 * metros a design uses (enforced by the {@code minimum_redundancy} constraint); no tier steers
 * selection any other way, and none silently forces a cross-region spread.
 *
 * <ul>
 *   <li>{@link #NONE}: one metro is enough.</li>
 *   <li>{@link #N_PLUS_1}: at least two metros, at least two of them in the same region (a nearby
 *       standby that fails over without crossing a region).</li>
 *   <li>{@link #MULTI_METRO}: at least two metros, in any regions.</li>
 *   <li>{@link #MULTI_REGION}: at least two metros in at least two different regions.</li>
 * </ul>
 */
@Getter
public enum RedundancyTier {

    /** No redundancy requirement: a single metro satisfies the tier. */
    NONE(1, 1, false, "One metro; no redundancy"),

    /** At least two metros, at least two of which share a region. */
    N_PLUS_1(2, 1, true, "At least two metros, two of them in the same region"),

    /** At least two metros in any regions. */
    MULTI_METRO(2, 1, false, "At least two metros, in any regions"),

    /** At least two metros spanning at least two regions. */
    MULTI_REGION(2, 2, false, "At least two metros spanning at least two regions");

    /** Fewest metros the tier needs. */
    private final int minimumMetros;
    /** Fewest distinct regions the tier needs. */
    private final int minimumRegions;
    /** Whether two of the metros must share a region. */
    private final boolean sameRegionPairRequired;
    /** Human-readable statement of the rule. */
    private final String description;

    RedundancyTier(int minimumMetros, int minimumRegions, boolean sameRegionPairRequired, String description) {
        this.minimumMetros = minimumMetros;
        this.minimumRegions = minimumRegions;
        this.sameRegionPairRequired = sameRegionPairRequired;
        this.description = description;
    }
}
