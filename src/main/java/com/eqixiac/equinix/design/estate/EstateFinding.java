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

package com.eqixiac.equinix.design.estate;

import java.util.List;
import java.util.Objects;

/**
 * One evidence-backed improvement to an existing estate.
 *
 * <p>The {@link #id()} is stable: the same rule on the same estate gives the same id, so findings
 * can be tracked between runs (for example "acknowledged", "fixed"). It is built from the category,
 * the rule's own discriminator and the affected resource ids.</p>
 *
 * @param id          a stable id, for example {@code RIGHT_SIZE_DOWN:connection:3a58dd05-...}
 * @param category    the category
 * @param severity    the severity
 * @param title       a one-line summary
 * @param resources   the affected resources, in a fixed order
 * @param evidence    the figures that triggered the finding, with their source and time window
 * @param action      the recommended action, stated concretely
 * @param impact      the financial impact of the action
 * @param confidence  how sure the analyser is
 * @param assumptions the assumptions the finding rests on
 * @param rule        the name of the rule that produced it
 */
public record EstateFinding(String id, FindingCategory category, Severity severity, String title,
                            List<ResourceRef> resources, List<Evidence> evidence, String action,
                            FinancialImpact impact, Confidence confidence, List<String> assumptions, String rule) {

    /** Validates the required fields and copies the lists. */
    public EstateFinding {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(impact, "impact");
        Objects.requireNonNull(confidence, "confidence");
        resources = resources == null ? List.of() : List.copyOf(resources);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
    }

    /**
     * Builds a stable id.
     *
     * @param category      the category
     * @param discriminator what distinguishes findings of one rule on the same resources (for example
     *                      {@code missing-secondary}); may be {@code null}
     * @param resourceIds   the affected resource ids
     * @return the id
     */
    public static String stableId(FindingCategory category, String discriminator, List<String> resourceIds) {
        StringBuilder b = new StringBuilder(category.name());
        if (discriminator != null && !discriminator.isBlank()) {
            b.append(':').append(discriminator);
        }
        List<String> ids = resourceIds.stream().filter(Objects::nonNull).sorted().toList();
        if (ids.size() <= 3) {
            ids.forEach(i -> b.append(':').append(i));
        } else {
            // A long list is folded into a deterministic digest so ids stay short.
            b.append(':').append(ids.size()).append("-resources-")
                    .append(Integer.toHexString(String.join(",", ids).hashCode()));
        }
        return b.toString();
    }

    /**
     * A resource a finding is about.
     *
     * @param kind the resource kind ({@code port}, {@code connection}, {@code cloudRouter},
     *             {@code network}, {@code serviceToken}, {@code device})
     * @param id   the resource id
     * @param name the resource name, when known
     */
    public record ResourceRef(String kind, String id, String name) {
    }

    /**
     * A figure that supports a finding.
     *
     * @param label  what it measures, for example {@code p95 utilisation}
     * @param value  the value as text, for example {@code 112.4}
     * @param unit   the unit, for example {@code Mbps}, or {@code null}
     * @param source where it came from, for example {@code inventory: GET /fabric/v4/ports} or
     *               {@code metrics: equinix.fabric.connection.bandwidth_tx.usage}
     * @param window the time window or as-of time it applies to, or {@code null}
     */
    public record Evidence(String label, String value, String unit, String source, String window) {
    }
}
