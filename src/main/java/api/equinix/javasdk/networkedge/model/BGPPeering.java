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

package api.equinix.javasdk.networkedge.model;

import api.equinix.javasdk.networkedge.enums.BGPState;
import api.equinix.javasdk.networkedge.enums.BGPStatus;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringUpdaterJson;

/**
 * <p>BGPPeering interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface BGPPeering {

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getConnectionUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getConnectionUuid();

    /**
     * <p>getConnectionName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getConnectionName();

    /**
     * <p>getVirtualDeviceUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getVirtualDeviceUuid();

    /**
     * <p>getLocalIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getLocalIpAddress();

    /**
     * <p>getRemoteIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getRemoteIpAddress();

    /**
     * <p>getLocalAsn.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getLocalAsn();

    /**
     * <p>getRemoteAsn.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getRemoteAsn();

    /**
     * <p>getAuthenticationKey.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getAuthenticationKey();

    /**
     * <p>getProvisioningStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BGPStatus} object.
     */
    BGPStatus getProvisioningStatus();

    /**
     * <p>getState.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BGPState} object.
     */
    BGPState getState();

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator.BGPPeeringUpdater} object.
     */
    BGPPeeringOperator.BGPPeeringUpdater update();

    /**
     * <p>save.</p>
     *
     * @param updaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringUpdaterJson} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean save(BGPPeeringUpdaterJson updaterJson);

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete();

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean refresh();
}
