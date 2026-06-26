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

    AI_ML_TRAINING(WorkloadProfile.of(LatencySensitivity.MEDIUM, true, true, false)),
    AI_ML_INFERENCE(WorkloadProfile.of(LatencySensitivity.HIGH, true, false, false)),
    REALTIME_COLLABORATION(WorkloadProfile.of(LatencySensitivity.CRITICAL, false, false, true)),
    TRANSACTIONAL(WorkloadProfile.of(LatencySensitivity.HIGH, false, false, false)),
    DISASTER_RECOVERY(WorkloadProfile.of(LatencySensitivity.LOW, false, false, false)),
    COLD_BACKUP(WorkloadProfile.of(LatencySensitivity.LOW, false, false, false)),
    GENERAL_COMPUTE(WorkloadProfile.of(LatencySensitivity.MEDIUM, false, false, false)),
    EDGE_COMPUTE(WorkloadProfile.of(LatencySensitivity.CRITICAL, false, false, true)),
    BIG_DATA_ANALYTICS(WorkloadProfile.of(LatencySensitivity.MEDIUM, true, false, false)),
    CUSTOM(null);

    private final WorkloadProfile defaultProfile;

    WorkloadType(WorkloadProfile defaultProfile) {
        this.defaultProfile = defaultProfile;
    }
}
