package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
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

    /** 1-based position in the ranked recommendation list (1 = primary metro). */
    int rank;

    /** The recommended Equinix metro. */
    MetroId metroId;

    /** The metro's display name from the Fabric catalog. */
    String metroName;

    /** The metro's Equinix region (AMER/EMEA/APAC). */
    Region region;

    /** The metro's centroid coordinates from the Fabric catalog; may be {@code null}. */
    GeoCoordinate coordinates;

    /** The composite 0&ndash;100 score and its per-category breakdown. */
    MetroScore score;

    /** Human-readable statements of why this metro ranked where it did. */
    List<String> reasons;

    /** Per-provider availability at this metro, including candidate service profiles. */
    List<ProviderAvailability> availableProviders;

    /**
     * Estimated latency per user site: site label &rarr; milliseconds. Values are the engine's
     * estimate — Equinix Fabric metro-to-metro {@code avgLatency} where published, otherwise a
     * Haversine fiber-distance estimate — the same figures the latency score and the
     * {@code LatencyMatrix} use. Empty when the request defined no sites.
     */
    Map<String, Double> siteLatencies;

    /**
     * This metro's cost estimate, or {@code null} when the metro could not be priced (neither
     * the rate card nor the regional heuristic produced a figure).
     */
    MetroCostBreakdown estimatedCost;

    /**
     * The workloads the topology phase placed in this metro; empty when none were assigned here
     * (a valid outcome — lower-ranked metros often carry no workloads).
     */
    List<WorkloadPlacement> assignedWorkloads;
}
