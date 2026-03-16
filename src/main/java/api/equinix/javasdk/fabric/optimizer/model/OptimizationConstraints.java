package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.fabric.optimizer.enums.ComplianceZone;
import api.equinix.javasdk.fabric.optimizer.enums.RedundancyTier;
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
    List<MetroCode> requiredMetros;
    List<MetroCode> excludedMetros;
    List<ComplianceZone> complianceZones;
    RedundancyTier minimumRedundancy;
    Double maxLatencyMs;
    Integer maxMetroCount;
    Integer minMetroCount;
}
