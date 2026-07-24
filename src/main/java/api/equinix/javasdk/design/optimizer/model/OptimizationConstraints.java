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
     * ({@link BudgetRange#getMaxMonthly()}) is <em>reported against, never enforced</em>: no metro is
     * ever excluded or scored on it. The engine compares the estimated monthly total against the
     * ceiling and exposes the answer two ways &mdash; {@code CostEstimate.withinBudget}, and, when the
     * estimate exceeds the ceiling, a {@code BUDGET_EXCEEDED} risk finding &mdash; so an over-budget
     * deployment is surfaced rather than silently presented as acceptable. A {@code null} budget (the
     * default) is checked against nothing and raises no finding. {@link BudgetRange#getMinMonthly()} is
     * read by nothing in the engine.
     */
    BudgetRange budget;
    List<Region> requiredRegions;
    List<Region> excludedRegions;
    List<MetroId> requiredMetros;
    List<MetroId> excludedMetros;
    List<ComplianceZone> complianceZones;
    RedundancyTier minimumRedundancy;
    Double maxLatencyMs;
    Integer maxMetroCount;
}
