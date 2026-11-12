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

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The progress of one plan's execution: every step known to exist, by logical key. Written by
 * reconcile and execute after every step (immediately after a create is accepted, before waiting for
 * it to provision), so a crashed or aborted run resumes without creating duplicates.
 *
 * <p>Immutable; {@link #with} and {@link #without} return new instances.</p>
 *
 * @param planId    the plan id ({@code PlanJson.planId}) the checkpoint belongs to
 * @param deploymentName the plan's deployment name
 * @param steps     the completed steps, by key, in completion order
 * @param updatedAt when the checkpoint was last written
 */
public record Checkpoint(String planId, String deploymentName, Map<String, CompletedStep> steps, Instant updatedAt) {

    /** Makes {@code steps} immutable, keeping its order. */
    public Checkpoint {
        steps = steps == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(steps));
    }

    /**
     * An empty checkpoint for a plan.
     *
     * @param planId         the plan id
     * @param deploymentName the deployment name
     * @return a checkpoint with no step
     */
    public static Checkpoint empty(String planId, String deploymentName) {
        return new Checkpoint(planId, deploymentName, Map.of(), null);
    }

    /**
     * The recorded step for a key.
     *
     * @param key the logical key
     * @return the step, or empty
     */
    public Optional<CompletedStep> step(String key) {
        return Optional.ofNullable(steps.get(key));
    }

    /**
     * This checkpoint with one step added or replaced.
     *
     * @param step the step
     * @param now  the write time
     * @return a new checkpoint
     */
    public Checkpoint with(CompletedStep step, Instant now) {
        Map<String, CompletedStep> next = new LinkedHashMap<>(steps);
        next.put(step.key(), step);
        return new Checkpoint(planId, deploymentName, next, now);
    }

    /**
     * This checkpoint with one step removed (after a confirmed rollback delete).
     *
     * @param key the logical key
     * @param now the write time
     * @return a new checkpoint
     */
    public Checkpoint without(String key, Instant now) {
        Map<String, CompletedStep> next = new LinkedHashMap<>(steps);
        next.remove(key);
        return new Checkpoint(planId, deploymentName, next, now);
    }
}
