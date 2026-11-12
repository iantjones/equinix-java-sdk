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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The estimated availability of one workload, composed from {@link AvailabilityAssumptions} over the
 * graph's redundant paths. An estimate under stated assumptions, never a guarantee or an SLA.
 *
 * @param workload                the workload label
 * @param coreAvailability        fraction of time it runs with every cloud it needs reachable (hosts, routers,
 *                                connections, on-ramps, backbone, cloud regions)
 * @param endToEndAvailability    {@code coreAvailability} times the site-weighted availability of the sites'
 *                                access circuits
 * @param percent                 {@code endToEndAvailability} as a percentage
 * @param downtimeMinutesPerYear  expected downtime per year implied by {@code endToEndAvailability}
 *                                (365.25-day year, 525,960 minutes)
 * @param formula                 the series/parallel composition used, in words
 * @param notes                   approximations made for this workload
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record WorkloadAvailability(String workload, double coreAvailability, double endToEndAvailability,
                                   double percent, double downtimeMinutesPerYear, String formula, List<String> notes) {

    /** Minutes in a 365.25-day year. */
    public static final double MINUTES_PER_YEAR = 525_960.0;

    /** Makes {@code notes} immutable. */
    public WorkloadAvailability {
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
