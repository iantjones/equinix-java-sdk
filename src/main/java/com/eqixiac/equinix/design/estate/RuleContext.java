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

import java.time.Instant;
import java.util.Objects;

/**
 * What a rule may use besides the inventory and utilisation: pricing, thresholds, and the time the
 * analysis treats as "now" (the snapshot's as-of time, so offline re-runs give the same result).
 *
 * @param pricing    the pricing (possibly {@link EstatePricing#unavailable()})
 * @param thresholds the thresholds
 * @param asOf       the reference time for ages
 */
public record RuleContext(EstatePricing pricing, EstateThresholds thresholds, Instant asOf) {

    /** Validates the fields. */
    public RuleContext {
        Objects.requireNonNull(pricing, "pricing");
        Objects.requireNonNull(thresholds, "thresholds");
        Objects.requireNonNull(asOf, "asOf");
    }

    /**
     * The age of something created at {@code createdAt}, in whole days.
     *
     * @param createdAt the creation time, or {@code null}
     * @return the age in days, or {@code -1} when unknown
     */
    public long ageDays(Instant createdAt) {
        return createdAt == null ? -1 : java.time.Duration.between(createdAt, asOf).toDays();
    }
}
