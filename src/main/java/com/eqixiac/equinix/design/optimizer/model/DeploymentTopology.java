package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.core.model.MetroId;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The recommended deployment topology: which workloads are placed in which metros.
 */
@Value
public class DeploymentTopology {

    List<WorkloadPlacement> placements;

    /**
     * Returns the placements assigned to the given metro.
     *
     * <p>Metros are compared by {@link MetroId#equals(Object) value}, not by identity.
     * {@link MetroId} is a value type whose factories allocate a fresh instance per call
     * ({@code MetroId.of("DC")} twice yields two equal-but-distinct objects, and
     * {@code Metro.metroId()} re-derives one on every invocation), so an identity comparison here
     * matched nothing against live data: every recommendation came back with no assigned workloads
     * and the Deployment Wizard silently fell back to its default per-connection bandwidth while
     * reporting that it had summed the workloads.</p>
     *
     * @param metro the metro to select placements for; {@code null} yields an empty list
     * @return the placements assigned to that metro, in declaration order; never {@code null}
     */
    public List<WorkloadPlacement> forMetro(MetroId metro) {
        if (metro == null) return List.of();
        return placements.stream()
                .filter(p -> metro.equals(p.getAssignedMetro()))
                .collect(Collectors.toList());
    }

    /**
     * Renders the placement list grouped by metro, in first-placement order so the same topology
     * always renders the same way.
     */
    public String summary() {
        StringBuilder sb = new StringBuilder("Deployment Topology:\n");
        placements.stream()
                .collect(Collectors.groupingBy(WorkloadPlacement::getAssignedMetro,
                        LinkedHashMap::new, Collectors.toList()))
                .forEach((metro, wps) -> {
                    sb.append("  ").append(metro.code()).append(":\n");
                    wps.forEach(wp -> sb.append("    - ").append(wp.getWorkloadLabel())
                            .append(" (").append(wp.getReasoning()).append(")\n"));
                });
        return sb.toString();
    }
}
