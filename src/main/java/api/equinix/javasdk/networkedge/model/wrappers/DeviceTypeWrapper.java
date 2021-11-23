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

package api.equinix.javasdk.networkedge.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.Vendor;
import api.equinix.javasdk.networkedge.model.json.MetroJson;
import api.equinix.javasdk.networkedge.model.implementation.DeviceManagementTypes;
import api.equinix.javasdk.networkedge.model.implementation.SoftwarePackage;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.json.DeviceTypeJson;
import lombok.Getter;

import java.util.ArrayList;

/**
 * <p>DeviceTypeWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceTypeWrapper extends ResourceImpl<DeviceType> implements DeviceType {

    private final DeviceTypeJson jsonObject;
    @Getter
    private final Pageable<DeviceType> serviceClient;

    /**
     * <p>Constructor for DeviceTypeWrapper.</p>
     *
     * @param deviceTypeJson a {@link api.equinix.javasdk.networkedge.model.json.DeviceTypeJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public DeviceTypeWrapper(DeviceTypeJson deviceTypeJson, Pageable<DeviceType> serviceClient) {
        this.jsonObject = deviceTypeJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getDeviceTypeCode.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDeviceTypeCode() {
        return jsonObject.getDeviceTypeCode();
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return jsonObject.getName();
    }

    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        return jsonObject.getDescription();
    }

    /**
     * <p>getVendor.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.Vendor} object.
     */
    public Vendor getVendor() {
        return jsonObject.getVendor();
    }

    /**
     * <p>getCategory.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceCategory} object.
     */
    public DeviceCategory getCategory() {
        return jsonObject.getCategory();
    }

    /**
     * <p>getMaxInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getMaxInterfaceCount() {
        return jsonObject.getMaxInterfaceCount();
    }

    /**
     * <p>getDefaultInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getDefaultInterfaceCount() {
        return jsonObject.getDefaultInterfaceCount();
    }

    /**
     * <p>getClusterMaxInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getClusterMaxInterfaceCount() {
        return jsonObject.getClusterMaxInterfaceCount();
    }

    /**
     * <p>getClusterDefaultInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getClusterDefaultInterfaceCount() {
        return jsonObject.getClusterDefaultInterfaceCount();
    }

    /**
     * <p>getAvailableMetros.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<MetroJson> getAvailableMetros() {
        return jsonObject.getAvailableMetros();
    }

    /**
     * <p>getSoftwarePackages.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<SoftwarePackage> getSoftwarePackages() {
        return jsonObject.getSoftwarePackages();
    }

    /**
     * <p>getDeviceManagementTypes.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceManagementTypes} object.
     */
    public DeviceManagementTypes getDeviceManagementTypes() {
        return jsonObject.getDeviceManagementTypes();
    }
}
