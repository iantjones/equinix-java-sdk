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

package com.eqixiac.equinix.networkedge.model.implementation;

import com.eqixiac.equinix.networkedge.enums.DeviceManagementType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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

    // Spec DefaultAclsConfig is a single object ({dnsServers, ntpServers}), not an array.
    @JsonProperty("defaultAcls")
    private DefaultACL defaultAcls;

    /**
     * License options keyed by license type. The fields were package-private with no
     * {@code @JsonProperty}, so Jackson saw no properties and failed on the live {@code "SUB"} key
     * ({@code GET /ne/v1/deviceTypes}, checked 2026-11-11).
     */
    @Getter
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class LicenseOptions {
        @JsonProperty("SUB")
        private LicenseOption sub;

        @JsonProperty("BYOL")
        private LicenseOption byol;
    }
}
