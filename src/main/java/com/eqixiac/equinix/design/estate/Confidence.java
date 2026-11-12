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

package com.eqixiac.equinix.design.estate;

/** How sure the analyser is that an {@link EstateFinding} is right. */
public enum Confidence {
    /** Inferred from partial data or reference prices. */
    LOW(0.4),
    /** Based on live inventory, but with an assumption that matters (for example a reference price). */
    MEDIUM(0.7),
    /** Based on live inventory, measured utilisation and live or contract prices. */
    HIGH(1.0);

    private final double weight;

    Confidence(double weight) {
        this.weight = weight;
    }

    /**
     * The weight used when prioritising actions.
     *
     * @return 0.4 (LOW) to 1.0 (HIGH)
     */
    public double weight() {
        return weight;
    }

    /**
     * The lower of two confidences.
     *
     * @param other the other confidence
     * @return the lower one
     */
    public Confidence min(Confidence other) {
        return other == null || compareTo(other) <= 0 ? this : other;
    }
}
