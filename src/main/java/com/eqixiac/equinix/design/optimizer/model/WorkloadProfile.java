package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.optimizer.enums.LatencySensitivity;
import lombok.Builder;
import lombok.Value;

/**
 * Defines the infrastructure characteristics of a workload type.
 * Built-in {@link com.eqixiac.equinix.design.optimizer.enums.WorkloadType} constants
 * carry a default profile; users can override individual fields or supply a
 * fully custom profile for {@code WorkloadType.CUSTOM}.
 */
@Value
@Builder(toBuilder = true)
public class WorkloadProfile {

    /**
     * The latency tier this profile implies; its {@code thresholdMs} is the workload's default
     * placement ceiling when {@code maxLatencyToleranceMs} is unset.
     */
    LatencySensitivity defaultLatencySensitivity;

    /** High power density required (e.g. GPU racks). Recorded for facility selection; not a scoring input. */
    boolean requiresHighPowerDensity;

    /** Liquid cooling required. Recorded for facility selection; not a scoring input. */
    boolean requiresLiquidCooling;

    /**
     * When {@code true}, the workload is placed by the lowest-weighted-latency rule rather than
     * the highest-scored-metro rule (same effect as {@code LatencySensitivity.CRITICAL}).
     */
    boolean proximityWeighted;

    /**
     * Explicit per-workload latency ceiling in ms; overrides the tier's default ceiling. Breaches
     * raise {@code WORKLOAD_LATENCY_TOLERANCE_*} risk findings. {@code null} = use the tier's
     * {@code thresholdMs}.
     */
    Double maxLatencyToleranceMs;

    /**
     * Bandwidth floor in Mbps used for cost sizing: a workload declaring less bandwidth than this
     * is costed at the floor. {@code null} = no floor.
     */
    Double minBandwidthMbps;

    /**
     * Convenience factory for enum constant initialization.
     */
    public static WorkloadProfile of(LatencySensitivity sensitivity,
                                     boolean highPower,
                                     boolean liquidCooling,
                                     boolean proximityWeighted) {
        return WorkloadProfile.builder()
                .defaultLatencySensitivity(sensitivity)
                .requiresHighPowerDensity(highPower)
                .requiresLiquidCooling(liquidCooling)
                .proximityWeighted(proximityWeighted)
                .build();
    }
}
