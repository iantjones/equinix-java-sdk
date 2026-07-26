package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.core.model.MetroId;
import lombok.Value;

/**
 * A single latency measurement between a metro and a user site.
 */
@Value
public class LatencyEntry {

    /** The metro the measurement is from. */
    MetroId fromMetro;

    /** The user site's label the measurement is to. */
    String toSiteLabel;

    /** The estimated latency in milliseconds. */
    double latencyMs;

    /**
     * {@code true} when the figure is a Haversine fiber-distance estimate (rendered with a
     * {@code *} in the matrix); {@code false} when it comes from Equinix Fabric's published
     * metro-to-metro {@code avgLatency}.
     */
    boolean estimated;
}
