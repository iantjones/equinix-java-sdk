package api.equinix.javasdk.design.optimizer.enums;

/**
 * Severity levels for risk findings produced by the metro optimization engine.
 *
 * @see api.equinix.javasdk.design.optimizer.model.RiskFinding
 * @see api.equinix.javasdk.design.optimizer.model.RiskAssessment
 */
public enum RiskSeverity {

    /** The deployment is not viable as recommended (e.g. no metro satisfied the constraints). */
    CRITICAL,

    /** A serious weakness that should be addressed before deploying (e.g. a single point of failure). */
    HIGH,

    /** A meaningful but survivable concern (e.g. a breached latency ceiling, a provider gap). */
    MEDIUM,

    /** A minor observation worth knowing about (e.g. mild provider concentration). */
    LOW,

    /** Informational only — includes the {@code HEALTHY} finding emitted when no risk was found. */
    INFO
}
