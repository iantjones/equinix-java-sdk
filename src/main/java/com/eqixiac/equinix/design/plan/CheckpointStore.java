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

import java.util.Optional;

/**
 * Where reconcile keeps its {@link Checkpoint}s. Implementations must make {@link #save} durable
 * before returning: reconcile writes after every step and relies on the last write surviving a crash.
 *
 * @see InMemoryCheckpointStore
 * @see FileCheckpointStore
 */
public interface CheckpointStore {

    /**
     * The checkpoint for a deployment, if one was saved. Checkpoints are keyed by deployment name,
     * not plan id, so an edited plan (a new plan id) still sees what earlier runs created.
     *
     * @param deploymentName the plan's deployment name
     * @return the checkpoint, or empty
     */
    Optional<Checkpoint> load(String deploymentName);

    /**
     * Saves (replaces) the checkpoint of its deployment.
     *
     * @param checkpoint the checkpoint
     */
    void save(Checkpoint checkpoint);

    /**
     * Removes a deployment's checkpoint.
     *
     * @param deploymentName the deployment name
     */
    void delete(String deploymentName);
}
