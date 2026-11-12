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
 * What a rollback removed and what it had to leave behind.
 *
 * <p>Connections are deleted first and each delete is polled to {@code DEPROVISIONED} (or HTTP 404)
 * within a bounded timeout; a Cloud Router is deleted only after every connection attached to it is
 * confirmed gone. A connection whose delete fails or is not confirmed in time is an orphan, and so is
 * the router it attaches to (deleting it would fail or strand the connection), and so are its routing
 * protocols.</p>
 *
 * @param removed resources deleted and confirmed gone, in deletion order
 * @param orphans resources that still exist or whose deletion is unconfirmed, each with its uuid and reason
 */
public record RollbackReport(List<RemovedResource> removed, List<OrphanedResource> orphans) {

    /** Makes both lists immutable and non-null. */
    public RollbackReport {
        removed = removed == null ? List.of() : List.copyOf(removed);
        orphans = orphans == null ? List.of() : List.copyOf(orphans);
    }

    /**
     * An empty report: nothing to roll back.
     *
     * @return a report with no entries
     */
    public static RollbackReport empty() {
        return new RollbackReport(List.of(), List.of());
    }

    /**
     * Whether everything was removed.
     *
     * @return {@code true} when there is no orphan
     */
    public boolean isClean() {
        return orphans.isEmpty();
    }

    /**
     * A readable report listing every removed resource and every orphan with its uuid.
     *
     * @return Markdown
     */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder("## Rollback\n\n");
        md.append(isClean() ? "Everything this run created was removed.\n\n"
                : "**" + orphans.size() + " resource(s) could not be removed and may still be billed.**\n\n");
        if (!removed.isEmpty()) {
            md.append("| Removed | Name | UUID |\n|---|---|---|\n");
            for (RemovedResource r : removed) {
                md.append("| ").append(r.kind()).append(" | ").append(r.name()).append(" | ").append(r.uuid()).append(" |\n");
            }
            md.append("\n");
        }
        if (!orphans.isEmpty()) {
            md.append("| Orphan | Name | UUID | Reason |\n|---|---|---|---|\n");
            for (OrphanedResource o : orphans) {
                md.append("| ").append(o.kind()).append(" | ").append(o.name()).append(" | ").append(o.uuid())
                        .append(" | ").append(o.reason()).append(" |\n");
            }
            md.append("\n");
        }
        return md.toString();
    }
}
