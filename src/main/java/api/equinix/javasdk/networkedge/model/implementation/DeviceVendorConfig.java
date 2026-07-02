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

import api.equinix.javasdk.networkedge.enums.CvpType;
import api.equinix.javasdk.networkedge.enums.VendorManagementType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DeviceVendorConfig {

    @JsonProperty("siteId")
    private String siteId;
    
    @JsonProperty("systemIpAddress")
    private String systemIpAddress;
    
    @JsonProperty("licenseKey")
    private String licenseKey;
    
    @JsonProperty("licenseSecret")
    private String licenseSecret;

    @JsonProperty("managementType")
    private VendorManagementType managementType;

    @JsonProperty("localId")
    private String localId;
    
    @JsonProperty("remoteId")
    private String remoteId;

    @JsonProperty("controller1")
    private String controller1;
    
    @JsonProperty("controller2")
    private String controller2;
    
    @JsonProperty("serialNumber")
    private String serialNumber;
    
    @JsonProperty("adminPassword")
    private String adminPassword;
    
    @JsonProperty("activationKey")
    private String activationKey;
    
    @JsonProperty("controllerFqdn")
    private String controllerFqdn;
    
    @JsonProperty("rootPassword")
    private String rootPassword;
    
    @JsonProperty("accountName")
    private String accountName;
    
    @JsonProperty("hostname")
    private String hostname;
    
    @JsonProperty("accountKey")
    private String accountKey;
    
    @JsonProperty("applianceTag")
    private String applianceTag;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("connectToCloudVision")
    private Boolean connectToCloudVision;

    @JsonProperty("cvpType")
    private CvpType cvpType;

    @JsonProperty("cvpFqdn")
    private String cvpFqdn;

    @JsonProperty("cvpIpAddress")
    private String cvpIpAddress;

    @JsonProperty("cvaasPort")
    private String cvaasPort;

    @JsonProperty("cvpPort")
    private String cvpPort;

    @JsonProperty("cvpToken")
    private String cvpToken;

    @JsonProperty("provisioningKey")
    private String provisioningKey;

    @JsonProperty("privateAddress")
    private String privateAddress;

    @JsonProperty("privateCidrMask")
    private String privateCidrMask;

    @JsonProperty("privateGateway")
    private String privateGateway;

    @JsonProperty("licenseId")
    private String licenseId;

    @JsonProperty("panoramaIpAddress")
    private String panoramaIpAddress;

    @JsonProperty("panoramaAuthKey")
    private String panoramaAuthKey;
}
