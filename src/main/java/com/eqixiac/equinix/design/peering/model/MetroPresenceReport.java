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

package com.eqixiac.equinix.design.peering.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.peering.enums.ConnectivityType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Detailed report of all ASN presence at a specific Equinix metro.
 *
 * <p>Provides a metro-centric view: given a metro, shows all target ASNs that are
 * present there, their connectivity types, and IX session details. Useful for
 * answering "what can I peer with in Ashburn?".</p>
 *
 * @author ianjones
 * @see PresenceMatrix
 */
@Value
@Builder
public class MetroPresenceReport {

    MetroId metro;

    String metroName;

    int ixCount;

    int facilityCount;

    List<PresenceCell> asnPresence;

    /**
     * Returns cells filtered to a specific connectivity type.
     *
     * @param type the connectivity type to filter by
     * @return filtered list of presence cells
     */
    public List<PresenceCell> byConnectivityType(ConnectivityType type) {
        return asnPresence.stream()
                .filter(c -> c.getConnectivityType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Returns ASNs that have IX peering at this metro.
     *
     * @return list of cells with IX peering
     */
    public List<PresenceCell> withIxPeering() {
        return asnPresence.stream()
                .filter(PresenceCell::isIxPresent)
                .collect(Collectors.toList());
    }

    /**
     * Returns ASNs available via Fabric at this metro.
     *
     * @return list of cells with Fabric availability
     */
    public List<PresenceCell> withFabric() {
        return asnPresence.stream()
                .filter(PresenceCell::isFabricAvailable)
                .collect(Collectors.toList());
    }

    /**
     * Returns the total IX capacity across all ASNs at this metro.
     *
     * @return total IX capacity in Mbps
     */
    public long totalIxCapacityMbps() {
        return asnPresence.stream()
                .mapToLong(PresenceCell::getTotalIxCapacityMbps)
                .sum();
    }
}
