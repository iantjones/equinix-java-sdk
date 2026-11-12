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

/**
 * What an {@link Objective} measured for one portfolio.
 *
 * @param value        the measurement in the objective's unit; {@link Double#NaN} when {@code known} is false
 * @param known        {@code false} when the objective could not be measured (missing data), which is
 *                     reported as unknown and never treated as a good or a bad value
 * @param explanation  how the value was reached, or why it is unknown; shown to the reader
 */
public record ObjectiveValue(double value, boolean known, String explanation) {

    /** Rejects a known value that is not a finite number. */
    public ObjectiveValue {
        if (known && !Double.isFinite(value)) {
            throw new IllegalArgumentException("A known objective value must be finite, got " + value);
        }
        if (!known) {
            value = Double.NaN;
        }
    }

    /**
     * @param value        the measurement
     * @param explanation  how it was reached
     * @return a known value
     */
    public static ObjectiveValue of(double value, String explanation) {
        return new ObjectiveValue(value, true, explanation);
    }

    /**
     * @param reason why the objective could not be measured
     * @return an unknown value
     */
    public static ObjectiveValue unknown(String reason) {
        return new ObjectiveValue(Double.NaN, false, reason);
    }
}
