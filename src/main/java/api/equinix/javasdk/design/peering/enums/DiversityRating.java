/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package api.equinix.javasdk.design.peering.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Rates the geographic diversity of failover paths for resiliency analysis.
 *
 * <p>Geographic diversity measures how independent two locations are in terms of
 * shared failure domains (power grids, fiber routes, weather corridors, seismic zones).
 * A backup in the same metro area provides minimal diversity, while a backup on a
 * different continent provides maximum diversity.</p>
 *
 * @author ianjones
 * @see api.equinix.javasdk.design.peering.model.DiversityScore
 * @see api.equinix.javasdk.design.peering.model.FailoverPath
 */
@Getter
@AllArgsConstructor
public enum DiversityRating {

    /** 3000&nbsp;km or more apart — typically cross-region/continental (e.g. Ashburn&ndash;Frankfurt). Score 1.0. */
    EXCELLENT("Excellent", 1.0, 3000),

    /** 1500&ndash;2999&nbsp;km apart — good diversity within a continent (e.g. Ashburn&ndash;Dallas). Score 0.75. */
    GOOD("Good", 0.75, 1500),

    /** 500&ndash;1499&nbsp;km apart — some shared infrastructure risk (e.g. Ashburn&ndash;Chicago). Score 0.5. */
    MODERATE("Moderate", 0.5, 500),

    /** 150&ndash;499&nbsp;km apart — limited diversity, likely shared corridors (e.g. Ashburn&ndash;New York). Score 0.25. */
    POOR("Poor", 0.25, 150),

    /** Under 150&nbsp;km apart — effectively the same failure domain (same-metro pairings). Score 0.0. */
    CRITICAL("Critical", 0.0, 0),

    /**
     * The distance between the two metros could not be determined (no facility or Fabric metro
     * coordinates were available for at least one of them). This is an <em>absence</em> of data, not a
     * proximity finding: it is never returned by {@link #fromDistance(double)} and must be excluded from
     * same-site detection and diversity scoring rather than treated as {@code 0 km} (which would falsely
     * read as CRITICAL "same-site"). Its {@link #score} is a placeholder and must not be averaged into a
     * resiliency score.
     */
    UNKNOWN("Unknown (distance unavailable)", 0.0, Integer.MIN_VALUE);

    private final String displayName;

    private final double score;

    private final int minDistanceKm;

    /**
     * Rates geographic diversity based on the distance between two metros.
     *
     * <p>Only ever returns one of the five distance-based ratings; {@link #UNKNOWN} is reserved for the
     * caller to assign when the distance itself is unavailable and is never produced here.</p>
     *
     * @param distanceKm the distance between primary and backup metros in kilometers
     * @return the diversity rating for the given distance
     */
    public static DiversityRating fromDistance(double distanceKm) {
        if (distanceKm >= EXCELLENT.minDistanceKm) return EXCELLENT;
        if (distanceKm >= GOOD.minDistanceKm) return GOOD;
        if (distanceKm >= MODERATE.minDistanceKm) return MODERATE;
        if (distanceKm >= POOR.minDistanceKm) return POOR;
        return CRITICAL;
    }
}
