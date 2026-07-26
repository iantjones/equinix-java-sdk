package api.equinix.javasdk.design.optimizer.enums;

import lombok.Getter;

/**
 * Defines latency sensitivity tiers for workload placement decisions.
 *
 * <p>Each tier carries {@code thresholdMs}, the <strong>default per-workload latency
 * ceiling</strong> the optimizer engine ({@code MetroOptimizerEngine})
 * applies during workload placement. A workload's effective ceiling is its explicit
 * {@code WorkloadProfile.maxLatencyToleranceMs} when set; otherwise it is this tier's
 * {@code thresholdMs} &mdash; so a {@code CRITICAL} or {@code HIGH} workload is placed under a
 * tighter default ceiling than a {@code LOW} one, measured as the worst-case estimated latency
 * from the metro to any user site.</p>
 *
 * <p>Exactly what the ceiling does, per kind:</p>
 * <ul>
 *   <li>An <em>explicit</em> ceiling always narrows which recommended metros the workload may be
 *       placed in, is stated on the placement rationale, and raises risk findings when it cannot be
 *       evaluated or honoured.</li>
 *   <li>The tier's <em>implied</em> ceiling narrows placement the same way, and the placement
 *       rationale names the tier whenever the ceiling actually ruled metros out or could not be
 *       honoured; when it binds nothing (or there are no sites to measure from) it is silent and
 *       raises no findings, because the caller never stated it.</li>
 * </ul>
 *
 * <p>Separately, {@code CRITICAL} (and any profile flagged proximity-weighted) routes the workload
 * through the lowest-weighted-latency placement rule rather than the highest-scored-metro rule.
 * The tiers do not scale the latency <em>scoring</em> curve &mdash; that is driven by the
 * thresholds on {@link api.equinix.javasdk.design.optimizer.model.ScoringWeights}.</p>
 *
 * @see api.equinix.javasdk.design.optimizer.model.WorkloadProfile
 * @see api.equinix.javasdk.design.optimizer.model.ScoringWeights
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

    /** 200&nbsp;ms default placement ceiling — effectively unconstrained for most topologies. */
    LOW(200.0, "Latency tolerant up to 200ms");

    private final double thresholdMs;
    private final String description;

    LatencySensitivity(double thresholdMs, String description) {
        this.thresholdMs = thresholdMs;
        this.description = description;
    }
}
