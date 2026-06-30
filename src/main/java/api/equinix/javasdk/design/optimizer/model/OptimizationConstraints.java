package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.design.optimizer.enums.ComplianceZone;
import api.equinix.javasdk.design.optimizer.enums.RedundancyTier;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Hard and soft constraints that bound the optimization search space.
 */
@Value
@Builder
public class OptimizationConstraints {

    BudgetRange budget;
    List<Region> requiredRegions;
    List<Region> excludedRegions;
    List<MetroId> requiredMetros;
    List<MetroId> excludedMetros;
    List<ComplianceZone> complianceZones;
    RedundancyTier minimumRedundancy;
    Double maxLatencyMs;
    Integer maxMetroCount;
    Integer minMetroCount;
}
