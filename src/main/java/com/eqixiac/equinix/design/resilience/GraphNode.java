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

import com.eqixiac.equinix.core.enums.Region;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;
import java.util.TreeSet;

/**
 * One node of a {@link DesignGraph}.
 *
 * @param id        stable id, unique in the graph (for example {@code metro:DC}, {@code onramp:aws@DC})
 * @param kind      what the node stands for
 * @param label     readable name
 * @param metro     the metro code the node is located in (metros, routers, on-ramps), or {@code null}
 * @param region    the Equinix region of {@code metro}, or {@code null} when unknown or not located
 * @param cloudId   the spec cloud id (on-ramps and clouds), or {@code null}
 * @param geoZones  the metro's Fabric geo scopes / zones as the catalogue reports them (metros only); never {@code null}
 * @param weight    a site's share of the total site weight (0..1); {@code null} for other nodes
 * @param providerRegion for a cloud, {@code PROVIDER region} when the spec names its region (clouds sharing it share
 *                  fate); {@code null} otherwise
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record GraphNode(String id, NodeKind kind, String label, String metro, Region region, String cloudId,
                        Set<String> geoZones, Double weight, String providerRegion) {

    /** Requires an id and a kind; makes {@code geoZones} immutable, sorted and non-null. */
    public GraphNode {
        if (id == null || id.isBlank() || kind == null) {
            throw new IllegalArgumentException("A graph node needs an id and a kind");
        }
        geoZones = geoZones == null ? Set.of() : java.util.Collections.unmodifiableSet(new TreeSet<>(geoZones));
        label = label == null ? id : label;
    }

    /**
     * A node with no provider region.
     *
     * @param id       the id
     * @param kind     the kind
     * @param label    the label
     * @param metro    the metro code, or {@code null}
     * @param region   the region, or {@code null}
     * @param cloudId  the cloud id, or {@code null}
     * @param geoZones the zones, or {@code null}
     * @param weight   the site weight, or {@code null}
     */
    public GraphNode(String id, NodeKind kind, String label, String metro, Region region, String cloudId,
                     Set<String> geoZones, Double weight) {
        this(id, kind, label, metro, region, cloudId, geoZones, weight, null);
    }
}
