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

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.networkedge.client.Devices;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.internal.DeviceClient;
import api.equinix.javasdk.networkedge.client.internal.DeviceTypeClient;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import api.equinix.javasdk.networkedge.model.json.DeviceTypeJson;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceTypeWrapper;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceWrapper;
import lombok.Getter;

import java.util.List;

/**
 * <p>DevicesImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class DevicesImpl implements Devices {

    private final NetworkEdge serviceManager;

    private final DeviceClient<Device> serviceClient;

    private final DeviceTypeClient<DeviceType> deviceTypesServiceClient;

    /**
     * <p>Constructor for DevicesImpl.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.networkedge.client.internal.DeviceClient} object.
     * @param deviceTypesServiceClient a {@link api.equinix.javasdk.networkedge.client.internal.DeviceTypeClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.NetworkEdge} object.
     */
    public DevicesImpl(DeviceClient<Device> serviceClient,
                       DeviceTypeClient<DeviceType> deviceTypesServiceClient,
                       NetworkEdge serviceManager) {
        this.serviceManager = serviceManager;
        this.deviceTypesServiceClient = deviceTypesServiceClient;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<Device> list() {
        return list(null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>list.</p>
     */
    public PaginatedList<Device> list(RequestBuilder.Device requestBuilder) {
        Page<Device, DeviceJson> responsePage = serviceClient.list(requestBuilder);
        PaginatedList<Device> deviceList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, DeviceWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /**
     * <p>listDeviceTypes.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<DeviceType> listDeviceTypes() {
        Page<DeviceType, DeviceTypeJson> responsePage = deviceTypesServiceClient.list();
        PaginatedList<DeviceType> deviceTypeList = Utils.mapPaginatedList(responsePage.getItems(), this.deviceTypesServiceClient, DeviceTypeWrapper::new);
        return new PaginatedList<>(deviceTypeList, this.deviceTypesServiceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public List<NetworkInterface> listInterfaces(String uuid) {
        return serviceClient.listInterfaces(uuid);
    }

    /** {@inheritDoc} */
    public Device getByUuid(String uuid) {
        DeviceJson deviceJson = serviceClient.getByUuid(uuid);
        return new DeviceWrapper(deviceJson, this.serviceClient);
    }

    /**
     * {@inheritDoc}
     *
     * <p>define.</p>
     */
    public DeviceOperator.DeviceBuilder define(String deviceName) {
        return new DeviceOperator(this.serviceClient).create(deviceName);
    }

    /**
     * {@inheritDoc}
     *
     * <p>define.</p>
     */
    public DeviceOperator.DeviceBuilderSecondary defineSecondary(String secondaryDeviceName, String primaryDeviceUuid) {
        return new DeviceOperator(this.serviceClient).createRedundantDevice(secondaryDeviceName, primaryDeviceUuid);
    }

    /** {@inheritDoc} */
    public String postLicenseFile(MetroCode metroCode, String deviceTypeCode, LicenseType licenseType, String fileContents) {
        return this.serviceClient.postLicenseFile(metroCode, deviceTypeCode, licenseType, fileContents);
    }
}
