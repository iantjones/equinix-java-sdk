package api.equinix.javasdk.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Bandwidth sizing breakdown for a single connection, showing how the total
 * bandwidth <em>requirement</em> was derived from individual workload requirements under the
 * configured {@link api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy}.
 *
 * <p>This records the requirement, not the bill: when the requirement is rounded up to the
 * selected service profile's nearest billable tier, {@code totalMbps} here keeps the raw
 * requirement while the billed tier lives on {@code PlannedConnection.bandwidthMbps} /
 * {@code ProfileSelection.selectedTierMbps} — so a reader always sees both the ask and the
 * charge.</p>
 */
@Value
@Builder(toBuilder = true)
public class BandwidthAllocation {

    /**
     * The derived bandwidth requirement in Mbps — before any service-profile tier round-up
     * (the billed tier is on the connection, not here). Never below the 1000&nbsp;Mbps floor.
     */
    int totalMbps;

    /**
     * The per-workload contributions, keyed by workload label. Two sentinel keys replace
     * workload labels when no per-workload breakdown applies: {@code "custom"} (the value came
     * from a matched custom-bandwidth-map key) and {@code "default"} (no dependent workload
     * contributed, so the 1000&nbsp;Mbps floor was applied).
     */
    Map<String, Integer> perWorkload;

    /**
     * A human-readable explanation of the sizing — which strategy applied, whether a
     * custom-map key matched (or which keys were tried and missed), and, appended at profile
     * selection, any tier round-up with its billable impact.
     */
    String reasoning;
}
