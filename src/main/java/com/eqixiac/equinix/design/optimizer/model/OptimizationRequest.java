package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.optimizer.enums.OptimizationStrategy;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * The fully-assembled optimization request, containing all sites, providers,
 * workloads, constraints, strategy, and scoring overrides. Built by
 * {@link com.eqixiac.equinix.design.optimizer.MetroOptimizer.Builder}.
 */
@Value
@Builder
public class OptimizationRequest {

    List<UserSite> sites;

    List<ProviderRequirement> providers;

    List<WorkloadSpec> workloads;

    OptimizationConstraints constraints;

    OptimizationStrategy strategy;

    ScoringWeights scoringWeights;

    RateCard rateCard;

    Term term;

    /**
     * <b>Beta.</b> The catalog of native multicloud environments the engine consults for the
     * {@code NATIVE_MULTICLOUD_ALTERNATIVE} finding, and the Deployment Wizard's default when its
     * own lever is not set. {@code null} means {@link MulticloudEnvironmentCatalog#standard()}.
     * Reference data, not part of the request's serialized form.
     */
    @JsonIgnore
    MulticloudEnvironmentCatalog multicloudEnvironments;
}
