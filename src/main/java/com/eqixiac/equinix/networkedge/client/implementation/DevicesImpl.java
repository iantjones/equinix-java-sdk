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

package com.eqixiac.equinix.networkedge.client.implementation;

import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.networkedge.client.Devices;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.client.internal.DeviceClient;
import com.eqixiac.equinix.networkedge.client.internal.DeviceTypeClient;
import com.eqixiac.equinix.networkedge.enums.LicenseType;
import com.eqixiac.equinix.networkedge.model.Device;
import com.eqixiac.equinix.networkedge.model.DeviceType;
import com.eqixiac.equinix.networkedge.model.implementation.AllowedInterfaceResponse;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceACL;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceReboot;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceUpgrade;
import com.eqixiac.equinix.networkedge.model.implementation.DownloadableImage;
import com.eqixiac.equinix.networkedge.model.implementation.ImageDownload;
import com.eqixiac.equinix.networkedge.model.implementation.InterfaceStats;
import com.eqixiac.equinix.networkedge.model.implementation.NetworkInterface;
import com.eqixiac.equinix.networkedge.model.json.DeviceJson;
import com.eqixiac.equinix.networkedge.model.json.DeviceTypeJson;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceACLRequest;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceOperator;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceRMARequest;
import com.eqixiac.equinix.networkedge.model.wrappers.DeviceTypeWrapper;
import com.eqixiac.equinix.networkedge.model.wrappers.DeviceWrapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class DevicesImpl implements Devices {

    private final DeviceClient<Device> serviceClient;

    private final DeviceTypeClient<DeviceType> deviceTypesServiceClient;

    private final NetworkEdge serviceManager;

    public PaginatedList<Device> list() {
        return list(null);
    }

    /**
     * {@inheritDoc}
     *
     */
    public PaginatedList<Device> list(RequestBuilder.Device requestBuilder) {
        Page<DeviceJson> responsePage = serviceClient.list(requestBuilder);
        PaginatedList<Device> deviceList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, DeviceWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedList<DeviceType> listDeviceTypes() {
        Page<DeviceTypeJson> responsePage = deviceTypesServiceClient.list();
        PaginatedList<DeviceType> deviceTypeList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.deviceTypesServiceClient, DeviceTypeWrapper::new);
        return new PaginatedList<>(deviceTypeList, this.deviceTypesServiceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public List<NetworkInterface> listInterfaces(String uuid) {
        return serviceClient.listInterfaces(uuid);
    }

    public AllowedInterfaceResponse listAllowedInterfaces(RequestBuilder.AllowedInterfaces requestBuilder) {
        return serviceClient.getAllowedInterfaces(requestBuilder.getDeviceType(), ParameterMapper.newMap(requestBuilder));
    }

    public List<DeviceReboot> listReloadHistory(String uuid) {
        return serviceClient.listReloadHistory(uuid);
    }

    public List<DeviceUpgrade> listUpgradeHistory(String uuid) {
        return serviceClient.listUpgradeHistory(uuid);
    }

    public InterfaceStats getInterfaceStatistics(String uuid, String interfaceId, String startDateTime, String endDateTime) {
        return serviceClient.getInterfaceStatistics(uuid, interfaceId, startDateTime, endDateTime);
    }

    public List<DownloadableImage> listDownloadableImages(String deviceType) {
        return serviceClient.listDownloadableImages(deviceType);
    }

    public ImageDownload requestImageDownload(String deviceType, String version) {
        return serviceClient.requestImageDownload(deviceType, version);
    }

    public Device getByUuid(String uuid) {
        DeviceJson deviceJson = serviceClient.getByUuid(uuid);
        return new DeviceWrapper(deviceJson, this.serviceClient);
    }

    /**
     * {@inheritDoc}
     *
     */
    public DeviceOperator.DeviceBuilder define(String deviceName) {
        return new DeviceOperator(this.serviceClient).create(deviceName);
    }

    /**
     * {@inheritDoc}
     *
     */
    public DeviceOperator.DeviceBuilderSecondary defineSecondary(String secondaryDeviceName, String primaryDeviceUuid) {
        return new DeviceOperator(this.serviceClient).createRedundantDevice(secondaryDeviceName, primaryDeviceUuid);
    }

    public String postLicenseFile(MetroCode metroCode, String deviceTypeCode, LicenseType licenseType, String fileContents) {
        return this.serviceClient.postLicenseFile(metroCode, deviceTypeCode, licenseType, fileContents);
    }

    public Boolean softReboot(String uuid) {
        return this.serviceClient.softReboot(uuid);
    }

    public Boolean createRMA(String uuid, DeviceRMARequest deviceRMARequest) {
        return this.serviceClient.createRMA(uuid, deviceRMARequest);
    }

    public DeviceACL getDeviceAcl(String uuid) {
        return this.serviceClient.getACL(uuid);
    }

    public DeviceACL addDeviceAcl(String uuid, DeviceACLRequest deviceACLRequest) {
        return this.serviceClient.addACL(uuid, deviceACLRequest, null);
    }

    public DeviceACL addDeviceAcl(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId) {
        return this.serviceClient.addACL(uuid, deviceACLRequest, accountUcmId);
    }

    public DeviceACL updateDeviceAcl(String uuid, DeviceACLRequest deviceACLRequest) {
        return this.serviceClient.updateACL(uuid, deviceACLRequest, null);
    }

    public DeviceACL updateDeviceAcl(String uuid, DeviceACLRequest deviceACLRequest, String accountUcmId) {
        return this.serviceClient.updateACL(uuid, deviceACLRequest, accountUcmId);
    }
}
