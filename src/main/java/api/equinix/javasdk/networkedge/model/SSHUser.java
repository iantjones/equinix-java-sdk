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

import api.equinix.javasdk.networkedge.enums.SSHUserAction;
import api.equinix.javasdk.networkedge.model.json.SSHUserJson;

import java.util.List;
import java.util.Map;

/**
 * <p>SSHUser interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface SSHUser {

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getUsername.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUsername();

    /**
     * <p>getDeviceUuids.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<String> getDeviceUuids();

    /**
     * <p>getMetroStatusMap.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    Map<String, SSHUserJson.Status> getMetroStatusMap();

    /**
     * <p>getAction.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.SSHUserAction} object.
     */
    SSHUserAction getAction();

    /**
     * <p>addDevice.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean addDevice(String deviceUuid);

    /**
     * <p>updatePassword.</p>
     *
     * @param newPassword a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean updatePassword(String newPassword);

    /**
     * <p>deleteDevice.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean deleteDevice(String deviceUuid);

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
