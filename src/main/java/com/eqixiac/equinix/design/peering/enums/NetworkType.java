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

package com.eqixiac.equinix.design.peering.enums;

import lombok.Getter;

/**
 * Network type classification from PeeringDB's {@code info_type} field.
 *
 * <p>The type of network affects peering dynamics and the nature of connectivity:</p>
 * <ul>
 *   <li>{@link #NSP} — Network Service Provider (transit). Peering provides route table access.</li>
 *   <li>{@link #CONTENT} — Content delivery (Netflix, Google). Peering receives their content.</li>
 *   <li>{@link #ENTERPRISE} — Enterprise network. Enterprise-to-enterprise peering is uncommon.</li>
 *   <li>{@link #CABLE_DSL_ISP} — Access ISP. Peering offloads eyeball traffic.</li>
 *   <li>{@link #EDUCATION} — Academic/research network (e.g., Internet2).</li>
 *   <li>{@link #NON_PROFIT} — Non-profit organization network.</li>
 *   <li>{@link #ROUTE_SERVER} — IX route server (e.g., Equinix MLPE RS).</li>
 *   <li>{@link #GOV} — Government network.</li>
 * </ul>
 *
 * @author ianjones
 * @see com.eqixiac.equinix.design.peering.model.NetworkPresence
 */
@Getter
public enum NetworkType {

    NSP("NSP", "Network Service Provider"),
    CONTENT("Content", "Content Delivery Network"),
    ENTERPRISE("Enterprise", "Enterprise Network"),
    CABLE_DSL_ISP("Cable/DSL/ISP", "Access ISP"),
    EDUCATION("Educational/Research", "Academic / Research Network"),
    NON_PROFIT("Non-Profit", "Non-Profit Organization"),
    ROUTE_SERVER("Route Server", "Internet Exchange Route Server"),
    GOV("Government", "Government Network"),
    UNKNOWN("Unknown", "Unclassified Network");

    private final String displayName;
    private final String description;

    /**
     * Constructs a network type. Argument order is pinned here — the two
     * parameters are both {@code String}, so never regenerate this constructor
     * from field order.
     *
     * @param displayName the PeeringDB-facing display name
     * @param description the human-readable classification
     */
    NetworkType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Parses the PeeringDB {@code info_type} string to an enum value.
     *
     * @param pdbValue the raw PeeringDB value (e.g., "NSP", "Content", "Enterprise")
     * @return the matching type, or {@link #UNKNOWN} if unrecognized or null
     */
    public static NetworkType fromPeeringDb(String pdbValue) {
        if (pdbValue == null || pdbValue.isEmpty()) return UNKNOWN;
        switch (pdbValue) {
            case "NSP": return NSP;
            case "Content": return CONTENT;
            case "Enterprise": return ENTERPRISE;
            case "Cable/DSL/ISP": return CABLE_DSL_ISP;
            case "Educational/Research": return EDUCATION;
            case "Non-Profit": return NON_PROFIT;
            case "Route Server": return ROUTE_SERVER;
            case "Government": return GOV;
            default: return UNKNOWN;
        }
    }
}
