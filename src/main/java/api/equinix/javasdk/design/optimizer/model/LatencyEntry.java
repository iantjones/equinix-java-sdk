package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.model.MetroId;
import lombok.Value;

/**
 * A single latency measurement between a metro and a user site.
 */
@Value
public class LatencyEntry {

    MetroId fromMetro;
    String toSiteLabel;
    double latencyMs;
    boolean estimated;
}
