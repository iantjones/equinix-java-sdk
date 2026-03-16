package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * A single metro recommendation with composite score, placement rationale,
 * provider availability, latency data, and cost estimates.
 */
@Value
@Builder
public class MetroRecommendation {

    int rank;
    MetroCode metroCode;
    String metroName;
    Region region;
    GeoCoordinate coordinates;
    MetroScore score;
    List<String> reasons;
    List<ProviderAvailability> availableProviders;
    Map<String, Double> siteLatencies;
    MetroCostBreakdown estimatedCost;
    List<WorkloadPlacement> assignedWorkloads;
}
