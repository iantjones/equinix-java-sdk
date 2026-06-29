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

import api.equinix.javasdk.core.enums.MetroCode;
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

    MetroCode primaryMetro;

    MetroCode backupMetro;

    double distanceKm;

    boolean sameRegion;

    DiversityRating rating;

    String explanation;
}
