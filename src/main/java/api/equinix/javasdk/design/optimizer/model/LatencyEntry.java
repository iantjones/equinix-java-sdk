package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Value;

/**
 * A single latency measurement between a metro and a user site.
 */
@Value
public class LatencyEntry {

    MetroCode fromMetro;
    String toSiteLabel;
    double latencyMs;
    boolean estimated;
}
