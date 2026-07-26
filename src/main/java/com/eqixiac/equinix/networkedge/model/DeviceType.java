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

package com.eqixiac.equinix.networkedge.model;

import com.eqixiac.equinix.networkedge.enums.DeviceCategory;
import com.eqixiac.equinix.networkedge.enums.Vendor;
import com.eqixiac.equinix.networkedge.model.json.MetroJson;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceManagementTypes;
import com.eqixiac.equinix.networkedge.model.implementation.SoftwarePackage;

import java.util.ArrayList;

/**
 *
 * @author ianjones
 */
public interface DeviceType {

    String getDeviceTypeCode();

    String getName();

    String getDescription();

    Vendor getVendor();

    DeviceCategory getCategory();

    Integer getMaxInterfaceCount();

    Integer getDefaultInterfaceCount();

    Integer getClusterMaxInterfaceCount();

    Integer getClusterDefaultInterfaceCount();

    ArrayList<MetroJson> getAvailableMetros();

    ArrayList<SoftwarePackage> getSoftwarePackages();

    DeviceManagementTypes getDeviceManagementTypes();
}
