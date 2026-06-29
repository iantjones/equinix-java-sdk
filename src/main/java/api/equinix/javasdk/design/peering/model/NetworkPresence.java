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
import api.equinix.javasdk.design.peering.enums.ConnectivityType;
import api.equinix.javasdk.design.peering.enums.NetworkType;
import api.equinix.javasdk.design.peering.enums.PeeringPolicy;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

/**
 * Aggregated presence information for a single network (ASN) across all Equinix metros.
 *
 * <p>Combines PeeringDB network metadata (peering policy, network type, traffic profile)
 * with per-metro IX peering and facility presence data. This is the primary data object
 * used to populate the {@link PresenceMatrix} and drive resiliency analysis.</p>
 *
 * @author ianjones
 * @see PresenceMatrix
 * @see IxPresenceDetail
 */
@Value
@Builder
public class NetworkPresence {

    long asn;

    String label;

    String peeringDbName;

    PeeringPolicy peeringPolicy;

    NetworkType networkType;

    String trafficVolume;

    String trafficRatio;

    boolean routeServerParticipant;

    boolean bfdSupported;

    boolean ipv6Capable;

    Set<MetroCode> ixPeeringMetros;

    Set<MetroCode> facilityMetros;

    Set<MetroCode> allMetros;

    List<IxPresenceDetail> ixDetails;

    long totalIxCapacityMbps;

    /**
     * Returns the number of distinct Equinix metros where this ASN has IX peering.
     *
     * @return count of metros with IX presence
     */
    public int ixMetroCount() {
        return ixPeeringMetros != null ? ixPeeringMetros.size() : 0;
    }

    /**
     * Returns the number of distinct Equinix metros where this ASN has facility presence.
     *
     * @return count of metros with facility presence
     */
    public int facilityMetroCount() {
        return facilityMetros != null ? facilityMetros.size() : 0;
    }

    /**
     * Checks whether this ASN has IX peering at a specific metro.
     *
     * @param metro the metro to check
     * @return {@code true} if IX peering is available at this metro
     */
    public boolean hasIxPeeringAt(MetroCode metro) {
        return ixPeeringMetros != null && ixPeeringMetros.contains(metro);
    }

    /**
     * Checks whether this ASN has facility presence at a specific metro.
     *
     * @param metro the metro to check
     * @return {@code true} if facility presence exists at this metro
     */
    public boolean hasFacilityAt(MetroCode metro) {
        return facilityMetros != null && facilityMetros.contains(metro);
    }

    /**
     * Checks whether this ASN has any Equinix presence at a specific metro.
     *
     * @param metro the metro to check
     * @return {@code true} if any presence exists at this metro
     */
    public boolean hasPresentAt(MetroCode metro) {
        return allMetros != null && allMetros.contains(metro);
    }
}
