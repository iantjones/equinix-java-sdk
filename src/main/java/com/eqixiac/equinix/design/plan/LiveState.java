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


package com.eqixiac.equinix.design.plan;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A snapshot of the live Fabric resources relevant to one plan, as {@link LiveStateReader} read them
 * (or as a test builds them). Plain data, so drift detection is a pure function of plan and snapshot.
 *
 * @param routers          Cloud Routers in the plan's scope
 * @param connections      connections in the plan's scope
 * @param routingProtocols routing protocols by parent connection uuid (read only for connections matched to the plan)
 * @param notes            what could not be read and why (for example a 403 on one connection's protocols)
 * @param readAt           when the snapshot was taken
 */
public record LiveState(List<LiveRouter> routers, List<LiveConnection> connections,
                        Map<String, List<LiveRoutingProtocol>> routingProtocols, List<String> notes,
                        Instant readAt) {

    /** Makes every collection immutable and non-null. */
    public LiveState {
        routers = routers == null ? List.of() : List.copyOf(routers);
        connections = connections == null ? List.of() : List.copyOf(connections);
        routingProtocols = routingProtocols == null ? Map.of() : Map.copyOf(routingProtocols);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /**
     * An empty snapshot (nothing exists).
     *
     * @return an empty state
     */
    public static LiveState empty() {
        return new LiveState(List.of(), List.of(), Map.of(), List.of(), null);
    }

    /**
     * The routing protocols read for a connection.
     *
     * @param connectionUuid the connection uuid
     * @return the protocols, empty when none were read
     */
    public List<LiveRoutingProtocol> protocolsOf(String connectionUuid) {
        return routingProtocols.getOrDefault(connectionUuid, List.of());
    }
}
