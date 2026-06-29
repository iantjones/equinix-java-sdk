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

    EXCELLENT("Excellent", 1.0, 3000),

    GOOD("Good", 0.75, 1500),

    MODERATE("Moderate", 0.5, 500),

    POOR("Poor", 0.25, 150),

    CRITICAL("Critical", 0.0, 0);

    private final String displayName;

    private final double score;

    private final int minDistanceKm;

    /**
     * Rates geographic diversity based on the distance between two metros.
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
