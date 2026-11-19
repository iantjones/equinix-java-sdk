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
 * An amount of power.
 *
 * @param value the amount, greater than zero
 * @param unit the unit
 */
public record Power(double value, PowerUnit unit) {

    /**
     * Creates an amount of power.
     *
     * @throws IllegalArgumentException if the value is not greater than zero
     */
    public Power {
        Objects.requireNonNull(unit, "unit");
        if (!(value > 0)) {
            throw new IllegalArgumentException("Power must be greater than zero: " + value);
        }
    }

    /**
     * Returns an amount in kW.
     *
     * @param value the amount
     * @return the power
     */
    public static Power kw(double value) {
        return new Power(value, PowerUnit.KW);
    }

    /**
     * Returns an amount in MW.
     *
     * @param value the amount
     * @return the power
     */
    public static Power mw(double value) {
        return new Power(value, PowerUnit.MW);
    }

    /**
     * Returns an amount in kVA.
     *
     * @param value the amount
     * @return the power
     */
    public static Power kva(double value) {
        return new Power(value, PowerUnit.KVA);
    }

    /**
     * Returns an amount in MVA.
     *
     * @param value the amount
     * @return the power
     */
    public static Power mva(double value) {
        return new Power(value, PowerUnit.MVA);
    }

    /**
     * Converts to kW, or to kVA for an apparent-power unit, without applying a power factor.
     *
     * @return the amount in thousands of the base unit
     */
    public double inKilo() {
        return unit == PowerUnit.MW || unit == PowerUnit.MVA ? value * 1000 : value;
    }
}
