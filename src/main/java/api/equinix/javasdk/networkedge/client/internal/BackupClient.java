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

package api.equinix.javasdk.networkedge.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.json.RestoreFeasibilityJson;
import api.equinix.javasdk.networkedge.model.json.creators.BackupCreatorJson;

/**
 *
 * @author ianjones
 */
public interface BackupClient<T> extends Pageable<T> {

    Page<Backup, BackupJson> list(String deviceUuid, RequestBuilder.Backup requestBuilder);

    BackupJson getByUuid(String uuid);

    RestoreFeasibilityJson checkRestoreFeasibility(String uuid, String deviceUuid);

    /**
     * <p>restore. Restores the backup identified by {@code uuid} (the backup uuid). The
     * spec requires the backup {@code name} in the request body.</p>
     *
     * @param uuid the unique identifier of the backup to restore.
     * @param name the name of the backup ({@code DeviceBackupUpdateRequest.name}).
     */
    Boolean restore(String uuid, String name);

    String download(String uuid);

    BackupJson create(BackupCreatorJson backupCreatorJson);

    Boolean delete(String uuid);

    BackupJson refresh(String uuid);
}
