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

import api.equinix.javasdk.networkedge.model.json.creators.VPNOperator;
import api.equinix.javasdk.networkedge.model.json.creators.VPNUpdaterJson;

/**
 * <p>VPN interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface VPN {

    /**
     * <p>getSecondary.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.VPN} object.
     */
    VPN getSecondary();

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getSiteName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSiteName();

    /**
     * <p>getVirtualDeviceUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getVirtualDeviceUuid();

    /**
     * <p>getConfigName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getConfigName();

    /**
     * <p>getPeerIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPeerIp();

    /**
     * <p>getPeerSharedKey.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPeerSharedKey();

    /**
     * <p>getRemoteAsn.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getRemoteAsn();

    /**
     * <p>getRemoteIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getRemoteIpAddress();

    /**
     * <p>getPassword.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPassword();

    /**
     * <p>getLocalAsn.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getLocalAsn();

    /**
     * <p>getProjectId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getProjectId();

    /**
     * <p>getTunnelIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getTunnelIp();

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.VPNOperator.VPNUpdater} object.
     */
    VPNOperator.VPNUpdater update();

    /**
     * <p>save.</p>
     *
     * @param updaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.VPNUpdaterJson} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean save(VPNUpdaterJson updaterJson);

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
