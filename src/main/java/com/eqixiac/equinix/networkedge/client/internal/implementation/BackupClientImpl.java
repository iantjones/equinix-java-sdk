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

package com.eqixiac.equinix.networkedge.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.BackupClient;
import com.eqixiac.equinix.networkedge.model.Backup;
import com.eqixiac.equinix.networkedge.model.implementation.UUIDResult;
import com.eqixiac.equinix.networkedge.model.json.BackupJson;
import com.eqixiac.equinix.networkedge.model.json.RestoreFeasibilityJson;
import com.eqixiac.equinix.networkedge.model.json.creators.BackupCreatorJson;
import com.eqixiac.equinix.networkedge.model.json.creators.BackupUpdaterJson;
import com.eqixiac.equinix.networkedge.model.wrappers.BackupWrapper;

import java.util.List;
import java.util.Map;

/**
 * Internal client for Network Edge device Backups. Plumbing/paging come from {@link ResourceClientBase};
 * Network Edge's non-standard contracts (create→UUIDResult→refetch, update→refetch, Boolean delete/restore,
 * String download, secondary restore-analysis type) use the generic helpers on {@code ClientBase}.
 *
 * @author ianjones
 */
public class BackupClientImpl extends ResourceClientBase<Backup, BackupJson> implements BackupClient<Backup> {

    public BackupClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Backups", BackupJson.class);
    }

    @Override
    protected Backup wrap(BackupJson json) {
        return new BackupWrapper(json, this);
    }

    public Page<BackupJson> list(String deviceUuid, RequestBuilder.Backup requestBuilder) {
        Map<String, List<String>> qParams = ParameterMapper.newMap(requestBuilder);
        qParams.put("virtualDeviceUuid", ParameterMapper.singleParamList(deviceUuid));
        return listPage("ListBackups", qParams);
    }

    public BackupJson getByUuid(String uuid) {
        return getOne("GetBackup", uuid);
    }

    public RestoreFeasibilityJson checkRestoreFeasibility(String uuid, String deviceUuid) {
        return getAs("GetRestoreAnalysis", Map.of("deviceUuid", deviceUuid),
                Map.of("backupUuid", ParameterMapper.singleParamList(uuid)), RestoreFeasibilityJson.class);
    }

    public Boolean restore(String uuid, String name) {
        // Per spec restoreDeviceBackupByUuid: PATCH /ne/v1/devices/{uuid}/restore where {uuid} is the
        // BACKUP uuid; the body is DeviceBackupUpdateRequest (required name). No query parameter.
        return booleanOp("RestoreBackup", RequestType.SINGLE, Map.of("backupUuid", uuid),
                null, ParameterMapper.singlePropertyBody("name", name));
    }

    public String download(String uuid) {
        return stringOp("DownloadBackup", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public BackupJson create(BackupCreatorJson backupCreatorJson) {
        UUIDResult result = postForType("CreateBackup", backupCreatorJson, BackupJson.getCreateTypeRef());
        return getByUuid(result.getUuid());
    }

    public BackupJson update(String uuid, BackupUpdaterJson backupUpdaterJson) {
        voidOp("UpdateBackup", RequestType.SINGLE, Map.of("uuid", uuid), null, backupUpdaterJson);
        return getByUuid(uuid);
    }

    public Boolean delete(String uuid) {
        return booleanOp("DeleteBackup", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public BackupJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
