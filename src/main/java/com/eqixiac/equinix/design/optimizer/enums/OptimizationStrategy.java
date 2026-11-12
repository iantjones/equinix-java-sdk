package com.eqixiac.equinix.design.optimizer.enums;

/**
 * Named weightings of the optimizer's objectives, each one of the
 * {@link com.eqixiac.equinix.design.solve.WeightProfiles} profiles. A convenience for callers that
 * prefer an enum to a profile name: {@link #profile()} is what goes into
 * {@code DesignObjectives.profile}.
 */
public enum OptimizationStrategy {

    /** The {@code balanced} profile. */
    BALANCED("balanced"),

    /** The {@code latency_first} profile. */
    LATENCY_FIRST("latency_first"),

    /** The {@code cost_first} profile. */
    COST_FIRST("cost_first"),

    /** The {@code redundancy_first} profile. */
    REDUNDANCY_FIRST("redundancy_first"),

    /** The {@code provider_coverage_first} profile. */
    PROVIDER_COVERAGE_FIRST("provider_coverage_first");

    private final String profile;

    OptimizationStrategy(String profile) {
        this.profile = profile;
    }

    /** @return the {@code WeightProfiles} name */
    public String profile() {
        return profile;
    }
}
