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

package com.eqixiac.equinix.networkedge.model.json;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.Lifecycle;
import com.eqixiac.equinix.core.model.deserializers.LocalDateTimeDeserializer;
import com.eqixiac.equinix.core.enums.BandwidthUnit;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.networkedge.enums.*;
import com.eqixiac.equinix.networkedge.model.Device;
import com.eqixiac.equinix.networkedge.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Virtual device details as returned by the Network Edge API
 * ({@code VirtualDeviceDetailsResponse} in the network-edge v1 catalog spec). Field set matches
 * the spec schema exactly; response-side fields not declared there were removed in 2.0.
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DeviceJson extends Lifecycle {

    @Getter static TypeReference<List<DeviceJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<UUIDResult> createTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("deviceTypeCode")
    private String deviceTypeCode;

    @JsonProperty("deviceTypeName")
    private String deviceTypeName;

    @JsonProperty("deviceTypeVendor")
    private Vendor deviceTypeVendor;

    @JsonProperty("deviceTypeCategory")
    private DeviceCategory deviceTypeCategory;

    @JsonProperty("status")
    private DeviceStatus status;

    @JsonProperty("licenseStatus")
    private LicenseStatus licenseStatus;

    @JsonProperty("metroCode")
    private MetroCode metroCode;

    @JsonProperty("metroName")
    private String metroName;

    @JsonProperty("licenseFileId")
    private String licenseFileId;

    @JsonProperty("region")
    private Region region;

    @JsonProperty("throughput")
    private Double throughput;

    @JsonProperty("throughputUnit")
    private BandwidthUnit throughputUnit;

    @JsonProperty("hostName")
    private String hostName;

    @JsonProperty("packageCode")
    private String packageCode;

    @JsonProperty("packageName")
    private String packageName;

    @JsonProperty("version")
    private String version;

    @JsonProperty("licenseType")
    private LicenseType licenseType;

    @JsonProperty("licenseName")
    private String licenseName;

    @JsonProperty("sshIpAddress")
    private String sshIpAddress;

    @JsonProperty("systemIpAddress")
    private String systemIpAddress;

    @JsonProperty("publicIp")
    private String publicIp;

    @JsonProperty("publicGatewayIp")
    private String publicGatewayIp;

    @JsonProperty("managementIp")
    private String managementIp;

    @JsonProperty("managementGatewayIp")
    private String managementGatewayIp;

    @JsonProperty("deviceSerialNo")
    private String deviceSerialNo;

    @JsonProperty("sshIpFqdn")
    private String sshIpFqdn;

    @JsonProperty("primaryDnsName")
    private String primaryDnsName;

    @JsonProperty("secondaryDnsName")
    private String secondaryDnsName;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("siteId")
    private String siteId;

    @JsonProperty("userPublicKey")
    private UserPublicKey userPublicKey;

    @JsonProperty("notifications")
    private ArrayList<String> notifications;

    @JsonProperty("vendorConfig")
    private DeviceVendorConfig vendorConfig;

    @JsonProperty("redundancyType")
    private RedundancyType redundancyType;

    @JsonProperty("redundantUuid")
    private String redundantUuid;

    @JsonProperty("purchaseOrderNumber")
    private String purchaseOrderNumber;

    @JsonProperty("termLength")
    private Integer termLength;

    @JsonProperty("additionalBandwidth")
    private Integer additionalBandwidth;

    @JsonProperty("interfaceCount")
    private Integer interfaceCount;

    @JsonProperty("core")
    private DeviceCore core;

    @JsonProperty("deviceManagementType")
    private DeviceManagementType deviceManagementType;

    @JsonProperty("interfaces")
    private ArrayList<NetworkInterface> interfaces;

    // Spec type is number; 4-byte ASNs exceed Integer.MAX_VALUE, so Long.
    @JsonProperty("asn")
    private Long asn;

    @JsonProperty("diverseFromDeviceUuid")
    private String diverseFromDeviceUuid;

    @JsonProperty("diverseFromDeviceName")
    private String diverseFromDeviceName;

    @JsonProperty("expiry")
    private String expiry;

    @JsonProperty("sshIpFqdnStatus")
    private SshIpFqdnStatus sshIpFqdnStatus;

    @JsonProperty("connectivity")
    private Connectivity connectivity;

    @JsonProperty("pricingDetails")
    private DevicePricingDetail pricingDetails;

    @JsonProperty("plane")
    private DevicePlane plane;

    @JsonProperty("newTermLength")
    private String newTermLength;

    @JsonProperty("channelPartner")
    private String channelPartner;

    // Network Edge responses use *DateTime audit fields rather than the shared Lifecycle *Date names.
    @JsonProperty("createdDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @JsonProperty("lastUpdatedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime lastUpdatedDateTime;

    @JsonProperty("deletedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime deletedDateTime;

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDateTime != null ? createdDateTime : super.getCreatedDate();
    }

    @Override
    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDateTime != null ? lastUpdatedDateTime : super.getLastUpdatedDate();
    }

    @Override
    public LocalDateTime getDeletedDate() {
        return deletedDateTime != null ? deletedDateTime : super.getDeletedDate();
    }
}