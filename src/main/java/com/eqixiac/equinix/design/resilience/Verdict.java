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

/** The outcome of one scenario, compared with the baseline (no failure). */
public enum Verdict {
    /** Nothing the design serves uses the failed elements. */
    UNAFFECTED,
    /** Traffic moves to other paths and everything is still served at full capacity within its limits. */
    SURVIVES,
    /** Everything still runs, but at reduced capacity, with fewer sites or over a broken latency limit. */
    DEGRADED,
    /** A workload goes down, a site is cut off, or a required cloud becomes unreachable for a workload that needs it. */
    OUTAGE
}
