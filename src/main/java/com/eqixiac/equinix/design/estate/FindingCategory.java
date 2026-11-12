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

/** What kind of improvement an {@link EstateFinding} proposes. */
public enum FindingCategory {
    /** Provisioned bandwidth far above measured use: reduce it. */
    RIGHT_SIZE_DOWN,
    /** Measured use close to or above provisioned bandwidth: increase it. */
    RIGHT_SIZE_UP,
    /** A long-lived on-demand resource that would cost less on a 12, 24 or 36-month term. */
    ON_DEMAND_TO_TERM,
    /** Several cloud connections in one metro a Cloud Router would serve, or a router that serves too few. */
    CONSOLIDATE_BEHIND_ROUTER,
    /** A resource that is paid for or held but not used. */
    IDLE_OR_UNUSED,
    /** A cloud, site or service reached through one port, metro, connection or device. */
    SINGLE_POINT_OF_FAILURE,
    /** A port or router close to its capacity or package limit. */
    CAPACITY_RISK,
    /** Stuck or failed states, missing notifications or tagging. */
    HYGIENE
}
