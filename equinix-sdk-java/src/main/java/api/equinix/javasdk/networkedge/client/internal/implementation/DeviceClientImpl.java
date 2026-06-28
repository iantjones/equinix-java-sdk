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

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.DeviceClient;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.AllowedInterfaceResponse;
import api.equinix.javasdk.networkedge.model.implementation.DeviceACL;
import api.equinix.javasdk.networkedge.model.implementation.DeviceReboot;
import api.equinix.javasdk.networkedge.model.implementation.DeviceRebootHistory;
import api.equinix.javasdk.networkedge.model.implementation.DeviceUpgrade;
import api.equinix.javasdk.networkedge.model.implementation.DeviceUpgradeHistory;
import api.equinix.javasdk.networkedge.model.implementation.DownloadableImage;
import api.equinix.javasdk.networkedge.model.implementation.ImageDownload;
import api.equinix.javasdk.networkedge.model.implementation.InterfaceStats;
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import api.equinix.javasdk.networkedge.model.json.Pricing;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>DeviceClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceClientImpl extends ResourceClientBase<Device, DeviceJson> implements DeviceClient<Device> {

    /**
     * <p>Constructor for DeviceClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public DeviceClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Devices", DeviceJson.class);
    }

    /** {@inheritDoc} */
    @Override
    protected Device wrap(DeviceJson json) {
        return new DeviceWrapper(json, this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public Page<Device, DeviceJson> list(RequestBuilder.Device requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        return listPage("ListDevices", qParams);
    }

    /** {@inheritDoc} */
    public DeviceJson getByUuid(String uuid) {
        return getOne("GetDevice", uuid);
    }

    /** {@inheritDoc} */
    public List<NetworkInterface> listInterfaces(String uuid) {
        return listAs("GetNetworkInterfaces", Map.of("uuid", uuid), null, NetworkInterface.class);
    }

    /** {@inheritDoc} */
    public AllowedInterfaceResponse getAllowedInterfaces(String deviceType, Map<String, List<String>> queryParams) {
        return getAs("GetAllowedInterfaces", Map.of("deviceType", deviceType), queryParams, AllowedInterfaceResponse.class);
    }

    /** {@inheritDoc} */
    public List<DeviceReboot> listReloadHistory(String uuid) {
        DeviceRebootHistory history = getAs("ListReloadHistory", Map.of("uuid", uuid), null, DeviceRebootHistory.class);
        return history != null ? history.getData() : null;
    }

    /** {@inheritDoc} */
    public List<DeviceUpgrade> listUpgradeHistory(String uuid) {
        DeviceUpgradeHistory history = getAs("ListUpgradeHistory", Map.of("uuid", uuid), null, DeviceUpgradeHistory.class);
        return history != null ? history.getData() : null;
    }

    /** {@inheritDoc} */
    public InterfaceStats getInterfaceStatistics(String uuid, String interfaceId, String startDateTime, String endDateTime) {
        Map<String, List<String>> qParams = new java.util.HashMap<>();
        if (startDateTime != null) {
            qParams.put("startDateTime", Utils.singleParamList(startDateTime));
        }
        if (endDateTime != null) {
            qParams.put("endDateTime", Utils.singleParamList(endDateTime));
        }
        return getAs("GetInterfaceStatistics", Map.of("uuid", uuid, "interfaceId", interfaceId),
                qParams.isEmpty() ? null : qParams, InterfaceStats.class);
    }

    /** {@inheritDoc} */
    public List<DownloadableImage> listDownloadableImages(String deviceType) {
        return listAs("ListDownloadableImages", Map.of("deviceType", deviceType), null, DownloadableImage.class);
    }

    /** {@inheritDoc} */
    public ImageDownload requestImageDownload(String deviceType, String version) {
        return postForType("RequestImageDownload", Map.of("deviceType", deviceType, "version", version),
                null, ImageDownload.getResponseTypeRef());
    }

    /** {@inheritDoc} */
    public Boolean restore(String backupUuid, String backupName) {
        // Per spec restoreDeviceBackupByUuid: PATCH /ne/v1/devices/{uuid}/restore where {uuid} is the
        // BACKUP uuid; the body is DeviceBackupUpdateRequest (required name). No query parameter.
        return booleanOp("RestoreBackup", RequestType.SINGLE, Map.of("uuid", backupUuid),
                null, Utils.singlePropertyBody("name", backupName));
    }

    /** {@inheritDoc} */
    public DeviceJson updateAdditionalBandwidth(String uuid, Integer additionalBandwidth) {
        voidOp("UpdateAdditionalBandwidth", RequestType.SINGLE, Map.of("uuid", uuid), null,
                Utils.singlePropertyBody("additionalBandwidth", additionalBandwidth));
        return getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public Boolean softReboot(String uuid) {
        return booleanOp("SoftRebootDevice", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    /** {@inheritDoc} */
    public Boolean createRMA(String uuid, DeviceRMARequest deviceRMARequest) {
        return booleanOp("CreateDeviceRMA", RequestType.SINGLE, Map.of("uuid", uuid), null, deviceRMARequest);
    }

    /** {@inheritDoc} */
    public DeviceACL getACL(String uuid) {
        return getAs("GetDeviceACL", Map.of("uuid", uuid), null, DeviceACL.class);
    }

    /** {@inheritDoc} */
    public DeviceACL addACL(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId) {
        voidOp("AddDeviceACL", RequestType.SINGLE, Map.of("uuid", uuid),
                Utils.singleParamMap("accountUcmId", accountUcmId), deviceACLRequest);
        return getACL(uuid);
    }

    /** {@inheritDoc} */
    public DeviceACL updateACL(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId) {
        voidOp("UpdateDeviceACL", RequestType.SINGLE, Map.of("uuid", uuid),
                Utils.singleParamMap("accountUcmId", accountUcmId), deviceACLRequest);
        return getACL(uuid);
    }

    /** {@inheritDoc} */
    public Boolean ping(String uuid) {
        return booleanOp("PingDevice", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    /** {@inheritDoc} */
    public String postLicenseFile(MetroCode metroCode, String deviceTypeCode, LicenseType licenseType, String fileContents) {
        Map<String, List<String>> qParams = Map.of("metroCode", Utils.singleParamList(metroCode),
                "deviceTypeCode", Utils.singleParamList(deviceTypeCode),
                "licenseType", Utils.singleParamList(licenseType));
        return mapOp("PostLicense", RequestType.SINGLE, null, qParams, Map.of("file", fileContents)).get("fileId");
    }

    /** {@inheritDoc} */
    public String postLicenseFile(String deviceUuid, String fileContents) {
        return mapOp("PostLicenseToDevice", RequestType.SINGLE, Map.of("uuid", deviceUuid), null,
                Map.of("file", fileContents)).get("fileId");
    }

    /** {@inheritDoc} */
    public String updateLicenseToken(String deviceUuid, String licenseToken) {
        return mapOp("UpdateLicenseToken", RequestType.SINGLE, Map.of("uuid", deviceUuid), null,
                Map.of("token", licenseToken)).get("fileId");
    }

    /**
     * <p>getPricing.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    public Pricing getPricing(String deviceUuid) {
        return getAs("GetPricing", null, Utils.singleParamMap("virtualDeviceUuid", deviceUuid), Pricing.class);
    }

    /** {@inheritDoc} */
    public DeviceJson create(DeviceCreatorJson deviceCreatorJson, Boolean draft) {
        UUIDResult uuidResult = postForType("CreateDevice", null, Utils.singleParamMap("draft", draft),
                deviceCreatorJson, DeviceJson.getCreateTypeRef());
        return getByUuid(uuidResult.getUuid());
    }

    /** {@inheritDoc} */
    public DeviceJson update(String uuid, DeviceUpdaterJson deviceUpdaterJson) {
        voidOp("UpdateDevice", RequestType.SINGLE, Map.of("uuid", uuid), null, deviceUpdaterJson);
        return getByUuid(uuid);
    }

    /** {@inheritDoc} */
    public Boolean delete(String uuid) {
        return booleanOp("DeleteDevice", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    /** {@inheritDoc} */
    public DeviceJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
