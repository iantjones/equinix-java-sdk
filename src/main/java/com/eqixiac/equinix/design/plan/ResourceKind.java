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

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/** The kinds of element a deployment plan holds, in the order they are created. */
public enum ResourceKind {
    /** A Fabric Cloud Router ({@code POST /fabric/v4/routers}). */
    CLOUD_ROUTER,
    /** A Cloud Router to service-provider connection ({@code POST /fabric/v4/connections}). */
    PROVIDER_CONNECTION,
    /** A Cloud Router to Cloud Router connection between two plan metros. */
    BACKBONE_LINK,
    /** A DIRECT or BGP routing protocol on a planned connection. */
    ROUTING_PROTOCOL,
    /** A Fabric Interconnect: planned, never executable through the public API. */
    INTERCONNECT,
    /** A native provider-to-provider link: planned and priced, created by the customer outside Fabric. */
    MULTICLOUD_LINK,
    /** Read-side fallback for a value this release does not know. */
    @JsonEnumDefaultValue
    UNKNOWN;

    /**
     * Whether the SDK creates elements of this kind through the Fabric API.
     *
     * @return {@code true} for routers, connections, backbone links and routing protocols
     */
    public boolean isExecutable() {
        return this == CLOUD_ROUTER || this == PROVIDER_CONNECTION || this == BACKBONE_LINK
                || this == ROUTING_PROTOCOL;
    }

    /**
     * Whether the element is a Fabric connection (provider connection or backbone link).
     *
     * @return {@code true} for {@link #PROVIDER_CONNECTION} and {@link #BACKBONE_LINK}
     */
    public boolean isConnection() {
        return this == PROVIDER_CONNECTION || this == BACKBONE_LINK;
    }
}
