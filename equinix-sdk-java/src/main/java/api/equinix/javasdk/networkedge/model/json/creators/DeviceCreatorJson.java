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

package api.equinix.javasdk.networkedge.model.json.creators;

import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.IPAssignment;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>DeviceCreatorJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class DeviceCreatorJson {

    @Setter(AccessLevel.PACKAGE)
    @JsonProperty("secondary")
    private DeviceCreatorJson secondary;

    @JsonProperty("deviceTypeCode")
    private String deviceTypeCode;

    @JsonProperty("metroCode")
    private MetroCode metroCode;

    @JsonProperty("throughput")
    private Integer throughput;

    @JsonProperty("throughputUnit")
    private BandwidthUnit throughputUnit;

    @JsonProperty("packageCode")
    private String packageCode;

    @JsonProperty("version")
    private String version;

    @JsonProperty("licenseType")
    private LicenseType licenseType;

    @JsonProperty("accountNumber")
    private Integer accountNumber;

    @JsonProperty("notifications")
    private ArrayList<String> notifications;

    @JsonProperty("accountReferenceId")
    private String accountReferenceId;

    @JsonProperty("userPublicKey")
    private DeviceOperator.PublicKeyJson userPublicKey;

    @JsonProperty("sshUsers")
    private List<DeviceOperator.SSHUserJson> sshUsers;

    @JsonProperty("additionalBandwidth")
    private Integer additionalBandwidth;

    @JsonProperty("interfaceCount")
    private Integer interfaceCount;

    @JsonProperty("systemIpAddress")
    private String systemIpAddress;

    @JsonProperty("ipType")
    private IPAssignment ipType;

    @JsonProperty("siteId")
    private String siteId;

    @JsonProperty("aclTemplateUuid")
    private String aclTemplateUuid;

    @JsonProperty("core")
    private Integer core;

    @JsonProperty("deviceManagementType")
    private DeviceManagementType deviceManagementType;

    @JsonProperty("virtualDeviceName")
    private String virtualDeviceName;

    @JsonProperty("hostNamePrefix")
    private String hostNamePrefix;

    @JsonProperty("licenseMode")
    private LicenseType licenseMode;

    @JsonProperty("licenseFileId")
    private String licenseFileId;

    @JsonProperty("diverseFromDeviceUuid")
    private String diverseFromDeviceUuid;

    @JsonProperty("diverseFromDeviceName")
    private String diverseFromDeviceName;

    @JsonProperty("primaryDeviceUuid")
    private String primaryDeviceUuid;

    @JsonProperty("licenseToken")
    private String licenseToken;

    @JsonProperty("smartLicenseUrl")
    private String smartLicenseUrl;

    @JsonProperty("orderingContact")
    public String orderingContact;

    @JsonProperty("sshInterfaceId")
    private Integer sshInterfaceId;
    
    /**
     * <p>Constructor for DeviceCreatorJson.</p>
     *
     * @param deviceBuilderSecondary a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceBuilderSecondary} object.
     */
    public DeviceCreatorJson(DeviceOperator.DeviceBuilderSecondary deviceBuilderSecondary) {
        
        this.virtualDeviceName = deviceBuilderSecondary.getDeviceName();
        this.accountNumber = deviceBuilderSecondary.getAccountNumber();
        this.accountReferenceId = deviceBuilderSecondary.getAccountReferenceId();
        this.sshUsers = deviceBuilderSecondary.getSshUsers();
        this.metroCode = deviceBuilderSecondary.getMetroCode();
        this.hostNamePrefix = deviceBuilderSecondary.getHostNamePrefix();
        this.notifications = deviceBuilderSecondary.getNotifications();

        this.primaryDeviceUuid = deviceBuilderSecondary.getPrimaryDeviceUuid();
        this.licenseFileId = deviceBuilderSecondary.getLicenseFileId();
        this.licenseToken = deviceBuilderSecondary.getLicenseToken();
        this.smartLicenseUrl = deviceBuilderSecondary.getSmartLicenseUrl();
        this.aclTemplateUuid = deviceBuilderSecondary.getAclTemplateUuid();
        this.siteId = deviceBuilderSecondary.getSiteId();
        this.systemIpAddress = deviceBuilderSecondary.getSystemIpAddress();
        this.additionalBandwidth = deviceBuilderSecondary.getAdditionalBandwidth();
    }

    /**
     * <p>Constructor for DeviceCreatorJson.</p>
     *
     * @param deviceBuilder a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceBuilder} object.
     */
    public DeviceCreatorJson(DeviceOperator.DeviceBuilder deviceBuilder) {
        this.virtualDeviceName = deviceBuilder.getDeviceName();
        this.accountNumber = deviceBuilder.getAccountNumber();
        this.accountReferenceId = deviceBuilder.getAccountReferenceId();
        this.userPublicKey = deviceBuilder.getUserPublicKey();
        this.deviceTypeCode = deviceBuilder.getDeviceTypeCode();
        this.sshUsers = deviceBuilder.getSshUsers();
        this.metroCode = deviceBuilder.getMetroCode();
        this.deviceManagementType = deviceBuilder.getDeviceManagementType();
        this.licenseMode = deviceBuilder.getLicenseMode();
        this.hostNamePrefix = deviceBuilder.getHostNamePrefix();
        this.packageCode = deviceBuilder.getPackageCode();
        this.version = deviceBuilder.getVersion();
        this.core = deviceBuilder.getCore();
        this.throughput = deviceBuilder.getThroughput();
        this.throughputUnit = deviceBuilder.getThroughputUnit();
        this.notifications = deviceBuilder.getNotifications();

        this.primaryDeviceUuid = deviceBuilder.getPrimaryDeviceUuid();
        this.diverseFromDeviceUuid = deviceBuilder.getDiverseFromDeviceUuid();
        this.licenseFileId = deviceBuilder.getLicenseFileId();
        this.licenseToken = deviceBuilder.getLicenseToken();
        this.smartLicenseUrl = deviceBuilder.getSmartLicenseUrl();
        this.orderingContact = deviceBuilder.getOrderingContact();
        this.aclTemplateUuid = deviceBuilder.getAclTemplateUuid();
        this.siteId = deviceBuilder.getSiteId();
        this.ipType = deviceBuilder.getIpType();
        this.systemIpAddress = deviceBuilder.getSystemIpAddress();
        this.sshInterfaceId = deviceBuilder.getSshInterfaceId();
        this.interfaceCount = deviceBuilder.getInterfaceCount();
        this.additionalBandwidth = deviceBuilder.getAdditionalBandwidth();
    }
}
