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

/**
 * An alternative connectivity path for reaching a target ASN from a different
 * Equinix metro than the primary location.
 *
 * <p>Represents a single failover option: "If your peering with AS16509 goes down
 * in DC, you could reach them via IX peering in NY with 10G capacity." Each failover
 * path includes the alternative metro, the connectivity type available, capacity,
 * and a geographic diversity assessment relative to the primary.</p>
 *
 * @author ianjones
 * @see ResiliencyAssessment
 * @see DiversityScore
 */
@Value
@Builder
public class FailoverPath {

    long targetAsn;

    String targetLabel;

    MetroId primaryMetro;

    MetroId failoverMetro;

    ConnectivityType connectivityType;

    long ixCapacityMbps;

    boolean routeServerAvailable;

    DiversityScore diversity;

    List<IxPresenceDetail> ixSessions;

    String recommendation;
}
