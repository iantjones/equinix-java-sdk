package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.core.model.MetroId;
import lombok.Builder;
import lombok.Value;

/**
 * Assignment of a single workload to a specific metro with placement rationale.
 */
@Value
@Builder
public class WorkloadPlacement {

    String workloadLabel;
    MetroId assignedMetro;
    String reasoning;
}
