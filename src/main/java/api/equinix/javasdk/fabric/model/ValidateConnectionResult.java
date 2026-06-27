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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.fabric.model.implementation.ConnectionSide;
import api.equinix.javasdk.fabric.model.implementation.Redundancy;

/**
 * A single connection specification returned by the Fabric connection validation API
 * ({@code POST /fabric/v4/connections/validate}). Describes a connection that would be (or
 * already is) provisioned for the supplied authorization key or VLAN, allowing callers to
 * verify availability before creating a connection.
 *
 * @author ianjones
 * @version $Id: $Id
 * @see api.equinix.javasdk.fabric.client.Connections#validate(api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList)
 */
public interface ValidateConnectionResult {

    /**
     * <p>The Equinix-assigned connection identifier, when the validation matched an existing
     * connection.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>The connection bandwidth in Mbps.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getBandwidth();

    /**
     * <p>The connection redundancy configuration.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Redundancy} object.
     */
    Redundancy getRedundancy();

    /**
     * <p>The A-side of the connection.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ConnectionSide} object.
     */
    ConnectionSide getASide();

    /**
     * <p>The Z-side of the connection.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ConnectionSide} object.
     */
    ConnectionSide getZSide();
}
