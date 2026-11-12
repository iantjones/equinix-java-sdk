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
package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.DesignPlugin;
import com.eqixiac.equinix.design.spi.Objective;

import java.util.List;

/**
 * Contributes the simulation-based resilience plug-ins to the Metro Optimizer:
 * <ul>
 *   <li>the {@link SimulatedResilienceObjective} ({@value SimulatedResilienceObjective#ID}), simulated only when
 *       the spec weights it;</li>
 *   <li>the {@link MinSurvivalConstraint} ({@value MinSurvivalConstraint#ID}), inert unless a threshold is given
 *       here, by the spec's {@code constraints.minSurvivalPct}, or by the spec label
 *       {@value MinSurvivalConstraint#LABEL} (the field wins over the label);</li>
 *   <li>the {@link FailoverGeoZonesConstraint} ({@value FailoverGeoZonesConstraint#ID}), inert unless the spec
 *       states geo zones.</li>
 * </ul>
 * <p>Registered for {@link DesignPlugin#discover()}; or pass one explicitly:
 * {@code MetroOptimizer.forSpec(fabric, spec).plugin(new ResiliencePlugin(options, 75.0)).run()}.</p>
 */
public final class ResiliencePlugin implements DesignPlugin {

    private final ResilienceOptions options;
    private final Double minSurvivalPct;

    /** Default options; the survival threshold, if any, comes from the spec (field, else label). */
    public ResiliencePlugin() {
        this(ResilienceOptions.defaults(), null);
    }

    /**
     * @param options        graph and simulation options
     * @param minSurvivalPct the least share of service that must survive the worst single failure, 0..100, or
     *                       {@code null} to read the spec ({@code constraints.minSurvivalPct}, else the label)
     */
    public ResiliencePlugin(ResilienceOptions options, Double minSurvivalPct) {
        this.options = options == null ? ResilienceOptions.defaults() : options;
        this.minSurvivalPct = minSurvivalPct;
    }

    @Override
    public String name() {
        return "resilience";
    }

    @Override
    public List<Objective> objectives() {
        return List.of(new SimulatedResilienceObjective(options, true));
    }

    @Override
    public List<Constraint> constraints() {
        return List.of(new MinSurvivalConstraint(minSurvivalPct, options), new FailoverGeoZonesConstraint(options));
    }
}
