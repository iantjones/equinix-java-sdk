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
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.DeviceClient;
import com.eqixiac.equinix.networkedge.enums.LicenseType;
import com.eqixiac.equinix.networkedge.model.Device;
import com.eqixiac.equinix.networkedge.model.implementation.AllowedInterfaceResponse;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceACL;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceReboot;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceRebootHistory;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceUpgrade;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceUpgradeHistory;
import com.eqixiac.equinix.networkedge.model.implementation.DownloadableImage;
import com.eqixiac.equinix.networkedge.model.implementation.ImageDownload;
import com.eqixiac.equinix.networkedge.model.implementation.InterfaceStats;
import com.eqixiac.equinix.networkedge.model.implementation.NetworkInterface;
import com.eqixiac.equinix.networkedge.model.implementation.UUIDResult;
import com.eqixiac.equinix.networkedge.model.json.DeviceJson;
import com.eqixiac.equinix.networkedge.model.json.Pricing;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceACLRequest;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceCreatorJson;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceRMARequest;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceUpdaterJson;
import com.eqixiac.equinix.networkedge.model.wrappers.DeviceWrapper;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class DeviceClientImpl extends ResourceClientBase<Device, DeviceJson> implements DeviceClient<Device> {

    public DeviceClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Devices", DeviceJson.class);
    }

    @Override
    protected Device wrap(DeviceJson json) {
        return new DeviceWrapper(json, this);
    }

    /**
     * {@inheritDoc}
     *
     */
    public Page<DeviceJson> list(RequestBuilder.Device requestBuilder) {
        Map<String, List<String>> qParams = ParameterMapper.newMap(requestBuilder);
        return listPage("ListDevices", qParams);
    }

    public DeviceJson getByUuid(String uuid) {
        return getOne("GetDevice", uuid);
    }

    public List<NetworkInterface> listInterfaces(String uuid) {
        return listAs("GetNetworkInterfaces", Map.of("uuid", uuid), null, NetworkInterface.class);
    }

    public AllowedInterfaceResponse getAllowedInterfaces(String deviceType, Map<String, List<String>> queryParams) {
        return getAs("GetAllowedInterfaces", Map.of("deviceType", deviceType), queryParams, AllowedInterfaceResponse.class);
    }

    public List<DeviceReboot> listReloadHistory(String uuid) {
        DeviceRebootHistory history = getAs("ListReloadHistory", Map.of("uuid", uuid), null, DeviceRebootHistory.class);
        return history != null ? history.getData() : null;
    }

    public List<DeviceUpgrade> listUpgradeHistory(String uuid) {
        DeviceUpgradeHistory history = getAs("ListUpgradeHistory", Map.of("uuid", uuid), null, DeviceUpgradeHistory.class);
        return history != null ? history.getData() : null;
    }

    public InterfaceStats getInterfaceStatistics(String uuid, String interfaceId, String startDateTime, String endDateTime) {
        Map<String, List<String>> qParams = new java.util.HashMap<>();
        if (startDateTime != null) {
            qParams.put("startDateTime", ParameterMapper.singleParamList(startDateTime));
        }
        if (endDateTime != null) {
            qParams.put("endDateTime", ParameterMapper.singleParamList(endDateTime));
        }
        return getAs("GetInterfaceStatistics", Map.of("uuid", uuid, "interfaceId", interfaceId),
                qParams.isEmpty() ? null : qParams, InterfaceStats.class);
    }

    public List<DownloadableImage> listDownloadableImages(String deviceType) {
        return listAs("ListDownloadableImages", Map.of("deviceType", deviceType), null, DownloadableImage.class);
    }

    public ImageDownload requestImageDownload(String deviceType, String version) {
        return postForType("RequestImageDownload", Map.of("deviceType", deviceType, "version", version),
                null, ImageDownload.getResponseTypeRef());
    }

    public Boolean restore(String backupUuid, String backupName) {
        // Per spec restoreDeviceBackupByUuid: PATCH /ne/v1/devices/{uuid}/restore where {uuid} is the
        // BACKUP uuid; the body is DeviceBackupUpdateRequest (required name). No query parameter.
        return booleanOp("RestoreBackup", RequestType.SINGLE, Map.of("uuid", backupUuid),
                null, ParameterMapper.singlePropertyBody("name", backupName));
    }

    public DeviceJson updateAdditionalBandwidth(String uuid, Integer additionalBandwidth) {
        voidOp("UpdateAdditionalBandwidth", RequestType.SINGLE, Map.of("uuid", uuid), null,
                ParameterMapper.singlePropertyBody("additionalBandwidth", additionalBandwidth));
        return getByUuid(uuid);
    }

    public Boolean softReboot(String uuid) {
        return booleanOp("SoftRebootDevice", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public Boolean createRMA(String uuid, DeviceRMARequest deviceRMARequest) {
        return booleanOp("CreateDeviceRMA", RequestType.SINGLE, Map.of("uuid", uuid), null, deviceRMARequest);
    }

    public DeviceACL getACL(String uuid) {
        return getAs("GetDeviceACL", Map.of("uuid", uuid), null, DeviceACL.class);
    }

    public DeviceACL addACL(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId) {
        voidOp("AddDeviceACL", RequestType.SINGLE, Map.of("uuid", uuid),
                ParameterMapper.singleParamMap("accountUcmId", accountUcmId), deviceACLRequest);
        return getACL(uuid);
    }

    public DeviceACL updateACL(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId) {
        voidOp("UpdateDeviceACL", RequestType.SINGLE, Map.of("uuid", uuid),
                ParameterMapper.singleParamMap("accountUcmId", accountUcmId), deviceACLRequest);
        return getACL(uuid);
    }

    public Boolean ping(String uuid) {
        return booleanOp("PingDevice", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public String postLicenseFile(MetroCode metroCode, String deviceTypeCode, LicenseType licenseType, String fileContents) {
        Map<String, List<String>> qParams = Map.of("metroCode", ParameterMapper.singleParamList(metroCode),
                "deviceTypeCode", ParameterMapper.singleParamList(deviceTypeCode),
                "licenseType", ParameterMapper.singleParamList(licenseType));
        return mapOp("PostLicense", RequestType.SINGLE, null, qParams, Map.of("file", fileContents)).get("fileId");
    }

    public String postLicenseFile(String deviceUuid, String fileContents) {
        return mapOp("PostLicenseToDevice", RequestType.SINGLE, Map.of("uuid", deviceUuid), null,
                Map.of("file", fileContents)).get("fileId");
    }

    public String updateLicenseToken(String deviceUuid, String licenseToken) {
        return mapOp("UpdateLicenseToken", RequestType.SINGLE, Map.of("uuid", deviceUuid), null,
                Map.of("token", licenseToken)).get("fileId");
    }

    public Pricing getPricing(String deviceUuid) {
        return getAs("GetPricing", null, ParameterMapper.singleParamMap("virtualDeviceUuid", deviceUuid), Pricing.class);
    }

    public DeviceJson create(DeviceCreatorJson deviceCreatorJson, Boolean draft) {
        UUIDResult uuidResult = postForType("CreateDevice", null, ParameterMapper.singleParamMap("draft", draft),
                deviceCreatorJson, DeviceJson.getCreateTypeRef());
        return getByUuid(uuidResult.getUuid());
    }

    public DeviceJson update(String uuid, DeviceUpdaterJson deviceUpdaterJson) {
        voidOp("UpdateDevice", RequestType.SINGLE, Map.of("uuid", uuid), null, deviceUpdaterJson);
        return getByUuid(uuid);
    }

    public Boolean delete(String uuid) {
        return booleanOp("DeleteDevice", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public DeviceJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
