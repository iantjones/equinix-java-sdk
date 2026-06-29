package api.equinix.javasdk.design.optimizer.wizard.enums;

/**
 * Determines how the Deployment Wizard sizes bandwidth for provider connections.
 *
 * <ul>
 *   <li>{@link #PER_WORKLOAD} — Each provider connection is sized to the sum of dependent
 *       workload bandwidths at that metro. Most accurate for pricing.</li>
 *   <li>{@link #AGGREGATED} — All connections at a metro are sized to the total metro bandwidth.
 *       Simpler but may over-provision individual connections.</li>
 *   <li>{@link #CUSTOM} — User supplies an explicit bandwidth map per connection.</li>
 * </ul>
 */
public enum BandwidthStrategy {

    PER_WORKLOAD,

    AGGREGATED,

    CUSTOM
}
