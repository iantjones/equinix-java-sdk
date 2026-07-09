package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import lombok.Builder;
import lombok.Value;

/**
 * A single risk identified in the recommended deployment topology.
 */
@Value
@Builder
public class RiskFinding {

    RiskSeverity severity;
    String category;
    String description;
    String recommendation;
    MetroId affectedMetro;
}
