package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The recommended deployment topology: which workloads are placed in which metros.
 */
@Value
public class DeploymentTopology {

    List<WorkloadPlacement> placements;

    public List<WorkloadPlacement> forMetro(MetroCode metro) {
        return placements.stream()
                .filter(p -> p.getAssignedMetro() == metro)
                .collect(Collectors.toList());
    }

    public String summary() {
        StringBuilder sb = new StringBuilder("Deployment Topology:\n");
        placements.stream()
                .collect(Collectors.groupingBy(WorkloadPlacement::getAssignedMetro))
                .forEach((metro, wps) -> {
                    sb.append("  ").append(metro.name()).append(":\n");
                    wps.forEach(wp -> sb.append("    - ").append(wp.getWorkloadLabel())
                            .append(" (").append(wp.getReasoning()).append(")\n"));
                });
        return sb.toString();
    }
}
