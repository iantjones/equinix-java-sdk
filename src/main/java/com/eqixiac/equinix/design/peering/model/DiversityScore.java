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

package com.eqixiac.equinix.design.peering.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.peering.enums.DiversityRating;
import com.eqixiac.equinix.design.optimizer.context.LatencyFigure;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiversityScore {

    MetroId primaryMetro;

    MetroId backupMetro;

    /**
     * Great-circle distance between the two metros in kilometres, or {@code null} when it could not be
     * computed (coordinates missing for at least one metro). Unknown is an absent value, never {@code 0}
     * and never {@code NaN}, so it cannot be mistaken for a "same-site" 0&nbsp;km finding and it is simply
     * left out of JSON.
     */
    Double distanceKm;

    /**
     * {@code true} when the geographic distance could not be determined because at least one metro has
     * no known Equinix-facility or Fabric coordinate. Such a pair is honestly <em>unassessed</em>: it is
     * excluded from same-site detection and from the overall resiliency score, its {@link #rating} is
     * {@link DiversityRating#UNKNOWN}, and {@link #distanceKm} / {@link #rttFloorMs} are {@code null}.
     */
    boolean distanceUnavailable;

    /**
     * The straight-fibre speed-of-light round trip for {@link #distanceKm}, in ms: a physical lower bound.
     * {@code null} when the distance is unknown.
     */
    Double rttFloorMs;

    /**
     * The round trip between the two metros in ms, the way the Metro Optimizer states it: Fabric's measured
     * {@code connectedMetros.avgLatency} when Fabric publishes the pair, otherwise the floor times the
     * route factor fitted from the measured pairs ({@code RouteFactorCalibration}). {@code null} when
     * neither a measurement nor a distance is available. {@link #rttBasis} says which.
     */
    Double estimatedRttMs;

    /** How {@link #estimatedRttMs} was obtained; {@code null} when it is unknown. */
    LatencyFigure.Basis rttBasis;

    /** Detail for {@link #estimatedRttMs} (which pair was measured, the route factor used). */
    String rttNote;

    boolean sameRegion;

    DiversityRating rating;

    String explanation;
}
