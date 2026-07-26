package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.RiskSeverity;
import lombok.Builder;
import lombok.Value;

/**
 * A single risk identified in the recommended deployment topology.
 */
@Value
@Builder
public class RiskFinding {

    /** How serious the finding is; {@code HEALTHY} is emitted at {@code INFO}. */
    RiskSeverity severity;

    /**
     * Machine-readable finding category. The engine emits a closed vocabulary — switch on these
     * exact strings:
     * <ul>
     *   <li>{@code NO_VIABLE_METRO} — no metro satisfied the constraints (CRITICAL)</li>
     *   <li>{@code SINGLE_POINT_OF_FAILURE} — one metro carries the whole deployment</li>
     *   <li>{@code SINGLE_REGION} — every selected metro sits in one region</li>
     *   <li>{@code LATENCY_THRESHOLD} — a (required) metro breaches the request-level
     *       {@code maxLatencyMs} bound</li>
     *   <li>{@code LATENCY_BOUND_NOT_EVALUATED} — a latency bound was set but no sites exist to
     *       measure against</li>
     *   <li>{@code WORKLOAD_LATENCY_TOLERANCE_UNMET} / {@code WORKLOAD_LATENCY_TOLERANCE_NOT_EVALUATED}
     *       — a per-workload latency ceiling was breached / could not be measured</li>
     *   <li>{@code REQUIRED_METRO_NOT_FOUND} — a {@code requireMetro(...)} code is not in the
     *       Fabric catalog</li>
     *   <li>{@code PROVIDER_UNAVAILABLE} — a required provider is missing from one or more
     *       selected metros</li>
     *   <li>{@code WORKLOAD_PROVIDER_UNAVAILABLE} / {@code WORKLOAD_PROVIDER_NOT_COVERED} — a
     *       workload's declared provider dependency could not be honoured</li>
     *   <li>{@code COMPLIANCE_GAP} — a requested compliance zone is covered by no selected metro
     *       (or a force-included metro sits outside every requested zone)</li>
     *   <li>{@code PROVIDER_CONCENTRATION} — a provider is reachable at only one selected metro</li>
     *   <li>{@code REQUIRED_CLOUD_NOT_COVERED} — a request-level required cloud is reachable at no
     *       selected metro</li>
     *   <li>{@code BUDGET_EXCEEDED} — the estimated monthly total exceeds the budget ceiling</li>
     *   <li>{@code REDUNDANCY_GAP} — the selected set falls short of the requested redundancy tier</li>
     *   <li>{@code HEALTHY} — emitted (at INFO) when no risk was found</li>
     * </ul>
     */
    String category;

    /** Human-readable statement of the risk. */
    String description;

    /** Suggested mitigation; may be {@code null} (e.g. on {@code HEALTHY}). */
    String recommendation;

    /** The metro the finding is about, or {@code null} for deployment-wide findings. */
    MetroId affectedMetro;
}
