package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.design.optimizer.enums.LatencySensitivity;
import lombok.Builder;
import lombok.Value;

/**
 * Defines the infrastructure characteristics of a workload type.
 * Built-in {@link api.equinix.javasdk.design.optimizer.enums.WorkloadType} constants
 * carry a default profile; users can override individual fields or supply a
 * fully custom profile for {@code WorkloadType.CUSTOM}.
 */
@Value
@Builder(toBuilder = true)
public class WorkloadProfile {

    LatencySensitivity defaultLatencySensitivity;
    boolean requiresHighPowerDensity;
    boolean requiresLiquidCooling;
    boolean proximityWeighted;
    Double maxLatencyToleranceMs;
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
