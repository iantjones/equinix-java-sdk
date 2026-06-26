package api.equinix.javasdk.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Bandwidth sizing breakdown for a single connection, showing how the total
 * bandwidth was derived from individual workload requirements.
 */
@Value
@Builder
public class BandwidthAllocation {

    /** Total allocated bandwidth in Mbps. */
    int totalMbps;

    /** Per-workload bandwidth breakdown: workload label to Mbps. */
    Map<String, Integer> perWorkload;

    /** Human-readable explanation of the bandwidth sizing rationale. */
    String reasoning;
}
