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

import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.Vendor;
import api.equinix.javasdk.networkedge.model.json.MetroJson;
import api.equinix.javasdk.networkedge.model.implementation.DeviceManagementTypes;
import api.equinix.javasdk.networkedge.model.implementation.SoftwarePackage;

import java.util.ArrayList;

/**
 * <p>DeviceType interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface DeviceType {

    /**
     * <p>getDeviceTypeCode.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDeviceTypeCode();

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getName();

    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDescription();

    /**
     * <p>getVendor.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.Vendor} object.
     */
    Vendor getVendor();

    /**
     * <p>getCategory.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceCategory} object.
     */
    DeviceCategory getCategory();

    /**
     * <p>getMaxInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getMaxInterfaceCount();

    /**
     * <p>getDefaultInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getDefaultInterfaceCount();

    /**
     * <p>getClusterMaxInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getClusterMaxInterfaceCount();

    /**
     * <p>getClusterDefaultInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getClusterDefaultInterfaceCount();

    /**
     * <p>getAvailableMetros.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    ArrayList<MetroJson> getAvailableMetros();

    /**
     * <p>getSoftwarePackages.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    ArrayList<SoftwarePackage> getSoftwarePackages();

    /**
     * <p>getDeviceManagementTypes.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceManagementTypes} object.
     */
    DeviceManagementTypes getDeviceManagementTypes();
}
