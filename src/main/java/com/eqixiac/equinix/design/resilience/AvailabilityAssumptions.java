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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The availability assumed for each kind of element, as a fraction (0.9995 = 99.95%).
 *
 * <p><strong>These are assumptions, not guarantees.</strong> Equinix publishes service levels per
 * product (Fabric connections, Fabric Cloud Router, Network Edge, colocation) in its product terms,
 * and each cloud provider publishes its own; the figure that applies to a design is the one in the
 * contracts that design is bought under. {@link #defaults()} holds round planning figures chosen by
 * this SDK so a report can show how redundancy changes the result; they are not Equinix or provider
 * SLA figures and must be replaced with contractual figures before an availability number is quoted
 * to anyone. Every report lists the values it used and their {@link #source()}.</p>
 *
 * @param availability fraction per element type, each in (0, 1]; a missing type is taken from {@link #defaults()}
 * @param source       where the figures came from, shown in every report
 */
public record AvailabilityAssumptions(Map<ElementType, Double> availability, String source) {

    /** The {@link #source()} of {@link #defaults()}. */
    public static final String DEFAULT_SOURCE = "SDK planning assumptions (round figures chosen 2026-11-12 for "
            + "comparison only; NOT Equinix or cloud-provider SLAs: Equinix publishes SLAs per product, replace "
            + "these with your contractual figures)";

    /** Validates every figure and fills missing element types from the defaults. */
    public AvailabilityAssumptions {
        Map<ElementType, Double> filled = new EnumMap<>(ElementType.class);
        filled.putAll(DEFAULTS);
        if (availability != null) {
            for (Map.Entry<ElementType, Double> e : availability.entrySet()) {
                Double v = e.getValue();
                if (e.getKey() == null || v == null || !(v > 0 && v <= 1)) {
                    throw new IllegalArgumentException("Availability for " + e.getKey() + " must be in (0, 1], got " + v);
                }
                filled.put(e.getKey(), v);
            }
        }
        availability = Collections.unmodifiableMap(filled);
        source = source == null || source.isBlank() ? "caller-supplied" : source;
    }

    private static final Map<ElementType, Double> DEFAULTS;

    static {
        Map<ElementType, Double> d = new EnumMap<>(ElementType.class);
        d.put(ElementType.METRO, 0.9999);
        d.put(ElementType.CLOUD_ROUTER, 0.9999);
        d.put(ElementType.CLOUD_CONNECTION, 0.9995);
        d.put(ElementType.CLOUD_ONRAMP, 0.9995);
        d.put(ElementType.BACKBONE_LINK, 0.9999);
        d.put(ElementType.SITE_ACCESS, 0.999);
        d.put(ElementType.CLOUD_REGION, 0.9999);
        DEFAULTS = Collections.unmodifiableMap(d);
    }

    /** @return the SDK's planning assumptions (see the class description: not SLAs) */
    public static AvailabilityAssumptions defaults() {
        return new AvailabilityAssumptions(null, DEFAULT_SOURCE);
    }

    /**
     * @param type an element type
     * @return its assumed availability
     */
    public double of(ElementType type) {
        return availability.get(type);
    }
}
