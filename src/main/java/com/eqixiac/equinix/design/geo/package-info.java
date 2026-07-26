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
 * Latency physics: {@link com.eqixiac.equinix.design.geo.SpeedOfLightLatency} computes
 * speed-of-light-in-fibre latency floors (one-way or round-trip) and haversine great-circle
 * distances between IBXes, metros, or raw coordinates — fully offline once the endpoints'
 * coordinates are known.
 *
 * <p>The figures are physical lower bounds (configurable fibre refractive index and route
 * factor), used by the peering resiliency analysis for diversity RTT floors and available
 * directly for architecture reviews: compare the computed floor with Equinix's measured
 * inter-metro {@code avgLatency} to see what the network delivers versus what physics allows.</p>
 *
 * @see com.eqixiac.equinix.design.geo.SpeedOfLightLatency
 */
package com.eqixiac.equinix.design.geo;
