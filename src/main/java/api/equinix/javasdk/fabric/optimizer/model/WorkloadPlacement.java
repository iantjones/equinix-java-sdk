package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Value;

/**
 * Assignment of a single workload to a specific metro with placement rationale.
 */
@Value
public class WorkloadPlacement {

    String workloadLabel;
    MetroCode assignedMetro;
    String reasoning;
}
