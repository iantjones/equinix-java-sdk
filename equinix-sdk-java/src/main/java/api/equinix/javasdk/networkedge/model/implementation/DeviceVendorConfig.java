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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>DeviceVendorConfig class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
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
    private String managementType;

    @JsonProperty("localId")
    private String localId;
    
    @JsonProperty("remoteId")
    private String remoteId;

    @JsonProperty("hostNamePrefix")
    private String hostNamePrefix;

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
}
