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

package api.equinix.javasdk.networkedge.model.implementation;

import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;

/**
 * <p>DeviceManagement class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class DeviceManagement {

    @JsonProperty("type")
    private DeviceManagementType type;

    @JsonProperty("licenseOptions")
    private LicenseOptions licenseOptions;

    @JsonProperty("supportedServices")
    private ArrayList<SupportedService> supportedServices;

    @JsonProperty("additionalFields")
    private ArrayList<AdditionalField> additionalFields;

    @JsonProperty("clusteringDetails")
    private ClusteringDetail clusteringDetails;

    @JsonProperty("defaultAcls")
    private ArrayList<DefaultACL> defaultAcls;

    @JsonProperty("supported")
    private Boolean supported;

    @JsonProperty("metadata")
    private DeviceMetadata metadata;

    @Getter
    static class LicenseOptions {
            LicenseOption SUB;
            LicenseOption BYOL;
    }
}
