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
 *
 * @author ianjones
 */
public interface Backup {

    String getUuid();

    String getName();

    BackupType getType();

    BackupStatus getStatus();

    BackupRequestType getRequestType();

    String getDownloadUrl();

    String getVersion();

    Boolean getDeleteAllowed();

    List<DeviceRestore> getRestores();

    /**
     * Restores this backup. The restore endpoint is keyed by the backup uuid only, so the restore
     * always targets the device this backup was taken from; there is no way to choose a different
     * target device.
     *
     */
    Boolean restore();

    /**
     *
     * @deprecated the restore endpoint is keyed by the backup uuid only; the supplied {@code device}
     *             is ignored. Use {@link #restore()} instead.
     */
    @Deprecated
    Boolean restoreToDevice(Device device);

    /**
     *
     * @deprecated the restore endpoint is keyed by the backup uuid only; the supplied
     *             {@code deviceUuid} is ignored. Use {@link #restore()} instead.
     */
    @Deprecated
    Boolean restoreToDevice(String deviceUuid);

    BackupOperator.BackupUpdater update();

    Boolean save(BackupUpdaterJson updaterJson);

    Boolean delete();

    Boolean refresh();
}
