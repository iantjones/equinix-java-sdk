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

package api.equinix.javasdk.networkedge.client.implementation;

import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.networkedge.client.Backups;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.internal.BackupClient;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.RestoreFeasibility;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.json.RestoreFeasibilityJson;
import api.equinix.javasdk.networkedge.model.json.creators.BackupOperator;
import api.equinix.javasdk.networkedge.model.wrappers.BackupWrapper;
import api.equinix.javasdk.networkedge.model.wrappers.RestoreFeasibilityWrapper;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class BackupsImpl implements Backups {

    private final BackupClient<Backup> serviceClient;

    private final NetworkEdge serviceManager;

    public PaginatedList<Backup> list(String deviceUuid) {
        return list(deviceUuid, null);
    }

    /**
     * {@inheritDoc}
     *
     */
    public PaginatedList<Backup> list(String deviceUuid, RequestBuilder.Backup requestBuilder) {
        Page<BackupJson> responsePage = serviceClient.list(deviceUuid, requestBuilder);
        PaginatedList<Backup> deviceList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, BackupWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Backup getByUuid(String uuid) {
        BackupJson deviceLinkJson = serviceClient.getByUuid(uuid);
        return new BackupWrapper(deviceLinkJson, this.serviceClient);
    }

    public String download(String uuid) {
        return serviceClient.download(uuid);
    }

    public RestoreFeasibility checkRestoreFeasibility(String uuid, String deviceUuid) {
        RestoreFeasibilityJson restoreFeasibilityJson = serviceClient.checkRestoreFeasibility(uuid, deviceUuid);
        return new RestoreFeasibilityWrapper(restoreFeasibilityJson, this.serviceClient);
    }

    public BackupOperator.BackupBuilder define(String deviceUuid, String backupName) {
        return new BackupOperator(this.serviceClient).create(deviceUuid, backupName);
    }
}
