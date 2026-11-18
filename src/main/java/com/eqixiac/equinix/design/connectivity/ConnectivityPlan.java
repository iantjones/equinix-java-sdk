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
package com.eqixiac.equinix.design.connectivity;

import com.eqixiac.equinix.design.access.AccessPlan;

import java.util.List;

/**
 * How each destination of a design is reached and how each metro is entered. Nothing in it is ordered.
 * Give {@link #accessPlan()} to the Deployment Wizard and to the resilience analyzer to put the ports,
 * cross connects and Network Edge devices on the deployment plan and into the failure simulation.
 *
 * @param specFingerprint the fingerprint of the spec the decisions are for
 * @param reach           one decision per cloud, per peer, and one for general internet traffic
 * @param access          one decision per metro of the design
 * @param notes           what the rules could not determine
 * @param accessPlan      the items behind {@code access}: each port, cross connect, device and link
 */
public record ConnectivityPlan(String specFingerprint, List<ReachDecision> reach, List<AccessDecision> access,
                               List<String> notes, AccessPlan accessPlan) {

    /** Makes the lists immutable. */
    public ConnectivityPlan {
        reach = reach == null ? List.of() : List.copyOf(reach);
        access = access == null ? List.of() : List.copyOf(access);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /** How a destination is reached. */
    public enum Reach {
        /** A private Fabric connection from a Cloud Router to the cloud on-ramp. */
        FABRIC_CLOUD_ROUTER,
        /** Public peering at an Internet Exchange (IX). */
        INTERNET_EXCHANGE,
        /** A private Fabric connection to the peer's service profile. */
        FABRIC_PRIVATE,
        /** Equinix Internet Access. */
        INTERNET_ACCESS,
        /** Not decided: the data the rule needs is missing. */
        UNDECIDED
    }

    /** How a metro is entered. */
    public enum Access {
        /** Fabric ports with cross connects from the customer's cage. */
        PHYSICAL_PORTS,
        /** Network Edge virtual devices. */
        NETWORK_EDGE,
        /** The Cloud Router only: sites reach the metro over the backbone or an existing WAN. */
        CLOUD_ROUTER_ONLY
    }

    /**
     * How one destination is reached.
     *
     * @param destination the cloud id, the peer label, or {@code internet}
     * @param reach       the choice
     * @param metros      the metros it is reached in
     * @param reason      the rule that applied
     */
    public record ReachDecision(String destination, Reach reach, List<String> metros, String reason) {
        /** Makes the list immutable. */
        public ReachDecision {
            metros = metros == null ? List.of() : List.copyOf(metros);
        }
    }

    /**
     * How one metro is entered.
     *
     * @param metro  the metro code
     * @param access the choice
     * @param reason the rule that applied
     * @param orders what would be ordered, described; none is sent
     */
    public record AccessDecision(String metro, Access access, String reason, List<String> orders) {
        /** Makes the list immutable. */
        public AccessDecision {
            orders = orders == null ? List.of() : List.copyOf(orders);
        }
    }

    /** @return the plan as two Markdown tables and its notes */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder("## How each destination is reached\n\n");
        md.append("| Destination | Reached by | Metros | Rule |\n|---|---|---|---|\n");
        for (ReachDecision d : reach) {
            md.append("| ").append(d.destination()).append(" | ").append(label(d.reach())).append(" | ")
                    .append(d.metros().isEmpty() ? "none" : String.join(", ", d.metros())).append(" | ")
                    .append(d.reason()).append(" |\n");
        }
        md.append("\n## How each metro is entered\n\n");
        md.append("| Metro | Entered by | Rule | Would order |\n|---|---|---|---|\n");
        for (AccessDecision d : access) {
            md.append("| ").append(d.metro()).append(" | ").append(label(d.access())).append(" | ")
                    .append(d.reason()).append(" | ")
                    .append(d.orders().isEmpty() ? "nothing" : String.join("; ", d.orders())).append(" |\n");
        }
        md.append("\nThese choices are rules applied to the design. Nothing is ordered.\n");
        if (accessPlan != null && !accessPlan.items().isEmpty()) {
            md.append("\n").append(accessPlan.toMarkdown().replace("## Metro access", "## Access items by metro"));
        }
        if (!notes.isEmpty()) {
            md.append("\nNotes:\n\n");
            notes.forEach(n -> md.append("- ").append(n).append("\n"));
        }
        return md.toString();
    }

    private static String label(Reach reach) {
        return switch (reach) {
            case FABRIC_CLOUD_ROUTER -> "Fabric and Cloud Router";
            case INTERNET_EXCHANGE -> "Internet Exchange (IX)";
            case FABRIC_PRIVATE -> "Private Fabric connection";
            case INTERNET_ACCESS -> "Internet Access";
            case UNDECIDED -> "Undecided";
        };
    }

    private static String label(Access access) {
        return switch (access) {
            case PHYSICAL_PORTS -> "Physical ports";
            case NETWORK_EDGE -> "Network Edge";
            case CLOUD_ROUTER_ONLY -> "Cloud Router only";
        };
    }
}
