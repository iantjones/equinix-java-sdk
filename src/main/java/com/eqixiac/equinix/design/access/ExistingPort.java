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

/**
 * A Fabric port that exists in the project.
 *
 * @param uuid            the port uuid
 * @param name            the port name
 * @param metro           the metro code
 * @param ibx             the IBX the port is in, or {@code null} when the API did not state it
 * @param fabricDevice    the Fabric device the port is on, or {@code null}
 * @param redundancyGroup the redundancy group of the port, or {@code null}
 * @param priority        the port's priority in its group, or {@code null}
 * @param bandwidthMbps   the port speed in Mbps, or {@code null}
 * @param availableMbps   bandwidth not yet used by connections, in Mbps, or {@code null}
 * @param state           the port state as the API names it
 */
public record ExistingPort(String uuid, String name, String metro, String ibx, String fabricDevice,
                           String redundancyGroup, RedundancyPriority priority, Integer bandwidthMbps,
                           Integer availableMbps, String state) {

    /** @return {@code true} when the port is in a state that carries connections */
    public boolean usable() {
        return "ACTIVE".equals(state) || "PROVISIONED".equals(state);
    }

    /**
     * @param mbps a bandwidth in Mbps
     * @return {@code true} when the port has that much unused bandwidth, or does not state its usage
     */
    public boolean hasRoomFor(int mbps) {
        return availableMbps == null ? bandwidthMbps == null || bandwidthMbps >= mbps : availableMbps >= mbps;
    }
}
