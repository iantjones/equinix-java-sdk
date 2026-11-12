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


package com.eqixiac.equinix.design.plan;

import java.util.List;

/**
 * The drift verdict for one planned resource.
 *
 * @param key         the logical key
 * @param kind        the element kind
 * @param name        the planned name
 * @param status      the verdict
 * @param liveUuid    the uuid of the matched live resource, or {@code null}
 * @param matchedBy   how the live resource was matched ({@code checkpoint}, {@code marker}, {@code name},
 *                    {@code type}), or {@code null}
 * @param differences the differing fields (empty unless {@code PRESENT_DIFFERENT})
 * @param note        an explanation, or {@code null}
 */
public record ResourceDrift(String key, ResourceKind kind, String name, DriftStatus status, String liveUuid,
                            String matchedBy, List<FieldDifference> differences, String note) {

    /** Makes {@code differences} immutable and non-null. */
    public ResourceDrift {
        differences = differences == null ? List.of() : List.copyOf(differences);
    }

    /**
     * Whether every difference can be patched in place.
     *
     * @return {@code true} when there is at least one difference and all are patchable
     */
    public boolean fullyPatchable() {
        return !differences.isEmpty() && differences.stream().allMatch(FieldDifference::patchable);
    }
}
