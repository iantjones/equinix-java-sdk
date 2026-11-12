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

package com.eqixiac.equinix.design.spi;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * The read-only facts of one design run. An engine gathers them once (from the live Fabric
 * catalogue, the rate cards and the spec) and hands the same context to every plug-in, so plug-ins
 * never perform I/O and a run is reproducible from its context.
 *
 * <p>All latencies are <strong>round-trip</strong> milliseconds, the unit of Fabric's measured
 * {@code connectedMetros.avgLatency}. An empty result means unknown, never zero.</p>
 */
public interface DesignContext {

    /** @return the design being evaluated */
    DesignSpec spec();

    /** @return every metro the catalogue returned, in a stable order (by code) */
    List<MetroFacts> metros();

    /**
     * @param metro a metro
     * @return its facts, or empty when the catalogue does not list it
     */
    Optional<MetroFacts> metro(MetroId metro);

    /**
     * Round-trip latency between a metro and a site: Fabric's measurement when the site names a
     * metro and the pair is measured, otherwise an estimate from distance, otherwise unknown.
     *
     * @param metro a metro
     * @param site  a site of the spec
     * @return the latency in ms, or empty when it is unknown
     */
    OptionalDouble roundTripLatencyMs(MetroId metro, SiteIntent site);

    /**
     * @param metro a metro
     * @param site  a site of the spec
     * @return {@code true} when {@link #roundTripLatencyMs(MetroId, SiteIntent)} is a Fabric measurement,
     *         {@code false} when it is an estimate or unknown
     */
    boolean latencyMeasured(MetroId metro, SiteIntent site);

    /**
     * Round-trip latency between two metros, measured where Fabric publishes it and estimated otherwise.
     *
     * @param a one metro
     * @param b the other metro
     * @return the latency in ms, or empty when it is unknown
     */
    OptionalDouble roundTripLatencyMs(MetroId a, MetroId b);

    /**
     * @param site a site of the spec
     * @return the site's share of the total site weight, between 0 and 1 (weights sum to 1 across sites)
     */
    double siteWeight(SiteIntent site);

    /**
     * @param metro a metro
     * @return the ids of the spec's {@code CloudIntent}s that can be reached privately from the metro
     */
    Set<String> reachableClouds(MetroId metro);

    /** @return the cost model that prices portfolios in this run */
    CostModel costModel();

    /** @return when the facts were gathered */
    Instant asOf();
}
