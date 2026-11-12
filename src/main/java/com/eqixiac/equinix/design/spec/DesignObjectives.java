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

package com.eqixiac.equinix.design.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What "better" means for this design: a weight per objective id. Weights are relative (they are
 * normalised to sum to 1) and only objectives with a positive weight influence the recommended
 * option; every evaluated objective is still reported so trade-offs are visible.
 *
 * <p>Built-in objective ids are the constants on {@code com.eqixiac.equinix.design.spi.Objective}
 * ({@code latency}, {@code cost}, {@code provider_coverage}, {@code resilience}, {@code compliance});
 * a plug-in objective is weighted by its own id (for example {@code carbon}).</p>
 *
 * @param profile  a named weight profile to start from (for example {@code balanced}, {@code latency_first},
 *                 {@code cost_first}, {@code redundancy_first}, {@code provider_coverage_first}); may be {@code null}
 * @param weights  explicit weights by objective id, applied on top of {@code profile}; never {@code null}
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DesignObjectives(String profile, Map<String, Double> weights) {

    /** Normalises {@code weights} to an immutable, non-null, insertion-ordered map. */
    public DesignObjectives {
        weights = weights == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(weights));
    }

    /** @return the {@code balanced} profile with no overrides */
    public static DesignObjectives balanced() {
        return new DesignObjectives("balanced", null);
    }
}
