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

import com.eqixiac.equinix.fabric.enums.RedundancyPriority;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One port, cross connect, Network Edge device or Internet Access service of a plan.
 *
 * @param name         the name of the item in the plan
 * @param kind         what it is
 * @param metro        the metro code
 * @param source       whether it exists, is assumed to exist, or is an order to place
 * @param existingUuid the uuid of the existing resource; {@code null} for an order
 * @param ibx          the IBX, when known
 * @param speedMbps    port speed in Mbps; {@code null} for other kinds or when unknown
 * @param priority     the item's place in its redundant pair, or {@code null} when it has no pair
 * @param attachesTo   for a cross connect, the name of the port it lands on; otherwise {@code null}
 * @param note         why the item was chosen, or how the order is placed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlannedAccessItem(String name, AccessKind kind, String metro, AccessSource source, String existingUuid,
                                String ibx, Integer speedMbps, RedundancyPriority priority, String attachesTo,
                                String note) {

    /** @return {@code true} when the item is a resource that already exists or is assumed to */
    @JsonIgnore
    public boolean exists() {
        return source != AccessSource.ORDER;
    }

    /** @return why a plan does not create this item */
    public String notExecutableReason() {
        if (exists()) {
            return "the " + label(kind) + " already exists and is used as it is";
        }
        return switch (kind) {
            case FABRIC_PORT -> "a Fabric port is a physical order with a letter of authorization; the plan does "
                    + "not place it. Order it, then put its uuid on the plan";
            case CROSS_CONNECT -> "a cross connect is installed by Equinix in the IBX and the API has no dry run "
                    + "for the order; the plan does not place it";
            case NETWORK_EDGE_DEVICE -> "a Network Edge device needs a device type, a licence and an SSH key, and "
                    + "the API has no dry run for the create; the plan does not create it. Create it, then put its "
                    + "uuid on the plan";
            case INTERNET_ACCESS -> "an Internet Access service is ordered separately; the plan does not place it";
        };
    }

    /**
     * @param kind an access kind
     * @return the kind in words
     */
    public static String label(AccessKind kind) {
        return switch (kind) {
            case FABRIC_PORT -> "Fabric port";
            case CROSS_CONNECT -> "cross connect";
            case NETWORK_EDGE_DEVICE -> "Network Edge device";
            case INTERNET_ACCESS -> "Internet Access service";
        };
    }
}
