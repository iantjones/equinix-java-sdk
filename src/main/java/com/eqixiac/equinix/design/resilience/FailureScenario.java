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
 * One failure to simulate: the graph nodes and edges it takes away.
 *
 * @param id           stable id (for example {@code metro:DC}, {@code conn:aws@NY},
 *                     {@code pair:metro:DC+conn:aws@NY})
 * @param kind         what fails
 * @param description  readable description
 * @param failedNodes  node ids taken away (every edge touching them fails too)
 * @param failedEdges  edge ids taken away
 * @param components   for a {@link ScenarioKind#PAIR}, the ids of the two single scenarios; empty otherwise
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record FailureScenario(String id, ScenarioKind kind, String description, List<String> failedNodes,
                              List<String> failedEdges, List<String> components) {

    /** Makes the lists immutable and non-null. */
    public FailureScenario {
        if (id == null || kind == null) {
            throw new IllegalArgumentException("A scenario needs an id and a kind");
        }
        failedNodes = failedNodes == null ? List.of() : List.copyOf(failedNodes);
        failedEdges = failedEdges == null ? List.of() : List.copyOf(failedEdges);
        components = components == null ? List.of() : List.copyOf(components);
        description = description == null ? id : description;
    }

    /** @return the scenario in which nothing fails */
    public static FailureScenario baseline() {
        return new FailureScenario("baseline", ScenarioKind.BASELINE, "no failure", null, null, null);
    }
}
