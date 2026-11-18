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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * How one metro of a design is entered.
 *
 * @param metro    the metro code
 * @param mode     ports, Network Edge, or the Cloud Router alone
 * @param reason   the rule that applied
 * @param items    the ports, cross connects, devices and Internet Access services, existing and to order
 * @param links    the virtual connections from the Cloud Router to each port or device
 * @param findings what a reader should act on, for example a metro entered through one port only
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MetroAccess(String metro, AccessMode mode, String reason, List<PlannedAccessItem> items,
                          List<PlannedAccessLink> links, List<String> findings) {

    /** Makes the lists immutable. */
    public MetroAccess {
        items = items == null ? List.of() : List.copyOf(items);
        links = links == null ? List.of() : List.copyOf(links);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    /**
     * @param kind an access kind
     * @return the items of that kind
     */
    public List<PlannedAccessItem> items(AccessKind kind) {
        return items.stream().filter(i -> i.kind() == kind).toList();
    }
}
