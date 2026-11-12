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

package com.eqixiac.equinix.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Instant;
import java.util.List;

/**
 * Equinix Fabric port or virtual-connection operational-status notification (port/VC up or down),
 * delivered with {@code Task.Resource} {@code DataCenter.Network.Port} or
 * {@code DataCenter.Network.VirtualConnection}. Shape: the spec's {@code FabricNotificationBody} and
 * the Fabric sample in the Notification Types guide.
 *
 * @param header  common header
 * @param body    the status-change details
 * @param rawJson the envelope as received
 */
public record FabricNotification(EmgHeader header, Body body, String rawJson) implements EmgMessage {

    /**
     * Whether the affected port or connection is reported {@link OperationalStatus#DOWN}.
     *
     * @return {@code true} only for an explicit DOWN status
     */
    public boolean isDown() {
        return body.data() != null && body.data().operation() != null
                && body.data().operation().operationalStatus() == OperationalStatus.DOWN;
    }

    /**
     * Fabric notification body. The spec also lists capitalised ticket-style attributes
     * ({@code State}, {@code RequestorId}, {@code ServicerId}); they are read when present.
     *
     * @param timestamp   event time (present in the documented sample, not in the spec)
     * @param type        notification type
     * @param uuid        Equinix id for the event
     * @param description summary of the event
     * @param data        the affected port or connection
     * @param state       operational status as a ticket-style {@code State} attribute, if sent
     * @param requestorId customer reference, if sent
     * @param servicerId  id of the port/connection, if sent
     */
    public record Body(Instant timestamp, Type type, String uuid, String description, Data data,
                       String state, String requestorId, String servicerId) {
    }

    /**
     * The affected port or virtual connection.
     *
     * @param type      asset type
     * @param uuid      Fabric UUID of the port or connection
     * @param name      name of the port or connection
     * @param href      Fabric v4 API link to the asset
     * @param operation operational status details
     * @param embedded  connections active on the port, if listed
     */
    public record Data(AssetType type, String uuid, String name, String href, Operation operation,
                       Embedded embedded) {

        /**
         * Fabric v4 API links of the virtual connections on the port ({@code embedded.connections.href}).
         *
         * @return the links, never {@code null}
         */
        public List<String> connectionHrefs() {
            return embedded == null || embedded.connections() == null
                    ? List.of() : embedded.connections().href();
        }
    }

    /**
     * Operational-status details.
     *
     * @param operationalStatus current status
     * @param opStatusChangedAt when the status was last observed to change
     * @param maintenanceMode   whether the change is due to (planned) maintenance
     */
    public record Operation(OperationalStatus operationalStatus, Instant opStatusChangedAt,
                            Boolean maintenanceMode) {
    }

    /**
     * Embedded related resources.
     *
     * @param connections the connections on the port
     */
    public record Embedded(Connections connections) {
    }

    /**
     * Virtual connections on a port.
     *
     * @param href Fabric v4 API links to the connections, never {@code null}
     */
    public record Connections(List<String> href) {

        /** Normalises {@code href} to an unmodifiable list without {@code null} elements. */
        public Connections {
            href = Wire.copy(href);
        }
    }

    /** Fabric notification type. */
    public enum Type {
        /** Port operational status changed. */
        PORT_OPSTATUS_NOTIFICATION,
        /** Virtual connection operational status changed. */
        CONNECTION_OPSTATUS_NOTIFICATION,
        /** A value this SDK version does not recognise. */
        UNKNOWN;

        /**
         * Tolerant lookup of a wire value.
         *
         * @param value the wire value
         * @return the constant, {@link #UNKNOWN} if unrecognised, {@code null} if {@code value} is null
         */
        @JsonCreator
        public static Type fromWire(String value) {
            return Wire.lookup(Type.class, value, UNKNOWN);
        }
    }

    /** Fabric asset type. */
    public enum AssetType {
        /** A Fabric port. */
        XF_PORT,
        /** A Fabric virtual connection. */
        EVPL_VC,
        /** A value this SDK version does not recognise. */
        UNKNOWN;

        /**
         * Tolerant lookup of a wire value.
         *
         * @param value the wire value
         * @return the constant, {@link #UNKNOWN} if unrecognised, {@code null} if {@code value} is null
         */
        @JsonCreator
        public static AssetType fromWire(String value) {
            return Wire.lookup(AssetType.class, value, UNKNOWN);
        }
    }

    /** Operational status of the port or connection. */
    public enum OperationalStatus {
        /** Operational. */
        UP,
        /** Not operational. */
        DOWN,
        /** A value this SDK version does not recognise. */
        UNKNOWN;

        /**
         * Tolerant lookup of a wire value.
         *
         * @param value the wire value
         * @return the constant, {@link #UNKNOWN} if unrecognised, {@code null} if {@code value} is null
         */
        @JsonCreator
        public static OperationalStatus fromWire(String value) {
            return Wire.lookup(OperationalStatus.class, value, UNKNOWN);
        }
    }
}
