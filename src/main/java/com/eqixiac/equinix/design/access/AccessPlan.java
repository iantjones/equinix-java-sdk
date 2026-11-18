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
package com.eqixiac.equinix.design.access;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * How every metro of a design is entered.
 *
 * @param specFingerprint the fingerprint of the spec the plan is for
 * @param metros          one entry per metro of the design
 * @param notes           what the planner could not determine
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AccessPlan(String specFingerprint, List<MetroAccess> metros, List<String> notes) {

    /** Makes the lists immutable. */
    public AccessPlan {
        metros = metros == null ? List.of() : List.copyOf(metros);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /**
     * @param metro a metro code
     * @return the access of that metro, or {@code null} when the plan has none
     */
    public MetroAccess metro(String metro) {
        return metros.stream().filter(m -> m.metro().equalsIgnoreCase(metro)).findFirst().orElse(null);
    }

    /** @return every item of the plan, metro by metro */
    @JsonIgnore
    public List<PlannedAccessItem> items() {
        List<PlannedAccessItem> all = new ArrayList<>();
        metros.forEach(m -> all.addAll(m.items()));
        return all;
    }

    /** @return every link of the plan, metro by metro */
    @JsonIgnore
    public List<PlannedAccessLink> links() {
        List<PlannedAccessLink> all = new ArrayList<>();
        metros.forEach(m -> all.addAll(m.links()));
        return all;
    }

    /** @return every finding of the plan, each with its metro */
    @JsonIgnore
    public List<String> findings() {
        List<String> all = new ArrayList<>();
        metros.forEach(m -> m.findings().forEach(f -> all.add(m.metro() + ": " + f)));
        return all;
    }

    /** @return the plan as Markdown tables */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder("## Metro access\n\n");
        md.append("| Metro | Entered by | Rule |\n|---|---|---|\n");
        for (MetroAccess m : metros) {
            md.append("| ").append(m.metro()).append(" | ").append(label(m.mode())).append(" | ")
                    .append(m.reason()).append(" |\n");
        }
        if (!items().isEmpty()) {
            md.append("\n### Access items\n\n");
            md.append("| Name | Kind | Metro | Source | UUID | IBX | Speed | Pair | Note |\n");
            md.append("|---|---|---|---|---|---|---|---|---|\n");
            for (PlannedAccessItem i : items()) {
                md.append("| ").append(i.name()).append(" | ").append(PlannedAccessItem.label(i.kind())).append(" | ")
                        .append(i.metro()).append(" | ").append(label(i.source())).append(" | ")
                        .append(i.existingUuid() == null ? "-" : i.existingUuid()).append(" | ")
                        .append(i.ibx() == null ? "-" : i.ibx()).append(" | ")
                        .append(i.speedMbps() == null ? "-" : i.speedMbps() + " Mbps").append(" | ")
                        .append(i.priority() == null ? "-" : i.priority().name()).append(" | ")
                        .append(i.note() == null ? "" : i.note()).append(" |\n");
            }
        }
        if (!links().isEmpty()) {
            md.append("\n### Access connections to the Cloud Router\n\n");
            md.append("| Name | Metro | Lands on | Endpoint UUID | Bandwidth | Pair |\n|---|---|---|---|---|---|\n");
            for (PlannedAccessLink l : links()) {
                md.append("| ").append(l.name()).append(" | ").append(l.metro()).append(" | ").append(l.item())
                        .append(" | ").append(l.endpointUuid() == null ? "after the order" : l.endpointUuid())
                        .append(" | ").append(l.bandwidthMbps()).append(" Mbps | ")
                        .append(l.priority() == null ? "-" : l.priority().name()).append(" |\n");
            }
        }
        if (!findings().isEmpty()) {
            md.append("\nFindings:\n\n");
            findings().forEach(f -> md.append("- ").append(f).append("\n"));
        }
        if (!notes.isEmpty()) {
            md.append("\nNotes:\n\n");
            notes.forEach(n -> md.append("- ").append(n).append("\n"));
        }
        return md.toString();
    }

    /**
     * @param mode an access mode
     * @return the mode in words
     */
    public static String label(AccessMode mode) {
        return switch (mode) {
            case PHYSICAL_PORTS -> "Physical ports";
            case NETWORK_EDGE -> "Network Edge";
            case CLOUD_ROUTER_ONLY -> "Cloud Router only";
        };
    }

    private static String label(AccessSource source) {
        return switch (source) {
            case EXISTING -> "existing";
            case ASSUMED -> "assumed to exist";
            case ORDER -> "to order";
        };
    }
}
