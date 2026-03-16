package api.equinix.javasdk.fabric.optimizer.enums;

import lombok.Getter;

/**
 * Defines latency sensitivity tiers for workload placement decisions.
 * Each tier maps to a maximum tolerable latency threshold used by the
 * {@link api.equinix.javasdk.fabric.optimizer.MetroOptimizerEngine} when scoring metros.
 *
 * @see api.equinix.javasdk.fabric.optimizer.model.WorkloadProfile
 * @see api.equinix.javasdk.fabric.optimizer.model.ScoringWeights
 */
@Getter
public enum LatencySensitivity {

    CRITICAL(5.0, "Sub-5ms latency required"),
    HIGH(15.0, "Sub-15ms latency preferred"),
    MEDIUM(50.0, "Sub-50ms latency acceptable"),
    LOW(200.0, "Latency tolerant up to 200ms");

    private final double thresholdMs;
    private final String description;

    LatencySensitivity(double thresholdMs, String description) {
        this.thresholdMs = thresholdMs;
        this.description = description;
    }
}
