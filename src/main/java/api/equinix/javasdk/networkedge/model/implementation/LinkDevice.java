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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.DeviceStatus;
import api.equinix.javasdk.networkedge.enums.NetworkScope;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>LinkDevice class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class LinkDevice {

    @JsonProperty("deviceUuid")
    private String deviceUuid;

    @JsonProperty("deviceName")
    private String deviceName;

    @JsonProperty("metroCode")
    private MetroCode metroCode;

    @JsonProperty("metroName")
    private String metroName;

    @JsonProperty("deviceTypeCode")
    private String deviceTypeCode;

    @JsonProperty("category")
    private DeviceCategory category;

    @JsonProperty("status")
    private DeviceStatus status;

    @JsonProperty("deviceManagementType")
    private DeviceManagementType deviceManagementType;

    @JsonProperty("networkScope")
    private NetworkScope networkScope;

    @JsonProperty("asn")
    private Integer asn;

    @JsonProperty("ipAssigned")
    private String ipAssigned;

    @JsonProperty("interfaceId")
    private Integer interfaceId;

    @JsonProperty("isDeviceAccessible")
    private Boolean isDeviceAccessible;

    @JsonProperty("zoneCode")
    private String zoneCode;

    @JsonProperty("zoneName")
    private String zoneName;
}
