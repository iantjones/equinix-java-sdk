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

package com.eqixiac.equinix.iam.client;

/**
 * Converts between the two project-identifier formats.
 *
 * <ul>
 *   <li><b>{@code project:<id>}</b>: the IAM (Access v1) {@code ProjectId}, pattern
 *       {@code ^project:[0-9a-zA-Z][0-9a-zA-Z-]{0,35}$}; required in every
 *       {@code /v1/projects/{projectId}/...} path.</li>
 *   <li><b>bare UUID</b>: the Resource Manager / Fabric {@code projectId}, as returned by
 *       {@code Projects}. Sent unchanged to IAM it fails the project-id format check
 *       (observed 2026-11-11).</li>
 * </ul>
 */
public final class ProjectIds {

    /** The prefix of an IAM project id. */
    public static final String PREFIX = "project:";

    private ProjectIds() {
    }

    /**
     * Returns the IAM {@code project:<id>} form of a project id given in either format.
     *
     * @param projectId a bare project UUID or an id that already starts with {@code project:}
     * @return the {@code project:} form
     * @throws IllegalArgumentException if {@code projectId} is null or blank
     */
    public static String of(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId must not be blank");
        }
        String trimmed = projectId.trim();
        return trimmed.startsWith(PREFIX) ? trimmed : PREFIX + trimmed;
    }

    /**
     * Returns the bare id of a project id given in either format.
     *
     * @param projectId a {@code project:<id>} or bare project id
     * @return the id without the {@code project:} prefix
     * @throws IllegalArgumentException if {@code projectId} is null or blank
     */
    public static String bare(String projectId) {
        return of(projectId).substring(PREFIX.length());
    }
}
