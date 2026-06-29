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

package api.equinix.javasdk.design.peering.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Peering policy classification from PeeringDB's {@code policy_general} field.
 *
 * <p>A network's peering policy dramatically affects whether peering can be established.
 * Two networks at the same Equinix IX may be unable to peer if one has a {@link #RESTRICTIVE}
 * policy requiring traffic minimums or geographic coverage.</p>
 *
 * <ul>
 *   <li>{@link #OPEN} — Will peer with anyone who requests it. Easiest to establish.</li>
 *   <li>{@link #SELECTIVE} — Evaluates peering requests on a case-by-case basis. May require
 *       minimum traffic or presence in multiple locations.</li>
 *   <li>{@link #RESTRICTIVE} — Has strict requirements for peering (traffic ratios, contracts,
 *       geographic presence). Most difficult to establish.</li>
 *   <li>{@link #NO} — Does not peer. Only reachable via transit or private connectivity (e.g., Fabric).</li>
 * </ul>
 *
 * @author ianjones
 * @see api.equinix.javasdk.design.peering.model.NetworkPresence
 */
@Getter
@AllArgsConstructor
public enum PeeringPolicy {

    OPEN("Open", 1.0, "Will peer with any network upon request"),
    SELECTIVE("Selective", 0.6, "Evaluates peering requests case-by-case"),
    RESTRICTIVE("Restrictive", 0.3, "Strict requirements for traffic, geography, or contracts"),
    NO("No", 0.0, "Does not peer publicly"),
    UNKNOWN("Unknown", 0.5, "Policy not specified in PeeringDB");

    private final String displayName;

    /** Feasibility score (0.0 - 1.0) representing how likely peering can be established. */
    private final double feasibilityScore;

    private final String description;

    /**
     * Parses the PeeringDB {@code policy_general} string to an enum value.
     *
     * @param pdbValue the raw PeeringDB value (e.g., "Open", "Selective", "Restrictive", "No")
     * @return the matching policy, or {@link #UNKNOWN} if unrecognized or null
     */
    public static PeeringPolicy fromPeeringDb(String pdbValue) {
        if (pdbValue == null || pdbValue.isEmpty()) return UNKNOWN;
        switch (pdbValue.toLowerCase()) {
            case "open": return OPEN;
            case "selective": return SELECTIVE;
            case "restrictive": return RESTRICTIVE;
            case "no": return NO;
            default: return UNKNOWN;
        }
    }
}
