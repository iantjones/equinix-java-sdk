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

package com.eqixiac.equinix.fabric.model;

import com.eqixiac.equinix.fabric.model.implementation.ConnectionSide;
import com.eqixiac.equinix.fabric.model.implementation.Redundancy;

/**
 * A single connection specification returned by the Fabric connection validation API
 * ({@code POST /fabric/v4/connections/validate}). Describes a connection that would be (or
 * already is) provisioned for the supplied authorization key or VLAN, allowing callers to
 * verify availability before creating a connection.
 *
 * @author ianjones
 * @see com.eqixiac.equinix.fabric.client.Connections#validate(com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList)
 */
public interface ValidateConnectionResult {

    /**
     * <p>The Equinix-assigned connection identifier, when the validation matched an existing
     * connection.</p>
     *
     */
    String getUuid();

    /**
     * <p>The connection bandwidth in Mbps.</p>
     *
     */
    Integer getBandwidth();

    /**
     * <p>The connection redundancy configuration.</p>
     *
     */
    Redundancy getRedundancy();

    /**
     * <p>The A-side of the connection.</p>
     *
     */
    ConnectionSide getASide();

    /**
     * <p>The Z-side of the connection.</p>
     *
     */
    ConnectionSide getZSide();
}
