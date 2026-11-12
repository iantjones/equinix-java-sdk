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

/** How one workload fares in a scenario. */
public enum WorkloadStatus {
    /** Every site that should reach it does, within its limits, and every cloud it needs is reachable at full capacity. */
    FULL,
    /** It still runs, but some sites, clouds, capacity or a latency limit are lost. */
    DEGRADED,
    /**
     * Nothing of it is served: no surviving host, or none of the clouds it needs is reachable (for a workload
     * that needs no cloud: no site reaches it).
     */
    DOWN
}
