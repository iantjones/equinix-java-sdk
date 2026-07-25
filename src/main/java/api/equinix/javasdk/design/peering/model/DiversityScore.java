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

package api.equinix.javasdk.design.peering.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.peering.enums.DiversityRating;
import lombok.Builder;
import lombok.Value;

/**
 * Geographic diversity assessment between a primary and backup metro.
 *
 * <p>Evaluates how independent two Equinix metros are in terms of shared failure
 * domains. A higher diversity score indicates greater independence from correlated
 * failures such as regional power grid outages, shared fiber routes, natural disasters,
 * or political instability affecting a geographic area.</p>
 *
 * @author ianjones
 * @see DiversityRating
 * @see FailoverPath
 */
@Value
@Builder
public class DiversityScore {

    MetroId primaryMetro;

    MetroId backupMetro;

    /**
     * Great-circle distance between the two metros in kilometers.
     *
     * <p><b>Only meaningful when {@link #distanceUnavailable} is {@code false}.</b> When the distance
     * could not be computed (coordinates missing for at least one metro) this is {@link Double#NaN} — a
     * deliberate non-value, never {@code 0}, so it can never be mistaken for a "same-site" 0&nbsp;km
     * proximity finding.</p>
     */
    double distanceKm;

    /**
     * {@code true} when the geographic distance could not be determined because at least one metro has
     * no known Equinix-facility or Fabric coordinate. Such a pair is honestly <em>unassessed</em>: it is
     * excluded from same-site detection and from the overall resiliency score, its {@link #rating} is
     * {@link DiversityRating#UNKNOWN}, and {@link #distanceKm}/{@link #estimatedRttMs} are
     * {@link Double#NaN} rather than a fabricated {@code 0}.
     */
    boolean distanceUnavailable;

    /**
     * Estimated round-trip fibre latency between the two metros, in milliseconds — the
     * {@linkplain api.equinix.javasdk.design.geo.SpeedOfLightLatency speed-of-light floor} for the
     * {@link #distanceKm} great-circle distance. A physical lower bound; real latency is higher
     * (switching, queuing, non-direct fibre).
     */
    double estimatedRttMs;

    boolean sameRegion;

    DiversityRating rating;

    String explanation;
}
