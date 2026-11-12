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

/** How urgent an {@link EstateFinding} is. Ordered from least to most severe. */
public enum Severity {
    /** Worth knowing; no action needed soon. */
    INFO(1),
    /** A modest improvement. */
    LOW(2),
    /** A clear improvement or a moderate risk. */
    MEDIUM(3),
    /** A large saving or a material risk. */
    HIGH(4),
    /** An outage risk or a resource near its hard limit. */
    CRITICAL(5);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    /**
     * The weight used when prioritising actions.
     *
     * @return 1 (INFO) to 5 (CRITICAL)
     */
    public int weight() {
        return weight;
    }
}
