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

import api.equinix.javasdk.networkedge.enums.BackupRequestType;
import api.equinix.javasdk.networkedge.enums.BackupStatus;
import api.equinix.javasdk.networkedge.enums.BackupType;
import api.equinix.javasdk.networkedge.model.implementation.DeviceRestore;
import api.equinix.javasdk.networkedge.model.json.creators.BackupOperator;
import api.equinix.javasdk.networkedge.model.json.creators.BackupUpdaterJson;

import java.util.List;

/**
 * <p>Backup interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Backup {

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getName();

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BackupType} object.
     */
    BackupType getType();

    /**
     * <p>getStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BackupStatus} object.
     */
    BackupStatus getStatus();

    /**
     * <p>getRequestType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.BackupRequestType} object.
     */
    BackupRequestType getRequestType();

    /**
     * <p>getDownloadUrl.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDownloadUrl();

    /**
     * <p>getVersion.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getVersion();

    /**
     * <p>getDeleteAllowed.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean getDeleteAllowed();

    /**
     * <p>getRestores.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<DeviceRestore> getRestores();

    /**
     * <p>restoreToDevice.</p>
     *
     * @param device a {@link api.equinix.javasdk.networkedge.model.Device} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean restoreToDevice(Device device);

    /**
     * <p>restoreToDevice.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean restoreToDevice(String deviceUuid);

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BackupOperator.BackupUpdater} object.
     */
    BackupOperator.BackupUpdater update();

    /**
     * <p>save.</p>
     *
     * @param updaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.BackupUpdaterJson} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean save(BackupUpdaterJson updaterJson);

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
