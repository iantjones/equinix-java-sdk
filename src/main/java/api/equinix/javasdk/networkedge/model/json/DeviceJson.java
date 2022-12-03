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
import api.equinix.javasdk.core.model.Lifecycle;
import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.networkedge.enums.*;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>DeviceJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class DeviceJson extends Lifecycle {

    @Getter static TypeReference<Page<Device, DeviceJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<DeviceJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<DeviceJson> singleTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<UUIDResult> createTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("podName")
    private String podName;

    @JsonProperty("ipType")
    private IPAssignment ipType;

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

    @JsonProperty("clusterSupported")
    private Boolean clusterSupported;

    @JsonProperty("siblingCustOrgFlag")
    private Boolean siblingCustOrgFlag;

    @JsonProperty("isSubCustomerDevice")
    private Boolean isSubCustomerDevice;

    @JsonProperty("supportServicesEnabled")
    private Boolean supportServicesEnabled;

    @JsonProperty("supportServicesNotification")
    private List<String> supportServicesNotification;

    @JsonProperty("metroCode")
    private MetroCode metroCode;

    @JsonProperty("metroName")
    private String metroName;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("aclTemplateUuid")
    private String aclTemplateUuid;

    @JsonProperty("licenseFileId")
    private String licenseFileId;

    @JsonProperty("region")
    private Region region;

    @JsonProperty("clusterDetails")
    private ClusterDetail clusterDetails;

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

    @JsonProperty("deviceOrderNumber")
    private String deviceOrderNumber;

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
    private Integer accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("sdwanHostname")
    private String sdwanHostname;

    @JsonProperty("sdwanAccountName")
    private String sdwanAccountName;

    @JsonProperty("siteId")
    private String siteId;

    @JsonProperty("applianceTag")
    private String applianceTag;

    @JsonProperty("orderingContact")
    private Contact orderingContact;

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

    @JsonProperty("accountReferenceId")
    private String accountReferenceId;

    @JsonProperty("purchaseOrderNumber")
    private String purchaseOrderNumber;

    @JsonProperty("orderReference")
    private String orderReference;

    @JsonProperty("dealId")
    private String dealId;

    @JsonProperty("termLength")
    private Integer termLength;

    @JsonProperty("billingCommencementDate")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime billingCommencementDate;

    @JsonProperty("billingEnabled")
    private Boolean billingEnabled;

    @JsonProperty("additionalBandwidth")
    private Integer additionalBandwidth;

    @JsonProperty("interfaceCount")
    private Integer interfaceCount;

    @JsonProperty("core")
    private DeviceCore core;

    @JsonProperty("deviceManagementType")
    private DeviceManagementType deviceManagementType;

    @JsonProperty("networkScope")
    private NetworkScope networkScope;

    @JsonProperty("interfaces")
    private ArrayList<NetworkInterface> interfaces;

    @JsonProperty("asn")
    private Integer asn;

    @JsonProperty("supportDetails")
    private SupportDetail supportDetails;

    @JsonProperty("diverseFromDeviceUuid")
    private String diverseFromDeviceUuid;

    @JsonProperty("diverseFromDeviceName")
    private String diverseFromDeviceName;

    @JsonProperty("sshInterfaceId")
    private String sshInterfaceId;

    @JsonProperty("versionChangeStatus")
    private VersionChangeStatus versionChangeStatus;

    @JsonProperty("etf")
    private Etf etf;

    @JsonProperty("versionLastUpgradedBy")
    private String versionLastUpgradedBy;

    @JsonProperty("versionLastUpgradedDate")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime versionLastUpgradedDate;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdDate")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDate;

    @JsonProperty("lastUpdatedBy")
    private String lastUpdatedBy;

    @JsonProperty("lastUpdatedDate")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime lastUpdatedDate;

    @JsonProperty("deletedBy")
    private String deletedBy;

    @JsonProperty("deletedDate")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime deletedDate;
}