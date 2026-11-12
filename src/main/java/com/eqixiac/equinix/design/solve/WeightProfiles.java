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

package com.eqixiac.equinix.design.solve;

import com.eqixiac.equinix.design.spec.DesignObjectives;
import com.eqixiac.equinix.design.spi.Objective;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Named starting points for objective weights. A profile is data, not code: it is only a map from
 * objective id to relative weight, and a spec can override any entry or weight a plug-in objective
 * that no profile mentions.
 */
public final class WeightProfiles {

    /** The profile used when a spec names none and gives no weights. */
    public static final String DEFAULT = "balanced";

    private static final Map<String, Map<String, Double>> PROFILES;

    static {
        Map<String, Map<String, Double>> profiles = new LinkedHashMap<>();
        profiles.put("balanced", weights(0.30, 0.25, 0.20, 0.15, 0.10));
        profiles.put("latency_first", weights(0.50, 0.20, 0.10, 0.10, 0.10));
        profiles.put("cost_first", weights(0.15, 0.15, 0.45, 0.15, 0.10));
        profiles.put("redundancy_first", weights(0.20, 0.15, 0.15, 0.40, 0.10));
        profiles.put("provider_coverage_first", weights(0.20, 0.45, 0.15, 0.10, 0.10));
        PROFILES = Collections.unmodifiableMap(profiles);
    }

    private WeightProfiles() {
    }

    private static Map<String, Double> weights(double latency, double coverage, double cost, double resilience,
                                               double compliance) {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put(Objective.LATENCY, latency);
        weights.put(Objective.PROVIDER_COVERAGE, coverage);
        weights.put(Objective.COST, cost);
        weights.put(Objective.RESILIENCE, resilience);
        weights.put(Objective.COMPLIANCE, compliance);
        return Collections.unmodifiableMap(weights);
    }

    /** @return the names of the built-in profiles */
    public static Set<String> names() {
        return PROFILES.keySet();
    }

    /**
     * @param name a profile name, case-insensitive
     * @return its weights by objective id
     * @throws IllegalArgumentException when no profile has that name; the message lists the names
     */
    public static Map<String, Double> of(String name) {
        Map<String, Double> profile = name == null ? null : PROFILES.get(name.toLowerCase(Locale.ROOT));
        if (profile == null) {
            throw new IllegalArgumentException("Unknown weight profile '" + name + "'. Profiles: "
                    + String.join(", ", PROFILES.keySet()));
        }
        return profile;
    }

    /**
     * Turns a spec's objectives into weights that sum to 1: the named profile (or {@value #DEFAULT}
     * when the spec names none and lists no weights), overlaid with the spec's explicit weights. When the
     * spec lists weights but names no profile, only the listed objectives are weighted.
     *
     * @param objectives the spec's objectives; {@code null} means the default profile
     * @return weight per objective id, summing to 1, in a stable order; entries may be 0
     * @throws IllegalArgumentException for an unknown profile, a negative or non-finite weight, or
     *                                  weights that are all zero
     */
    public static Map<String, Double> resolve(DesignObjectives objectives) {
        Map<String, Double> resolved = new LinkedHashMap<>();
        boolean hasProfile = objectives != null && objectives.profile() != null && !objectives.profile().isBlank();
        boolean hasWeights = objectives != null && !objectives.weights().isEmpty();
        if (hasProfile) {
            resolved.putAll(of(objectives.profile()));
        } else if (!hasWeights) {
            resolved.putAll(of(DEFAULT));
        }
        if (hasWeights) {
            for (Map.Entry<String, Double> entry : objectives.weights().entrySet()) {
                Double weight = entry.getValue();
                if (weight == null || !(weight >= 0) || !Double.isFinite(weight)) {
                    throw new IllegalArgumentException("Weight for objective '" + entry.getKey()
                            + "' must be a non-negative, finite number, got " + weight);
                }
                resolved.put(entry.getKey(), weight);
            }
        }
        double total = 0;
        for (double weight : resolved.values()) {
            total += weight;
        }
        if (!(total > 0)) {
            throw new IllegalArgumentException("Objective weights are all zero: nothing ranks options");
        }
        for (Map.Entry<String, Double> entry : resolved.entrySet()) {
            entry.setValue(entry.getValue() / total);
        }
        return Collections.unmodifiableMap(resolved);
    }
}
