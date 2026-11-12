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

package com.eqixiac.equinix.design.spec;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.optimizer.enums.ComplianceZone;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Hard limits: a design that breaks one is not offered, whatever its score.
 *
 * @param requiredMetros     metro codes every design must include
 * @param excludedMetros     metro codes no design may include
 * @param requiredRegions    regions every design must have at least one metro in
 * @param excludedRegions    regions no design may use
 * @param complianceZones    regulatory zones the design must stay within
 * @param geoZones           Fabric geo scopes / geo zones (for example {@code EU}, {@code UK}, {@code CH},
 *                           {@code BR}, {@code AU}) traffic must stay inside: a data-sovereignty limit
 * @param minimumRedundancy  the least redundancy acceptable; may be {@code null}
 * @param maxLatencyMs       round-trip latency ceiling from every site to its serving metro, in ms; may be {@code null}
 * @param minMetros          fewest metros a design may use; may be {@code null}
 * @param maxMetros          most metros a design may use; may be {@code null}
 * @param minSurvivalPct     the least share of the design's service, in percent (0 to 100), that must survive its
 *                           worst single-element failure, measured by failure simulation; may be {@code null}
 *                           (not required). Omitted from JSON, YAML and the fingerprint when unset, so specs that
 *                           do not use it keep their fingerprint. It takes precedence over the older spec label
 *                           {@code design.min_survival_pct}.
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DesignConstraints(List<String> requiredMetros, List<String> excludedMetros,
                                List<Region> requiredRegions, List<Region> excludedRegions,
                                List<ComplianceZone> complianceZones, List<String> geoZones,
                                RedundancyTier minimumRedundancy, Double maxLatencyMs, Integer minMetros,
                                Integer maxMetros, Double minSurvivalPct) {

    /** Normalises every list to an immutable, non-null list. */
    public DesignConstraints {
        requiredMetros = requiredMetros == null ? List.of() : List.copyOf(requiredMetros);
        excludedMetros = excludedMetros == null ? List.of() : List.copyOf(excludedMetros);
        requiredRegions = requiredRegions == null ? List.of() : List.copyOf(requiredRegions);
        excludedRegions = excludedRegions == null ? List.of() : List.copyOf(excludedRegions);
        complianceZones = complianceZones == null ? List.of() : List.copyOf(complianceZones);
        geoZones = geoZones == null ? List.of() : List.copyOf(geoZones);
    }

    /** @return constraints that limit nothing */
    public static DesignConstraints none() {
        return new DesignConstraints(null, null, null, null, null, null, null, null, null, null, null);
    }
}
