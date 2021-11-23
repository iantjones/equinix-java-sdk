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

package api.equinix.javasdk.networkedge.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.Vendor;
import api.equinix.javasdk.networkedge.model.implementation.DeviceManagementTypes;
import api.equinix.javasdk.networkedge.model.implementation.SoftwarePackage;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.ArrayList;

/**
 * <p>DeviceTypeJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class DeviceTypeJson {

    /**
     * <p>pagedTypeRef.</p>
     *
     * @return a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    public static TypeReference<Page<DeviceJson, DeviceTypeJson>> pagedTypeRef() {
        return new TypeReference<>() {};
    }

    /**
     * <p>singleTypeRef.</p>
     *
     * @return a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    public static TypeReference<DeviceTypeJson> singleTypeRef() {
        return new TypeReference<>() {};
    }

    @JsonProperty("deviceTypeCode")
    private String deviceTypeCode;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("vendor")
    private Vendor vendor;

    @JsonProperty("category")
    private DeviceCategory category;

    @JsonProperty("maxInterfaceCount")
    private Integer maxInterfaceCount;

    @JsonProperty("defaultInterfaceCount")
    private Integer defaultInterfaceCount;

    @JsonProperty("clusterMaxInterfaceCount")
    private Integer clusterMaxInterfaceCount;

    @JsonProperty("clusterDefaultInterfaceCount")
    private Integer clusterDefaultInterfaceCount;

    @JsonProperty("availableMetros")
    private ArrayList<MetroJson> availableMetros;

    @JsonProperty("softwarePackages")
    private ArrayList<SoftwarePackage> softwarePackages;

    @JsonProperty("deviceManagementTypes")
    private DeviceManagementTypes deviceManagementTypes;
}
