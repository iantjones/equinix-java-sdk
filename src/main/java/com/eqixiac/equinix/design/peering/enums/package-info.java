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

/**
 * Classification enums for Peering Intelligence:
 * {@link com.eqixiac.equinix.design.peering.enums.ConnectivityType} (how an ASN is reachable at
 * a metro — IX, Fabric, both, facility-only, none),
 * {@link com.eqixiac.equinix.design.peering.enums.PeeringPolicy} and
 * {@link com.eqixiac.equinix.design.peering.enums.NetworkType} (parsed from PeeringDB's
 * {@code policy_general} / {@code info_type} fields),
 * {@link com.eqixiac.equinix.design.peering.enums.DiversityRating} (distance-banded geographic
 * diversity, with {@code UNKNOWN} reserved for unavailable distances), and
 * {@link com.eqixiac.equinix.design.peering.enums.FailureScope} (blast-radius failure domains).
 *
 * @see com.eqixiac.equinix.design.peering.PeeringIntelligence
 */
package com.eqixiac.equinix.design.peering.enums;
