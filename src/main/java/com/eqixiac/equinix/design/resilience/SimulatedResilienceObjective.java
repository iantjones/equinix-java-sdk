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

import com.eqixiac.equinix.design.spi.ValueScale;
import com.eqixiac.equinix.design.solve.WeightProfiles;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;

/**
 * {@value #ID}: the share of a portfolio's baseline service that survives its worst single-element
 * failure, 0 to 100, measured by building the design graph and simulating every metro, Cloud Router,
 * on-ramp and connection loss ({@link N1Survival}). A workload's service is the site weight that still
 * reaches it within its limits times the share of its bandwidth its cloud connections still carry, so
 * "survives at 40% capacity" scores 40, not 100. Larger is better.
 *
 * <p>Richer than the built-in {@code resilience} objective (which only removes metros and counts
 * what is left): it sees Cloud Routers, on-ramps, single connections, capacity and failover latency.
 * Contributed by {@link ResiliencePlugin}; weight it in a spec with
 * {@code objectives.weights.simulated_resilience}.</p>
 */
public final class SimulatedResilienceObjective implements Objective {

    /** The objective id. */
    public static final String ID = "simulated_resilience";

    private final ResilienceOptions options;
    private final boolean onlyWhenWeighted;

    /** Always evaluates, with the default options. */
    public SimulatedResilienceObjective() {
        this(ResilienceOptions.defaults(), false);
    }

    /**
     * @param options          graph and simulation options
     * @param onlyWhenWeighted when {@code true}, a spec that gives {@value #ID} no positive weight (after its
     *                         profile is applied) gets an unknown value without simulating, which keeps a large
     *                         search fast; the explanation says so
     */
    public SimulatedResilienceObjective(ResilienceOptions options, boolean onlyWhenWeighted) {
        this.options = options == null ? ResilienceOptions.defaults() : options;
        this.onlyWhenWeighted = onlyWhenWeighted;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Direction direction() {
        return Direction.MAXIMIZE;
    }

    /** A percentage is its own score. */
    @Override
    public ValueScale scale(DesignContext context) {
        return ValueScale.percent();
    }

    @Override
    public String unit() {
        return "%";
    }

    @Override
    public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
        if (onlyWhenWeighted) {
            Double w = WeightProfiles.resolve(context.spec().objectives()).get(ID);
            if (w == null || w <= 0) {
                return ObjectiveValue.unknown("not simulated: the spec gives '" + ID + "' no weight");
            }
        }
        N1Survival n1 = N1Survival.of(portfolio, context, options);
        if (!n1.known()) {
            return ObjectiveValue.unknown(n1.reason());
        }
        return ObjectiveValue.of(100 * n1.worstSurvival(), n1.reason() + " over " + n1.scenarios()
                + " single-element failures");
    }
}
