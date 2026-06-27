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

package api.equinix.javasdk.design.peering.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Builder;
import lombok.Value;

/**
 * Detailed IX presence information for a network at a specific Equinix IX.
 *
 * <p>Captures the port speed, IP addressing, route server participation, and BFD
 * support for a single peering session at an Equinix Internet Exchange. A network
 * may have multiple {@code IxPresenceDetail} entries for the same metro if it
 * peers at multiple IXes within that metro (e.g., San Jose and Palo Alto both
 * map to the SV metro).</p>
 *
 * @author ianjones
 * @see NetworkPresence
 */
@Value
@Builder
public class IxPresenceDetail {

    /** The Equinix metro where this IX is located. */
    MetroCode metro;

    /** The PeeringDB IX ID. */
    int ixId;

    /** The IX name (e.g., "Equinix Ashburn"). */
    String ixName;

    /** Port speed in Mbps (e.g., 10000 for 10G, 100000 for 100G). */
    int speedMbps;

    /** IPv4 peering address at this IX, or {@code null} if IPv4 not configured. */
    String ipv4Address;

    /** IPv6 peering address at this IX, or {@code null} if IPv6 not configured. */
    String ipv6Address;

    /** Whether this network peers via the IX route servers (Equinix MLPE). */
    boolean routeServerPeer;

    /** Whether BFD (Bidirectional Forwarding Detection) is supported. */
    boolean bfdSupport;

    /** Whether this peering session is operational. */
    boolean operational;

    /**
     * Returns a human-readable speed string (e.g., "10G", "100G", "1G").
     *
     * @return formatted speed string
     */
    public String speedFormatted() {
        if (speedMbps >= 100000) return (speedMbps / 1000) + "G";
        if (speedMbps >= 1000) return (speedMbps / 1000) + "G";
        return speedMbps + "M";
    }
}
