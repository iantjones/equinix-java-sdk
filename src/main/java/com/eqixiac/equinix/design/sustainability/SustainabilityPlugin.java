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
package com.eqixiac.equinix.design.sustainability;

import com.eqixiac.equinix.design.spi.DesignPlugin;
import com.eqixiac.equinix.design.spi.Objective;

import java.util.List;

/**
 * Contributes the {@link CarbonObjective} ({@value CarbonObjective#ID}).
 *
 * <p><strong>Beta.</strong> May change in a minor release.</p>
 *
 * <p>Discovered through {@link DesignPlugin#discover()}, it uses the bundled reference table
 * ({@link CarbonIntensityTable#bundledReference()}) and measures only when the spec gives
 * {@code carbon} a positive weight; otherwise the objective is unknown for every portfolio. To use your
 * own figures, pass a table:
 * {@code MetroOptimizer.forSpec(fabric, spec).plugin(new SustainabilityPlugin(table)).run()}.</p>
 */
public final class SustainabilityPlugin implements DesignPlugin {

    private final CarbonIntensityTable table;
    private final boolean onlyWhenWeighted;

    /** The bundled reference table, measured only when the spec weights {@code carbon}. */
    public SustainabilityPlugin() {
        this.table = CarbonIntensityTable.bundledReference();
        this.onlyWhenWeighted = true;
    }

    /**
     * @param table the caller's intensity table, or {@code null} for an inert plug-in
     */
    public SustainabilityPlugin(CarbonIntensityTable table) {
        this.table = table;
        this.onlyWhenWeighted = false;
    }

    @Override
    public String name() {
        return "sustainability";
    }

    @Override
    public List<Objective> objectives() {
        return List.of(new CarbonObjective(table, onlyWhenWeighted));
    }
}
