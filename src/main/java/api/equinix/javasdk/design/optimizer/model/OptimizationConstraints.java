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

    /**
     * Monthly budget for the deployment. <strong>Defaults to {@code null}, which means no cap</strong>
     * &mdash; nothing here ever installs a hidden default ceiling. When set, the ceiling
     * ({@code BudgetRange.getMaxMonthly()}) is <em>reported against, never enforced</em>: no metro is
     * ever excluded or scored on it. The engine compares the estimated monthly total against the
     * ceiling and exposes the answer two ways &mdash; {@code CostEstimate.withinBudget}, and, when the
     * estimate exceeds the ceiling, a {@code BUDGET_EXCEEDED} risk finding &mdash; so an over-budget
     * deployment is surfaced rather than silently presented as acceptable. A {@code null} budget (the
     * default) is checked against nothing and raises no finding. {@code BudgetRange.getMinMonthly()} is
     * read by nothing in the engine.
     */
    BudgetRange budget;

    /** Only metros in these regions are candidates; {@code null}/empty = all regions. */
    List<Region> requiredRegions;

    /** Metros in these regions are excluded from candidacy. */
    List<Region> excludedRegions;

    /**
     * Metros guaranteed a place in the recommendation set regardless of score. They bypass the
     * latency filter (a breach is surfaced as a {@code LATENCY_THRESHOLD} finding instead); a code
     * not in the Fabric catalog raises {@code REQUIRED_METRO_NOT_FOUND}.
     */
    List<MetroId> requiredMetros;

    /** Metros excluded from candidacy entirely. */
    List<MetroId> excludedMetros;

    /**
     * Compliance zones the deployment must cover — deployment-level AND semantics: each zone must
     * be covered by at least one selected metro. See {@link ComplianceZone} for the full contract.
     */
    List<ComplianceZone> complianceZones;

    /** Minimum redundancy tier; also switches selection strategy — see {@link RedundancyTier}. */
    RedundancyTier minimumRedundancy;

    /**
     * Hard latency ceiling in ms from any candidate metro to any user site; metros beyond it are
     * filtered from candidacy (required metros excepted). {@code null} = no bound.
     */
    Double maxLatencyMs;

    /**
     * Maximum number of recommended metros. {@code null} = derived: {@code max(3, tierMinimum + 1)}
     * when a redundancy tier is set, else 3.
     */
    Integer maxMetroCount;
}
