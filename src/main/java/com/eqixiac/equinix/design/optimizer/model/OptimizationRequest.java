package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.optimizer.enums.OptimizationStrategy;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
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
}
