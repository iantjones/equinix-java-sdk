/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * A feature name from the OpenCapacity feature registry, or a provider extension that starts with
 * {@code X_}. The registry is open: a name without a constant here is still a valid feature.
 *
 * @param name the wire name, upper case with underscores
 */
public record Feature(@JsonValue String name) {

    /** Any liquid cooling method. */
    public static final Feature LIQUID_COOLING = new Feature("LIQUID_COOLING");
    /** Direct-to-chip liquid cooling. */
    public static final Feature DIRECT_TO_CHIP_COOLING = new Feature("DIRECT_TO_CHIP_COOLING");
    /** Immersion cooling. */
    public static final Feature IMMERSION_COOLING = new Feature("IMMERSION_COOLING");
    /** Rear-door heat exchangers. */
    public static final Feature REAR_DOOR_HEAT_EXCHANGER = new Feature("REAR_DOOR_HEAT_EXCHANGER");
    /** A private cage. */
    public static final Feature PRIVATE_CAGE = new Feature("PRIVATE_CAGE");
    /** A private suite. */
    public static final Feature PRIVATE_SUITE = new Feature("PRIVATE_SUITE");
    /** Floor loading above the standard for the site. */
    public static final Feature HIGH_FLOOR_LOADING = new Feature("HIGH_FLOOR_LOADING");
    /** Energy use matched with renewable energy. */
    public static final Feature RENEWABLE_ENERGY_MATCHED = new Feature("RENEWABLE_ENERGY_MATCHED");
    /** A cloud on-ramp at the location. */
    public static final Feature CLOUD_ON_RAMP = new Feature("CLOUD_ON_RAMP");
    /** An Internet Exchange (IX) at the location. */
    public static final Feature INTERNET_EXCHANGE = new Feature("INTERNET_EXCHANGE");
    /** Remote hands service. */
    public static final Feature REMOTE_HANDS = new Feature("REMOTE_HANDS");
    /** An option to expand later. */
    public static final Feature EXPANSION_OPTION = new Feature("EXPANSION_OPTION");
    /** A facility built for the customer. */
    public static final Feature BUILD_TO_SUIT = new Feature("BUILD_TO_SUIT");

    /**
     * Creates a feature.
     *
     * @param name the wire name
     * @throws IllegalArgumentException if the name does not match the registry pattern
     */
    public Feature {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new IllegalArgumentException("Feature name must match [A-Z][A-Z0-9_]{1,63}: " + name);
        }
    }

    /**
     * Returns the feature with a wire name.
     *
     * @param name the wire name
     * @return the feature
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Feature of(String name) {
        return new Feature(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
