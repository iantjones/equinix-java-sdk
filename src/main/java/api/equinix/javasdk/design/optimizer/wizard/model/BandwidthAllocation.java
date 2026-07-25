package api.equinix.javasdk.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Bandwidth sizing breakdown for a single connection, showing how the total
 * bandwidth was derived from individual workload requirements.
 */
@Value
@Builder(toBuilder = true)
public class BandwidthAllocation {

    int totalMbps;

    Map<String, Integer> perWorkload;

    String reasoning;
}
