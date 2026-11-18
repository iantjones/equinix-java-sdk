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
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The virtual connection from the Cloud Router of a metro to one port or Network Edge device.
 *
 * @param name          the name of the connection in the plan
 * @param metro         the metro code
 * @param item          the name of the port or device it lands on
 * @param kind          {@link AccessKind#FABRIC_PORT} or {@link AccessKind#NETWORK_EDGE_DEVICE}
 * @param endpointUuid  the uuid of the port or device; {@code null} until the order is placed
 * @param bandwidthMbps the bandwidth of the connection in Mbps
 * @param priority      the link's place in its redundant pair, or {@code null}
 * @param sizing        how the bandwidth was chosen
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlannedAccessLink(String name, String metro, String item, AccessKind kind, String endpointUuid,
                                int bandwidthMbps, RedundancyPriority priority, String sizing) {
}
