package com.eqixiac.equinix.design.optimizer.wizard.enums;

/**
 * Defines the inter-metro backbone link topology for connecting Cloud Routers
 * across multiple Equinix metros. Selected via
 * {@code DeploymentWizard.Builder.backboneTopology(BackboneTopology)}; the wizard turns the
 * chosen topology into concrete {@code PlannedBackboneLink}s between the recommended metros,
 * in the order the optimizer ranked them.
 */
public enum BackboneTopology {

    /**
     * Every metro connects to every other metro: {@code N*(N-1)/2} links. Maximum path
     * diversity (any single link failure leaves all metros reachable) at the highest link
     * count and cost. The default.
     */
    FULL_MESH,

    /**
     * The hub metro connects to all others: {@code N-1} links. The hub is the
     * <em>highest-ranked</em> recommended metro (the optimizer's first recommendation);
     * spoke-to-spoke traffic transits the hub, which is therefore a single point of failure.
     * Cheapest topology for three or more metros.
     */
    HUB_SPOKE,

    /**
     * Each metro connects to the next in rank order, and the last back to the first:
     * {@code N} links for three or more metros. Survives any single link failure with one
     * remaining path. With exactly two metros a ring degenerates to the single A&ndash;B link
     * (the same one link {@link #FULL_MESH} would produce) &mdash; never a doubly-billed
     * duplicate pair.
     */
    RING
}
