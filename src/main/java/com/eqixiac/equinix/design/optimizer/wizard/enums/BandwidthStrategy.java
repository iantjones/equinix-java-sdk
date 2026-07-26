package com.eqixiac.equinix.design.optimizer.wizard.enums;

/**
 * Determines how the Deployment Wizard sizes bandwidth for provider connections. Selected via
 * {@code DeploymentWizard.Builder.bandwidthStrategy(BandwidthStrategy)} (or implied by
 * {@code customBandwidthMap(...)}, which switches the strategy to {@link #CUSTOM}). Whatever the
 * strategy, the derivation is recorded on each connection's {@code BandwidthAllocation} and the
 * requirement may still be rounded up to the selected service profile's nearest billable tier
 * (surfaced on {@code ProfileSelection}, never silent).
 */
public enum BandwidthStrategy {

    /**
     * Each provider connection is sized to the sum of the bandwidths of the workloads that
     * depend on that provider at that metro. Most accurate sizing for pricing. The default.
     */
    PER_WORKLOAD,

    /**
     * Every provider connection at a metro is sized to the total bandwidth of all workloads
     * placed at that metro, regardless of which provider each workload depends on. Simpler,
     * but over-provisions (and over-prices) any provider only a subset of workloads uses.
     */
    AGGREGATED,

    /**
     * The caller supplies explicit bandwidths via
     * {@code DeploymentWizard.Builder.customBandwidthMap(Map)}. Values are Mbps; keys are
     * matched per (metro, provider), most specific first: {@code "<metroId>-<providerLabel>"}
     * (that provider in that metro, e.g. {@code "DC-Amazon Web Services"}) then
     * {@code "<providerLabel>"} (that provider in every metro). Keys are never connection
     * names. A (metro, provider) pair with no matching key &mdash; and the strategy as a whole
     * when no map was supplied &mdash; falls back to {@link #PER_WORKLOAD}-style aggregation of
     * dependent workloads, not a fabricated flat default; the fallback is stated in the
     * connection's {@code BandwidthAllocation} reasoning.
     */
    CUSTOM
}
