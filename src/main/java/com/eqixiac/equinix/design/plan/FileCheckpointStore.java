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

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A {@link CheckpointStore} that keeps one JSON file per deployment in a directory
 * ({@code <deployment>.checkpoint.json}, the name reduced to file-safe characters). Each save writes
 * a temporary file and renames it over the old one, so a crash mid-write leaves the previous
 * checkpoint intact.
 */
public final class FileCheckpointStore implements CheckpointStore {

    private final Path directory;

    /**
     * Creates a store in a directory (created on first save).
     *
     * @param directory the directory
     */
    public FileCheckpointStore(Path directory) {
        this.directory = java.util.Objects.requireNonNull(directory, "directory");
    }

    /**
     * The file a deployment's checkpoint is kept in.
     *
     * @param deploymentName the deployment name
     * @return the path
     */
    public Path fileFor(String deploymentName) {
        String safe = (deploymentName == null ? "plan" : deploymentName).replaceAll("[^A-Za-z0-9._-]", "_");
        return directory.resolve(safe + ".checkpoint.json");
    }

    @Override
    public Optional<Checkpoint> load(String deploymentName) {
        Path file = fileFor(deploymentName);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(PlanJson.mapper().readValue(Files.readString(file, StandardCharsets.UTF_8),
                    Checkpoint.class));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read checkpoint " + file, e);
        }
    }

    @Override
    public void save(Checkpoint checkpoint) {
        try {
            PlanJson.writeAtomically(fileFor(checkpoint.deploymentName()),
                    PlanJson.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(checkpoint));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("A checkpoint is always serializable", e);
        }
    }

    @Override
    public void delete(String deploymentName) {
        try {
            Files.deleteIfExists(fileFor(deploymentName));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete checkpoint for " + deploymentName, e);
        }
    }
}
