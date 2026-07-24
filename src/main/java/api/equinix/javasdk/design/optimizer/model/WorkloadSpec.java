package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.design.optimizer.enums.LatencySensitivity;
import api.equinix.javasdk.design.optimizer.enums.WorkloadType;
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

    String label;

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
     * A {@link #profileOverride} that states its own sensitivity outranks it; one that
     * leaves the field null does not.
     */
    LatencySensitivity latencySensitivity;

    int bandwidthMbps;

    List<ProviderRequirement> dependsOnProviders;

    /**
     * Resolves the effective workload profile by merging the type's defaults with any
     * user-supplied overrides.
     *
     * <p>Every field resolves by the same precedence: a non-null field on
     * {@link #profileOverride} wins, then the dedicated single-field override
     * ({@link #latencySensitivity}), then the {@link WorkloadType}'s default profile. The
     * boolean facility flags are unioned, because a stated requirement is never cancelled by a
     * profile that simply does not mention it.</p>
     *
     * <p>{@link #latencySensitivity} used to be consulted <em>only</em> when there was no
     * {@code profileOverride} at all. Every single-field setter on
     * {@code MetroOptimizer.WorkloadBuilder} — {@code maxLatencyToleranceMs},
     * {@code requiresHighPowerDensity}, {@code requiresLiquidCooling} — synthesizes a
     * {@code profileOverride}, so the common call shape
     * {@code .latencySensitivity(CRITICAL).maxLatencyToleranceMs(20)} (and the equivalent
     * {@code design_optimize_placement} payload, which sets both from one workload object) silently
     * discarded the stated sensitivity and fell back to the workload type's default. The merge
     * branch now consults it, so the two levers compose instead of one deleting the other.</p>
     *
     * <p>A null {@link #type} resolves as if it carried no default profile, rather than throwing:
     * {@code WorkloadSpec} is a public builder-built value and the optimizer reads this method for
     * every workload during validation.</p>
     *
     * @return the effective profile; its latency sensitivity is never null
     */
    public WorkloadProfile resolvedProfile() {
        WorkloadProfile base = type != null ? type.getDefaultProfile() : null;
        LatencySensitivity sensitivity = resolveLatencySensitivity(base);

        if (base == null && profileOverride == null) {
            return WorkloadProfile.builder()
                    .defaultLatencySensitivity(sensitivity)
                    .build();
        }
        if (base == null) {
            return profileOverride.toBuilder()
                    .defaultLatencySensitivity(sensitivity)
                    .build();
        }
        if (profileOverride == null) {
            return sensitivity == base.getDefaultLatencySensitivity()
                    ? base
                    : base.toBuilder().defaultLatencySensitivity(sensitivity).build();
        }
        // Merge: override takes precedence for non-null fields
        return WorkloadProfile.builder()
                .defaultLatencySensitivity(sensitivity)
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

    /**
     * The effective latency sensitivity: an explicit profile field, else the dedicated
     * {@link #latencySensitivity} override, else the type default, else
     * {@link LatencySensitivity#MEDIUM}. Never null, so callers comparing against a tier do not
     * have to special-case an absent one.
     */
    private LatencySensitivity resolveLatencySensitivity(WorkloadProfile base) {
        if (profileOverride != null && profileOverride.getDefaultLatencySensitivity() != null) {
            return profileOverride.getDefaultLatencySensitivity();
        }
        if (latencySensitivity != null) {
            return latencySensitivity;
        }
        if (base != null && base.getDefaultLatencySensitivity() != null) {
            return base.getDefaultLatencySensitivity();
        }
        return LatencySensitivity.MEDIUM;
    }
}
