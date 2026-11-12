package com.eqixiac.equinix.design.optimizer.enums;

import lombok.Getter;

/**
 * Latency sensitivity tiers for workloads, each with a typical round-trip ceiling
 * ({@code thresholdMs}).
 *
 * <p>The tier is descriptive: the Metro Optimizer does not turn it into a limit. Only an explicit
 * {@code WorkloadIntent.maxLatencyMs} constrains where a workload may run (the
 * {@code workload_max_latency} constraint), so a design is never rejected for a ceiling the caller
 * did not state. Use {@code thresholdMs} as a starting point when choosing that explicit value.</p>
 *
 * @see com.eqixiac.equinix.design.spec.WorkloadIntent
 */
@Getter
public enum LatencySensitivity {

    /**
     * Sub-5&nbsp;ms default placement ceiling. Additionally routes the workload through the
     * lowest-weighted-latency placement rule instead of the highest-scored-metro rule.
     */
    CRITICAL(5.0, "Sub-5ms latency required"),

    /** Sub-15&nbsp;ms default placement ceiling. */
    HIGH(15.0, "Sub-15ms latency preferred"),

    /** Sub-50&nbsp;ms default placement ceiling; the fallback tier when a workload states none. */
    MEDIUM(50.0, "Sub-50ms latency acceptable"),

    /** 200&nbsp;ms default placement ceiling - effectively unconstrained for most topologies. */
    LOW(200.0, "Latency tolerant up to 200ms");

    private final double thresholdMs;
    private final String description;

    LatencySensitivity(double thresholdMs, String description) {
        this.thresholdMs = thresholdMs;
        this.description = description;
    }
}
