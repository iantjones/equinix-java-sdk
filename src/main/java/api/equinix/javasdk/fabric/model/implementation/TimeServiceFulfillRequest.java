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

package api.equinix.javasdk.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Focused request body for fulfilling (provisioning) a Precision Time service via
 * {@code PUT /timeServices/{serviceId}}. Carries the Fabric connection UUIDs to attach to the
 * service instance.
 *
 * @author ianjones
 */
public class TimeServiceFulfillRequest {

    @JsonProperty("connections")
    private final List<Connection> connections;

    /**
     *
     * @param connectionUuids the Fabric connection UUIDs to attach to the service
     */
    public TimeServiceFulfillRequest(List<String> connectionUuids) {
        this.connections = connectionUuids.stream().map(Connection::new).collect(Collectors.toList());
    }

    /**
     * Reference to a Fabric connection, identified by UUID.
     */
    public static class Connection {

        @JsonProperty("uuid")
        private final String uuid;

        public Connection(String uuid) {
            this.uuid = uuid;
        }

        public String getUuid() {
            return this.uuid;
        }
    }
}
