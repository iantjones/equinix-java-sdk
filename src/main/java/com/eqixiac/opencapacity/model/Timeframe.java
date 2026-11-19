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

import java.util.List;

/**
 * When capacity is needed. Every component is optional. A requirement with no
 * {@code neededForService} states an immediate need.
 *
 * @param neededForSale when the customer needs to be able to contract, or {@code null}
 * @param neededForService when the capacity must be usable, or {@code null} for now
 * @param latestAcceptable the last service date that still works, or {@code null}
 * @param termMonths the intended length of the commitment in months, or {@code null}
 * @param ramp the phased need in date order, empty for the full demand at once
 */
public record Timeframe(
        FlexibleDate neededForSale, FlexibleDate neededForService, FlexibleDate latestAcceptable,
        Integer termMonths, List<RampStep> ramp) {

    /** Creates a timeframe. */
    public Timeframe {
        ramp = ramp == null ? List.of() : List.copyOf(ramp);
    }
}
