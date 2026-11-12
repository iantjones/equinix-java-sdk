/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.eqixiac.equinix.design.resilience;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * One edge of a {@link DesignGraph}. Edges are bidirectional; {@code from}/{@code to} only fix the
 * drawing order.
 *
 * @param id            stable id, unique in the graph (for example {@code conn:aws@DC})
 * @param kind          what the edge stands for
 * @param from          one endpoint's node id
 * @param to            the other endpoint's node id
 * @param role          {@link EdgeRole#PRIMARY} or {@link EdgeRole#STANDBY} for redundant edges; {@code null} otherwise
 * @param capacityMbps  provisioned capacity in Mbps; {@code null} when unknown or not provisioned by the design
 *                      (an unknown capacity never constrains the simulation, and the report says so)
 * @param roundTripMs   round-trip latency in ms; {@code null} when unknown (never a sentinel number)
 * @param latencyBasis  where {@code roundTripMs} came from ({@code measured}, {@code estimated},
 *                      {@code intra-metro}, {@code assumed}); {@code null} when unknown
 * @param carries       labels of the workloads the edge carries (a standby carriage is suffixed {@code (standby)})
 * @param note          detail for the reader, or {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record GraphEdge(String id, EdgeKind kind, String from, String to, EdgeRole role, Double capacityMbps,
                        Double roundTripMs, String latencyBasis, List<String> carries, String note) {

    /** Requires the identity fields and rejects non-finite numbers; makes {@code carries} immutable. */
    public GraphEdge {
        if (id == null || kind == null || from == null || to == null) {
            throw new IllegalArgumentException("A graph edge needs an id, a kind and two endpoints");
        }
        if (capacityMbps != null && !(capacityMbps >= 0 && Double.isFinite(capacityMbps))) {
            throw new IllegalArgumentException("Edge " + id + ": capacity must be finite and non-negative");
        }
        if (roundTripMs != null && !(roundTripMs >= 0 && Double.isFinite(roundTripMs))) {
            throw new IllegalArgumentException("Edge " + id + ": latency must be finite and non-negative");
        }
        carries = carries == null ? List.of() : List.copyOf(carries);
    }

    /**
     * @param nodeId a node id
     * @return the other endpoint, or {@code null} when the edge does not touch the node
     */
    public String other(String nodeId) {
        if (from.equals(nodeId)) {
            return to;
        }
        return to.equals(nodeId) ? from : null;
    }
}
