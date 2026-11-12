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

/** Whether a smaller or a larger {@link Objective} value is better. */
public enum Direction {
    /** Smaller is better (latency, cost). */
    MINIMIZE,
    /** Larger is better (coverage, resilience). */
    MAXIMIZE;

    /**
     * Compares two values of an objective with this direction.
     *
     * @param a one value
     * @param b another value
     * @return negative when {@code a} is better, positive when {@code b} is better, zero when equal
     */
    public int compare(double a, double b) {
        return this == MINIMIZE ? Double.compare(a, b) : Double.compare(b, a);
    }
}
