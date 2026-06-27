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

import api.equinix.javasdk.networkedge.enums.LicenseType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;

/**
 * <p>LicenseOption class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class LicenseOption {

    @JsonProperty("type")
    private LicenseType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("fileUploadSupportedCluster")
    private Boolean fileUploadSupportedCluster;

    @JsonProperty("requireLicenseAtCreateCluster")
    private Boolean requireLicenseAtCreateCluster;

    @JsonProperty("smartLicenseSupported")
    private Boolean smartLicenseSupported;

    @JsonProperty("cores")
    private ArrayList<DeviceCore> cores;

    @JsonProperty("supported")
    private Boolean supported;

    @JsonProperty("day0TextFileRequired")
    private Boolean day0TextFileRequired;

    @JsonProperty("day0XmlFileRequired")
    private Boolean day0XmlFileRequired;
}
