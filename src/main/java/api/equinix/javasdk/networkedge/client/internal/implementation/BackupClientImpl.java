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

package api.equinix.javasdk.networkedge.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.BackupClient;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.json.RestoreFeasibilityJson;
import api.equinix.javasdk.networkedge.model.json.creators.BackupCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.BackupUpdaterJson;
import api.equinix.javasdk.networkedge.model.wrappers.BackupWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>BackupClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class BackupClientImpl extends PageableBase implements BackupClient<Backup> {

    /**
     * <p>Constructor for BackupClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public BackupClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Backups");
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public Page<Backup, BackupJson> list(String deviceUuid, RequestBuilder.Backup requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        qParams.put("virtualDeviceUuid", Utils.singleParamList(deviceUuid));
        
        EquinixRequest<Backup> equinixRequest = this.buildRequest("ListBackups", RequestType.PAGINATED, null, qParams, BackupJson.getPagedTypeRef());
        EquinixResponse<Backup> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public BackupJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<BackupJson> equinixRequest = this.buildRequestWithPathParams("GetBackup", RequestType.SINGLE, pParams, BackupJson.getSingleTypeRef());
        EquinixResponse<BackupJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public RestoreFeasibilityJson checkRestoreFeasibility(String uuid, String deviceUuid) {
        Map<String, List<String>> qParams = Map.of("backupUuid", Utils.singleParamList(uuid));
        Map<String, String> pParams = Map.of("deviceUuid", deviceUuid);
        EquinixRequest<BackupJson> equinixRequest = this.buildRequest("GetRestoreAnalysis", RequestType.SINGLE, pParams, qParams, RestoreFeasibilityJson.getSingleTypeRef());
        EquinixResponse<BackupJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public Boolean restore(String uuid, String deviceUuid) {
        Map<String, List<String>> qParams = Map.of("backupUuid", Utils.singleParamList(uuid));
        Map<String, String> pParams = Map.of("deviceUuid", deviceUuid);
        EquinixRequest<BackupJson> equinixRequest = this.buildRequest("RestoreBackup", RequestType.SINGLE, pParams, qParams);
        EquinixResponse<BackupJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public String download(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<BackupJson> equinixRequest = this.buildRequestWithPathParams("DownloadBackup", RequestType.SINGLE, pParams);
        EquinixResponse<BackupJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleStringResponse(equinixResponse);
    }

    /** {@inheritDoc} */
    public BackupJson create(BackupCreatorJson deviceLinkCreatorJson) {
        EquinixRequest<BackupJson> equinixRequest = this.buildRequest("CreateBackup", RequestType.SINGLE, BackupJson.getCreateTypeRef());
        Utils.serializeJson(equinixRequest, deviceLinkCreatorJson);
        EquinixResponse<BackupJson> equinixResponse = this.invoke(equinixRequest);
        UUIDResult uuidResult = Utils.handleSingletonResponse(equinixResponse, equinixRequest);
        return getByUuid(uuidResult.getUuid());
    }

    /**
     * <p>update.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param deviceLinkUpdaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.BackupUpdaterJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BackupJson} object.
     */
    public BackupJson update(String uuid, BackupUpdaterJson deviceLinkUpdaterJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<BackupJson> equinixRequest = this.buildRequestWithPathParams("UpdateBackup", RequestType.SINGLE, pParams);
        Utils.serializeJson(equinixRequest, deviceLinkUpdaterJson);
        EquinixResponse<BackupJson> equinixResponse = this.invoke(equinixRequest);
        return getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public Boolean delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<Backup> equinixRequest = this.buildRequestWithPathParams("DeleteBackup", RequestType.SINGLE, pParams);
        EquinixResponse<Backup> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public BackupJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<Backup> nextPage(PaginatedRequest<Backup> equinixRequest) {
        EquinixResponse<Backup> equinixResponse = this.invoke(equinixRequest);
        Page<Backup, BackupJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Backup> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, BackupWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
