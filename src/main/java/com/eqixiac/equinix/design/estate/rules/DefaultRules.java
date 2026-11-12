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

package com.eqixiac.equinix.design.estate.rules;

import com.eqixiac.equinix.design.estate.EstateRule;

import java.util.List;

/** The rules an analysis runs when the caller supplies none. */
public final class DefaultRules {

    private DefaultRules() {
    }

    /**
     * Fresh instances of every default rule, in a fixed order.
     *
     * @return right-size, on-demand-to-term, consolidate-behind-router, idle-or-unused,
     *         single-point-of-failure, capacity-risk, hygiene
     */
    public static List<EstateRule> all() {
        return List.of(new RightSizeRule(), new OnDemandToTermRule(), new ConsolidateBehindRouterRule(),
                new IdleResourceRule(), new SinglePointOfFailureRule(), new CapacityRiskRule(), new HygieneRule());
    }
}
