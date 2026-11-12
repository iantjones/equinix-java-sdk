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

/**
 * How a design graph is built and simulated. Every field has a stated default; {@code null} takes it.
 *
 * @param accessHoming             how many portfolio metros each site has an access circuit into: its nearest
 *                                 metro (primary) and the next ones (standby). Default {@value #DEFAULT_ACCESS_HOMING}
 *                                 (dual-homed), an assumption the spec cannot state today
 * @param standbyCapacityFraction  the share of a workload's bandwidth its standby cloud connection is provisioned
 *                                 for, 0..1. Default 1.0, which is how the {@code WorkloadPlacer} sizes standby legs
 * @param maxPairScenarios         most pairwise (N-2) scenarios simulated; above it the pairs are taken in a fixed
 *                                 order and the report says how many were left out. Default
 *                                 {@value #DEFAULT_MAX_PAIRS}
 * @param availability             availability assumptions; default {@link AvailabilityAssumptions#defaults()}
 */
public record ResilienceOptions(Integer accessHoming, Double standbyCapacityFraction, Integer maxPairScenarios,
                                AvailabilityAssumptions availability) {

    /** Default {@link #accessHoming()}. */
    public static final int DEFAULT_ACCESS_HOMING = 2;
    /** Default {@link #maxPairScenarios()}. */
    public static final int DEFAULT_MAX_PAIRS = 2000;

    /** Applies the defaults and validates ranges. */
    public ResilienceOptions {
        accessHoming = accessHoming == null ? DEFAULT_ACCESS_HOMING : accessHoming;
        standbyCapacityFraction = standbyCapacityFraction == null ? 1.0 : standbyCapacityFraction;
        maxPairScenarios = maxPairScenarios == null ? DEFAULT_MAX_PAIRS : maxPairScenarios;
        availability = availability == null ? AvailabilityAssumptions.defaults() : availability;
        if (accessHoming < 1) {
            throw new IllegalArgumentException("accessHoming must be at least 1");
        }
        if (!(standbyCapacityFraction >= 0 && standbyCapacityFraction <= 1)) {
            throw new IllegalArgumentException("standbyCapacityFraction must be between 0 and 1");
        }
        if (maxPairScenarios < 0) {
            throw new IllegalArgumentException("maxPairScenarios must not be negative");
        }
    }

    /** @return every default */
    public static ResilienceOptions defaults() {
        return new ResilienceOptions(null, null, null, null);
    }

    /**
     * @param fraction the standby provisioning share, 0..1
     * @return a copy with it
     */
    public ResilienceOptions withStandbyCapacityFraction(double fraction) {
        return new ResilienceOptions(accessHoming, fraction, maxPairScenarios, availability);
    }

    /**
     * @param homing access circuits per site
     * @return a copy with it
     */
    public ResilienceOptions withAccessHoming(int homing) {
        return new ResilienceOptions(homing, standbyCapacityFraction, maxPairScenarios, availability);
    }

    /**
     * @param max most pairwise scenarios
     * @return a copy with it
     */
    public ResilienceOptions withMaxPairScenarios(int max) {
        return new ResilienceOptions(accessHoming, standbyCapacityFraction, max, availability);
    }

    /**
     * @param assumptions availability assumptions
     * @return a copy with them
     */
    public ResilienceOptions withAvailability(AvailabilityAssumptions assumptions) {
        return new ResilienceOptions(accessHoming, standbyCapacityFraction, maxPairScenarios, assumptions);
    }
}
