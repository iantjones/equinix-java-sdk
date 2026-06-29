package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import lombok.Value;

/**
 * A single risk identified in the recommended deployment topology.
 */
@Value
public class RiskFinding {

    RiskSeverity severity;
    String category;
    String description;
    String recommendation;
    MetroCode affectedMetro;
}
