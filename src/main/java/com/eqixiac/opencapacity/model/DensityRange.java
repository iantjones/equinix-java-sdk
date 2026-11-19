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

import java.util.Objects;

/**
 * Power per cabinet as a minimum, a target, and a maximum. Any subset is valid and at least one is
 * present. Where more than one is present, min is not greater than target and target is not greater
 * than max.
 *
 * @param min the minimum, or {@code null}
 * @param target the target, or {@code null}
 * @param max the maximum, or {@code null}
 * @param unit kW or kVA per cabinet
 */
public record DensityRange(Double min, Double target, Double max, DensityUnit unit) {

    /**
     * Creates a density range.
     *
     * @throws IllegalArgumentException if all three values are absent, a value is not positive, or
     *     the values are out of order
     */
    public DensityRange {
        Objects.requireNonNull(unit, "unit");
        if (min == null && target == null && max == null) {
            throw new IllegalArgumentException("A density range needs at least one of min, target and max");
        }
        double previous = 0;
        for (Double value : new Double[] {min, target, max}) {
            if (value != null) {
                if (!(value > 0) || value < previous) {
                    throw new IllegalArgumentException("Density values must be positive and ordered min, target, max");
                }
                previous = value;
            }
        }
    }

    /**
     * Starts a range in kVA per cabinet.
     *
     * @return a builder
     */
    public static Builder kva() {
        return new Builder(DensityUnit.KVA);
    }

    /**
     * Starts a range in kW per cabinet.
     *
     * @return a builder
     */
    public static Builder kw() {
        return new Builder(DensityUnit.KW);
    }

    /**
     * Returns the value that stands for the range when one number is needed: the target, or the
     * max when there is no target, or the min when only the min is given.
     *
     * @return the representative density
     */
    public double representative() {
        return target != null ? target : max != null ? max : min;
    }

    /** Builds a {@link DensityRange}. */
    public static final class Builder {
        private final DensityUnit unit;
        private Double min;
        private Double target;
        private Double max;

        private Builder(DensityUnit unit) {
            this.unit = unit;
        }

        /**
         * Sets the minimum.
         *
         * @param value power per cabinet
         * @return this builder
         */
        public Builder min(double value) {
            this.min = value;
            return this;
        }

        /**
         * Sets the target.
         *
         * @param value power per cabinet
         * @return this builder
         */
        public Builder target(double value) {
            this.target = value;
            return this;
        }

        /**
         * Sets the maximum.
         *
         * @param value power per cabinet
         * @return this builder
         */
        public Builder max(double value) {
            this.max = value;
            return this;
        }

        /**
         * Creates the range.
         *
         * @return the range
         * @throws IllegalArgumentException if the values are invalid
         */
        public DensityRange create() {
            return new DensityRange(min, target, max, unit);
        }
    }
}
