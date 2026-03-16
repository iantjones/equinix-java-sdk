package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.fabric.optimizer.enums.LatencySensitivity;
import api.equinix.javasdk.fabric.optimizer.enums.WorkloadType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Defines a workload to be placed by the optimizer, including its type,
 * infrastructure requirements, and provider dependencies.
 */
@Value
@Builder
public class WorkloadSpec {

    /** A descriptive name for this workload (e.g., "ML Training Pipeline"). */
    String label;

    /** The workload archetype, which determines default infrastructure requirements. */
    WorkloadType type;

    /**
     * An optional custom profile that overrides specific fields from the
     * {@link WorkloadType}'s built-in defaults. Non-null fields in this profile
     * take precedence during {@link #resolvedProfile()} merging.
     */
    WorkloadProfile profileOverride;

    /**
     * An optional latency sensitivity override. If set, this takes precedence over the
     * default sensitivity from the workload type's profile during profile resolution.
     */
    LatencySensitivity latencySensitivity;

    /** Required network bandwidth in megabits per second. Used in cost estimation. */
    int bandwidthMbps;

    /** Providers that must be available at the metro where this workload is placed. */
    List<ProviderRequirement> dependsOnProviders;

    /**
     * Resolves the effective workload profile by merging the type's defaults
     * with any user-supplied overrides.
     */
    public WorkloadProfile resolvedProfile() {
        WorkloadProfile base = type.getDefaultProfile();
        if (base == null && profileOverride == null) {
            return WorkloadProfile.builder()
                    .defaultLatencySensitivity(LatencySensitivity.MEDIUM)
                    .build();
        }
        if (base == null) return profileOverride;
        if (profileOverride == null) {
            if (latencySensitivity != null) {
                return base.toBuilder()
                        .defaultLatencySensitivity(latencySensitivity)
                        .build();
            }
            return base;
        }
        // Merge: override takes precedence for non-null fields
        return WorkloadProfile.builder()
                .defaultLatencySensitivity(
                        profileOverride.getDefaultLatencySensitivity() != null
                                ? profileOverride.getDefaultLatencySensitivity()
                                : base.getDefaultLatencySensitivity())
                .requiresHighPowerDensity(
                        profileOverride.isRequiresHighPowerDensity() || base.isRequiresHighPowerDensity())
                .requiresLiquidCooling(
                        profileOverride.isRequiresLiquidCooling() || base.isRequiresLiquidCooling())
                .proximityWeighted(
                        profileOverride.isProximityWeighted() || base.isProximityWeighted())
                .maxLatencyToleranceMs(
                        profileOverride.getMaxLatencyToleranceMs() != null
                                ? profileOverride.getMaxLatencyToleranceMs()
                                : base.getMaxLatencyToleranceMs())
                .minBandwidthMbps(
                        profileOverride.getMinBandwidthMbps() != null
                                ? profileOverride.getMinBandwidthMbps()
                                : base.getMinBandwidthMbps())
                .build();
    }
}
