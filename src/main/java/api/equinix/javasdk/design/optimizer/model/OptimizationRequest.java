package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.design.optimizer.enums.OptimizationStrategy;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * The fully-assembled optimization request, containing all sites, providers,
 * workloads, constraints, strategy, and scoring overrides. Built by
 * {@link api.equinix.javasdk.design.optimizer.MetroOptimizer.Builder}.
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
