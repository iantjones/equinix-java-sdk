package api.equinix.javasdk.design.optimizer.enums;

import api.equinix.javasdk.design.optimizer.model.WorkloadProfile;
import lombok.Getter;

/**
 * Pre-defined workload archetypes with built-in infrastructure profiles.
 * Each constant carries a default {@link WorkloadProfile} that the optimizer
 * uses when no explicit profile override is provided.
 *
 * <p>Use {@link #CUSTOM} with a user-supplied {@code WorkloadProfile} for
 * workloads not covered by the built-in types.</p>
 *
 * @see WorkloadProfile
 * @see api.equinix.javasdk.design.optimizer.model.WorkloadSpec
 */
@Getter
public enum WorkloadType {

    /** Model training: MEDIUM latency sensitivity, high power density and liquid cooling required. */
    AI_ML_TRAINING(WorkloadProfile.of(LatencySensitivity.MEDIUM, true, true, false)),

    /** Model serving: HIGH latency sensitivity, high power density required. */
    AI_ML_INFERENCE(WorkloadProfile.of(LatencySensitivity.HIGH, true, false, false)),

    /** Voice/video/screen-share: CRITICAL latency sensitivity, proximity-weighted placement. */
    REALTIME_COLLABORATION(WorkloadProfile.of(LatencySensitivity.CRITICAL, false, false, true)),

    /** OLTP and payment-style systems: HIGH latency sensitivity. */
    TRANSACTIONAL(WorkloadProfile.of(LatencySensitivity.HIGH, false, false, false)),

    /** Warm standby/DR: LOW latency sensitivity — distance from users is acceptable. */
    DISASTER_RECOVERY(WorkloadProfile.of(LatencySensitivity.LOW, false, false, false)),

    /** Archival storage: LOW latency sensitivity. */
    COLD_BACKUP(WorkloadProfile.of(LatencySensitivity.LOW, false, false, false)),

    /** The default archetype: MEDIUM latency sensitivity, no facility requirements. */
    GENERAL_COMPUTE(WorkloadProfile.of(LatencySensitivity.MEDIUM, false, false, false)),

    /** Edge processing close to users: CRITICAL latency sensitivity, proximity-weighted placement. */
    EDGE_COMPUTE(WorkloadProfile.of(LatencySensitivity.CRITICAL, false, false, true)),

    /** Batch analytics: MEDIUM latency sensitivity, high power density required. */
    BIG_DATA_ANALYTICS(WorkloadProfile.of(LatencySensitivity.MEDIUM, true, false, false)),

    /**
     * No built-in profile ({@code getDefaultProfile()} is {@code null}); supply a
     * {@link WorkloadProfile} on the workload. A CUSTOM workload with no profile resolves to
     * {@code LatencySensitivity.MEDIUM} with no facility requirements.
     */
    CUSTOM(null);

    private final WorkloadProfile defaultProfile;

    WorkloadType(WorkloadProfile defaultProfile) {
        this.defaultProfile = defaultProfile;
    }
}
