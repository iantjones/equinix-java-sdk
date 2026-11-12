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
import java.util.List;
import java.util.Optional;

/**
 * A plan compared with live state: one {@link ResourceDrift} per planned element, the live resources
 * that carry the plan's marker but are not in it, and notes on anything that could not be read.
 *
 * @param planId         the plan id compared
 * @param deploymentName the plan's deployment name
 * @param projectId      the project searched, or {@code null}
 * @param resources      one verdict per planned element, in creation order
 * @param unmanaged      marked live resources the plan does not contain
 * @param notes          read failures and scope caveats
 * @param readAt         when the live state was read
 */
public record PlanDiff(String planId, String deploymentName, String projectId, List<ResourceDrift> resources,
                       List<UnmanagedResource> unmanaged, List<String> notes, Instant readAt) {

    /** Makes every list immutable and non-null. */
    public PlanDiff {
        resources = resources == null ? List.of() : List.copyOf(resources);
        unmanaged = unmanaged == null ? List.of() : List.copyOf(unmanaged);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /**
     * The verdict for a key.
     *
     * @param key the logical key
     * @return the verdict, or empty when the plan has no such key
     */
    public Optional<ResourceDrift> drift(String key) {
        return resources.stream().filter(r -> r.key().equals(key)).findFirst();
    }

    /**
     * The verdicts with a given status.
     *
     * @param status the status
     * @return the matching verdicts, in creation order
     */
    public List<ResourceDrift> withStatus(DriftStatus status) {
        return resources.stream().filter(r -> r.status() == status).toList();
    }

    /**
     * Whether live state matches the plan exactly: nothing missing, different or unreadable, and no
     * unmanaged resource.
     *
     * @return {@code true} when in sync
     */
    public boolean isInSync() {
        return unmanaged.isEmpty() && resources.stream().allMatch(r ->
                r.status() == DriftStatus.PRESENT_MATCHING || r.status() == DriftStatus.NOT_EXECUTABLE);
    }

    /**
     * A readable report.
     *
     * @return Markdown
     */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder("# Plan drift\n\n");
        md.append("Plan `").append(planId).append("` (deployment `").append(deploymentName).append("`")
                .append(projectId != null ? ", project `" + projectId + "`" : "").append(")")
                .append(readAt != null ? ", live state read " + readAt : "").append(".\n\n");
        md.append(isInSync() ? "**In sync.**\n\n" : "**Out of sync:** "
                + withStatus(DriftStatus.MISSING).size() + " missing, "
                + withStatus(DriftStatus.PRESENT_DIFFERENT).size() + " different, "
                + withStatus(DriftStatus.UNREADABLE).size() + " unreadable, "
                + unmanaged.size() + " unmanaged.\n\n");
        md.append("| Key | Kind | Name | Status | Live UUID | Differences |\n|---|---|---|---|---|---|\n");
        for (ResourceDrift r : resources) {
            md.append("| ").append(r.key()).append(" | ").append(r.kind()).append(" | ").append(r.name())
                    .append(" | ").append(r.status()).append(" | ").append(r.liveUuid() == null ? "-" : r.liveUuid())
                    .append(" | ");
            if (r.differences().isEmpty()) {
                md.append(r.note() == null ? "-" : r.note());
            } else {
                for (FieldDifference d : r.differences()) {
                    md.append(d.field()).append(": planned ").append(d.planned()).append(", live ").append(d.live())
                            .append(d.patchable() ? " (patchable)" : " (not patchable)").append("; ");
                }
            }
            md.append(" |\n");
        }
        if (!unmanaged.isEmpty()) {
            md.append("\n## Unmanaged\n\n| Kind | Name | UUID | Why |\n|---|---|---|---|\n");
            for (UnmanagedResource u : unmanaged) {
                md.append("| ").append(u.kind()).append(" | ").append(u.name()).append(" | ").append(u.uuid())
                        .append(" | ").append(u.reason()).append(" |\n");
            }
        }
        if (!notes.isEmpty()) {
            md.append("\n## Notes\n\n");
            notes.forEach(n -> md.append("- ").append(n).append("\n"));
        }
        return md.toString();
    }
}
