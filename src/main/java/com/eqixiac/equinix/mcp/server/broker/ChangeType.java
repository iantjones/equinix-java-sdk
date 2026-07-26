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

package com.eqixiac.equinix.mcp.server.broker;

import java.util.Arrays;
import java.util.Locale;

/**
 * The mutations the Safe Mutation Broker can carry — deliberately the smallest honest set:
 * three <em>creates</em>, each backed by a spec-documented Fabric v4 dry run the propose phase
 * exercises for real. There are no update change types at launch and, by policy, no delete
 * change types ever.
 */
public enum ChangeType {

    /** Create a Fabric connection ({@code POST /fabric/v4/connections}). */
    CONNECTION_CREATE("connection_create"),

    /** Create a Fabric network ({@code POST /fabric/v4/networks}). */
    NETWORK_CREATE("network_create"),

    /** Create a Fabric service token ({@code POST /fabric/v4/serviceTokens}). */
    SERVICE_TOKEN_CREATE("service_token_create");

    private final String id;

    ChangeType(String id) {
        this.id = id;
    }

    /**
     * @return the stable snake_case id used on the tool wire (e.g. {@code connection_create})
     */
    public String id() {
        return id;
    }

    /**
     * @return every change-type id, in declaration order, for schema enums
     */
    public static String[] ids() {
        return Arrays.stream(values()).map(ChangeType::id).toArray(String[]::new);
    }

    /**
     * Parses a change-type id (case-insensitive; the enum constant name is accepted too).
     *
     * @param value the id, e.g. {@code "connection_create"}
     * @return the matching change type
     * @throws IllegalArgumentException if the id is unknown, listing the valid ids
     */
    public static ChangeType fromId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.id.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("'change_type' value '" + value
                        + "' is not valid. Valid values: " + String.join(", ", ids())
                        + ". There are no update or delete change types."));
    }
}
