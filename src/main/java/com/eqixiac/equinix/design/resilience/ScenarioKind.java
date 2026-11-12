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
package com.eqixiac.equinix.design.resilience;

/** What a {@link FailureScenario} takes away. */
public enum ScenarioKind {
    /** Nothing: the design as built. */
    BASELINE(false),
    /** A whole metro: its facilities, Cloud Router and on-ramps. */
    METRO(true),
    /** One cloud on-ramp (a provider's presence) in one metro. */
    CLOUD_ONRAMP(true),
    /** One connection: a cloud connection, a site access circuit or a backbone link. */
    CONNECTION(true),
    /** One Cloud Router. */
    CLOUD_ROUTER(true),
    /** A cloud provider region (every on-ramp of the clouds in it becomes useless). Shared fate: not counted in N-1. */
    CLOUD_REGION(false),
    /** An entire Equinix region (AMER, EMEA or APAC). Shared fate: not counted in N-1. */
    EQUINIX_REGION(false),
    /** Two single-element failures at once (N-2). */
    PAIR(false);

    private final boolean singleElement;

    ScenarioKind(boolean singleElement) {
        this.singleElement = singleElement;
    }

    /** @return {@code true} for the single-element failures that make up the N-1 set */
    public boolean isSingleElement() {
        return singleElement;
    }
}
