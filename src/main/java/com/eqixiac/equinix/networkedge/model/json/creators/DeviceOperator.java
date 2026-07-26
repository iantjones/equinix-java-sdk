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

package com.eqixiac.equinix.networkedge.model.json.creators;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.IPAddress;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.core.enums.BandwidthUnit;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.networkedge.client.internal.implementation.DeviceClientImpl;
import com.eqixiac.equinix.networkedge.enums.ACLInterfaceType;
import com.eqixiac.equinix.networkedge.enums.Connectivity;
import com.eqixiac.equinix.networkedge.enums.DeviceManagementType;
import com.eqixiac.equinix.networkedge.enums.DeviceStatus;
import com.eqixiac.equinix.networkedge.enums.IPAssignment;
import com.eqixiac.equinix.networkedge.enums.LicenseType;
import com.eqixiac.equinix.networkedge.enums.SSHUserAction;
import com.eqixiac.equinix.networkedge.model.ACLTemplate;
import com.eqixiac.equinix.networkedge.model.Device;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceVendorConfig;
import com.eqixiac.equinix.networkedge.model.json.DeviceJson;
import com.eqixiac.equinix.networkedge.model.wrappers.DeviceWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class DeviceOperator extends ResourceImpl<Device> {

    @Getter
    private final Pageable<Device> serviceClient;

    public DeviceOperator(Pageable<Device> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public DeviceBuilder create(String deviceName) {
        return new DeviceBuilder(deviceName);
    }

    public DeviceBuilderSecondary createRedundantDevice(String secondaryDeviceName, String primaryDeviceUuid) {
        return new DeviceBuilderSecondary(secondaryDeviceName, primaryDeviceUuid);
    }

    public DeviceBuilderSecondary createRedundantDevice(String secondaryDeviceName, Device primaryDevice) {
        return createRedundantDevice(secondaryDeviceName, primaryDevice.getUuid());
    }

    public DeviceOperator.DeviceUpdater update(DeviceJson json) {
        return new DeviceOperator.DeviceUpdater(json);
    }

    @Getter
    public class DeviceBuilder {

        private String deviceName;
        private String accountNumber;
        private String accountReferenceId;
        private String deviceTypeCode;
        private MetroCode metroCode;
        private DeviceManagementType deviceManagementType;
        private LicenseType licenseMode;
        private String hostNamePrefix;
        private String packageCode;
        private String version;
        private Integer core;
        private Integer throughput;
        private BandwidthUnit throughputUnit;
        private ArrayList<String> notifications;

        private String primaryDeviceUuid;
        private String diverseFromDeviceUuid;
        private String licenseFileId;
        private String licenseToken;
        private String smartLicenseUrl;
        private String orderingContact;
        private List<ACLDetail> aclDetails;
        private String siteId;
        private IPAssignment ipType;
        private String systemIpAddress;
        private Integer sshInterfaceId;
        private Integer interfaceCount;
        private Integer additionalBandwidth;

        private Boolean agreeOrderTerms;
        private String projectId;
        private Integer tier;
        private String termlength;
        private Connectivity connectivity;
        private ClusterConfig clusterDetails;
        private String day0TextFileId;
        private String cloudInitFileId;
        private String purchaseOrderNumber;
        private String orderReference;
        private String channelPartner;
        private String licenseCategory;

        private DeviceVendorConfig vendorConfig;
        private List<SSHUserJson> sshUsers;
        private PublicKeyJson userPublicKey;

        protected DeviceBuilder(String deviceName) {
            this.deviceName = deviceName;
        }

        public DeviceBuilder withAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public DeviceBuilder agreeToOrderTerms() {
            this.agreeOrderTerms = true;
            return this;
        }

        public DeviceBuilder withAgreeOrderTerms(Boolean agreeOrderTerms) {
            this.agreeOrderTerms = agreeOrderTerms;
            return this;
        }

        public DeviceBuilder withProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public DeviceBuilder withTier(Integer tier) {
            this.tier = tier;
            return this;
        }

        public DeviceBuilder withTermLength(String termlength) {
            this.termlength = termlength;
            return this;
        }

        public DeviceBuilder withConnectivity(Connectivity connectivity) {
            this.connectivity = connectivity;
            return this;
        }

        public DeviceBuilder withClusterDetails(ClusterConfig clusterDetails) {
            this.clusterDetails = clusterDetails;
            return this;
        }

        public DeviceBuilder withDay0TextFileId(String day0TextFileId) {
            this.day0TextFileId = day0TextFileId;
            return this;
        }

        public DeviceBuilder withCloudInitFileId(String cloudInitFileId) {
            this.cloudInitFileId = cloudInitFileId;
            return this;
        }

        public DeviceBuilder withPurchaseOrderNumber(String purchaseOrderNumber) {
            this.purchaseOrderNumber = purchaseOrderNumber;
            return this;
        }

        public DeviceBuilder withOrderReference(String orderReference) {
            this.orderReference = orderReference;
            return this;
        }

        public DeviceBuilder withChannelPartner(String channelPartner) {
            this.channelPartner = channelPartner;
            return this;
        }

        public DeviceBuilder withLicenseCategory(String licenseCategory) {
            this.licenseCategory = licenseCategory;
            return this;
        }

        public DeviceBuilder withAccountReferenceId(String accountReferenceId) {
            this.accountReferenceId = accountReferenceId;
            return this;
        }

        public DeviceBuilder withDeviceTypeCode(String deviceTypeCode) {
            this.deviceTypeCode = deviceTypeCode;
            return this;
        }

        public DeviceBuilder withMetroCode(MetroCode metroCode) {
            this.metroCode = metroCode;
            return this;
        }

        public DeviceBuilder withDeviceManagementType(DeviceManagementType deviceManagementType) {
            this.deviceManagementType = deviceManagementType;
            return this;
        }

        public DeviceBuilder withLicenseMode(LicenseType licenseMode) {
            this.licenseMode = licenseMode;
            return this;
        }

        public DeviceBuilder withHostNamePrefix(String hostNamePrefix) {
            this.hostNamePrefix = hostNamePrefix;
            return this;
        }

        public DeviceBuilder withPackageCode(String packageCode) {
            this.packageCode = packageCode;
            return this;
        }

        public DeviceBuilder withVersion(String version) {
            this.version = version;
            return this;
        }

        public DeviceBuilder withCore(Integer core) {
            this.core = core;
            return this;
        }

        public DeviceBuilder withThroughput(Integer throughput) {
            this.throughput = throughput;
            return this;
        }

        public DeviceBuilder withThroughputUnit(BandwidthUnit throughputUnit) {
            this.throughputUnit = throughputUnit;
            return this;
        }

        public DeviceBuilder withNotification(String emailAddress) {
            if (this.notifications == null) {
                this.notifications = new ArrayList<>();
            }
            this.notifications.add(emailAddress);
            return this;
        }

        public DeviceBuilder withPrimaryDeviceUuid(String primaryDeviceUuid) {
            this.primaryDeviceUuid = primaryDeviceUuid;
            return this;
        }

        public DeviceBuilder withDiverseFromDeviceUuid(String diverseFromDeviceUuid) {
            this.diverseFromDeviceUuid = diverseFromDeviceUuid;
            return this;
        }

        public DeviceBuilder withPrimaryDevice(Device primaryDevice) {
            return withPrimaryDeviceUuid(primaryDevice.getUuid());
        }

        public DeviceBuilder withDiverseFromDevice(Device diverseFromDevice) {
            return withDiverseFromDeviceUuid(diverseFromDevice.getUuid());
        }

        public DeviceBuilder withLicenseFileId(String licenseFileId) {
            this.licenseFileId = licenseFileId;
            return this;
        }

        public DeviceBuilder withLicenseToken(String licenseToken) {
            this.licenseToken = licenseToken;
            return this;
        }

        public DeviceBuilder withSmartLicenseUrl(String smartLicenseUrl) {
            this.smartLicenseUrl = smartLicenseUrl;
            return this;
        }

        public DeviceBuilder withOrderingContact(String orderingContact) {
            this.orderingContact = orderingContact;
            return this;
        }

        public DeviceBuilder withAclTemplateUuid(String aclTemplateUuid) {
            return withAclDetail(ACLInterfaceType.WAN, aclTemplateUuid);
        }

        public DeviceBuilder withAclTemplate(ACLTemplate aclTemplate) {
            return withAclTemplateUuid(aclTemplate.getUuid());
        }

        public DeviceBuilder withAclDetail(ACLInterfaceType interfaceType, String aclTemplateUuid) {
            if (this.aclDetails == null) {
                this.aclDetails = new ArrayList<>();
            }
            this.aclDetails.add(new ACLDetail(interfaceType, aclTemplateUuid));
            return this;
        }

        public DeviceBuilder withSiteId(String siteId) {
            this.siteId = siteId;
            return this;
        }

        public DeviceBuilder withIpType(IPAssignment ipType) {
            this.ipType = ipType;
            return this;
        }

        public DeviceBuilder withSystemIpAddress(String systemIpAddress) {
            this.systemIpAddress = systemIpAddress;
            return this;
        }

        /**
         * Typed variant of {@code withSystemIpAddress(String)}. Serializes the address via
         * {@link IPAddress#toCidr()}, producing the identical wire value to the String setter.
         */
        public DeviceBuilder withSystemIpAddress(IPAddress systemIpAddress) {
            return withSystemIpAddress(systemIpAddress == null ? null : systemIpAddress.toCidr());
        }

        public DeviceBuilder withSshInterfaceId(Integer sshInterfaceId) {
            this.sshInterfaceId = sshInterfaceId;
            return this;
        }

        public DeviceBuilder withInterfaceCount(Integer interfaceCount) {
            this.interfaceCount = interfaceCount;
            return this;
        }

        public DeviceBuilder withAdditionalBandwidth(Integer additionalBandwidth) {
            this.additionalBandwidth = additionalBandwidth;
            return this;
        }

        public DeviceBuilder withPublicKey(String keyName, String keyValue, String username) {
            this.userPublicKey = new PublicKeyJson(keyName, keyValue, username);
            return this;
        }

        public DeviceBuilder withVendorConfig(DeviceVendorConfig vendorConfig) {
            this.vendorConfig = vendorConfig;
            return this;
        }

        public DeviceBuilder withNewSSHUser(String username, String password) {
            if (this.sshUsers == null) {
                this.sshUsers = new ArrayList<>();
            }
            this.sshUsers.add(new SSHUserJson(username, password));
            return this;
        }

        public DeviceBuilder withExistingSSHUser(String sshUserUuid) {
            if (this.sshUsers == null) {
                this.sshUsers = new ArrayList<>();
            }
            this.sshUsers.add(new SSHUserJson(sshUserUuid));
            return this;
        }

        public DeviceBuilderSecondary withSecondary(String secondaryDeviceName) {
            DeviceCreatorJson deviceCreatorJson = new DeviceCreatorJson(this);
            return new DeviceBuilderSecondary(secondaryDeviceName, deviceCreatorJson);
        }

        public Device create() {
            DeviceCreatorJson deviceCreatorJson = new DeviceCreatorJson(this);
            DeviceJson deviceJson = ((DeviceClientImpl) DeviceOperator.this.getServiceClient()).create(deviceCreatorJson, false);
            return new DeviceWrapper(deviceJson, DeviceOperator.this.getServiceClient());
        }

        public Device saveAsDraft() {
            DeviceCreatorJson deviceCreatorJson = new DeviceCreatorJson(this);
            DeviceJson deviceJson = ((DeviceClientImpl) DeviceOperator.this.getServiceClient()).create(deviceCreatorJson, true);
            return new DeviceWrapper(deviceJson, DeviceOperator.this.getServiceClient());
        }
    }

    @Getter
    public class DeviceBuilderSecondary {

        private DeviceCreatorJson deviceCreatorJson;
        private String deviceName;
        private String primaryDeviceUuid;
        private String accountNumber;
        private String accountReferenceId;
        private Integer additionalBandwidth;
        private String licenseFileId;
        private String licenseToken;
        private MetroCode metroCode;
        private ArrayList<String> notifications;
        private List<ACLDetail> aclDetails;
        private List<SSHUserJson> sshUsers;
        private String hostNamePrefix;
        private String siteId;
        private String systemIpAddress;
        private DeviceVendorConfig vendorConfig;
        private Integer sshInterfaceId;
        private String smartLicenseUrl;
        private String day0TextFileId;
        private String cloudInitFileId;
        private String version;

        protected DeviceBuilderSecondary(String secondaryDeviceName, DeviceCreatorJson deviceCreatorJson) {
            this.deviceName = secondaryDeviceName;
            this.deviceCreatorJson = deviceCreatorJson;
        }

        protected DeviceBuilderSecondary(String deviceName, String primaryDeviceUuid) {
            this.deviceName = deviceName;
            this.primaryDeviceUuid = primaryDeviceUuid;
        }

        public DeviceBuilderSecondary withDeviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public DeviceBuilderSecondary withAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public DeviceBuilderSecondary withAccountReferenceId(String accountReferenceId) {
            this.accountReferenceId = accountReferenceId;
            return this;
        }

        public DeviceBuilderSecondary withMetroCode(MetroCode metroCode) {
            this.metroCode = metroCode;
            return this;
        }

        public DeviceBuilderSecondary withHostNamePrefix(String hostNamePrefix) {
            this.hostNamePrefix = hostNamePrefix;
            return this;
        }

        public DeviceBuilderSecondary withNotification(String emailAddress) {
            if (this.notifications == null) {
                this.notifications = new ArrayList<>();
            }
            this.notifications.add(emailAddress);
            return this;
        }

        public DeviceBuilderSecondary withLicenseFileId(String licenseFileId) {
            this.licenseFileId = licenseFileId;
            return this;
        }

        public DeviceBuilderSecondary withLicenseToken(String licenseToken) {
            this.licenseToken = licenseToken;
            return this;
        }

        public DeviceBuilderSecondary withSshInterfaceId(Integer sshInterfaceId) {
            this.sshInterfaceId = sshInterfaceId;
            return this;
        }

        public DeviceBuilderSecondary withSmartLicenseUrl(String smartLicenseUrl) {
            this.smartLicenseUrl = smartLicenseUrl;
            return this;
        }

        /**
         * <p>The software version of the secondary device. Only applicable when adding a secondary
         * device to an existing device ({@code VirtualDevicHARequest.version}).</p>
         */
        public DeviceBuilderSecondary withVersion(String version) {
            this.version = version;
            return this;
        }

        public DeviceBuilderSecondary withAclTemplateUuid(String aclTemplateUuid) {
            return withAclDetail(ACLInterfaceType.WAN, aclTemplateUuid);
        }

        public DeviceBuilderSecondary withAclTemplate(ACLTemplate aclTemplate) {
            return withAclTemplateUuid(aclTemplate.getUuid());
        }

        public DeviceBuilderSecondary withAclDetail(ACLInterfaceType interfaceType, String aclTemplateUuid) {
            if (this.aclDetails == null) {
                this.aclDetails = new ArrayList<>();
            }
            this.aclDetails.add(new ACLDetail(interfaceType, aclTemplateUuid));
            return this;
        }

        public DeviceBuilderSecondary withDay0TextFileId(String day0TextFileId) {
            this.day0TextFileId = day0TextFileId;
            return this;
        }

        public DeviceBuilderSecondary withCloudInitFileId(String cloudInitFileId) {
            this.cloudInitFileId = cloudInitFileId;
            return this;
        }

        public DeviceBuilderSecondary withSiteId(String siteId) {
            this.siteId = siteId;
            return this;
        }

        public DeviceBuilderSecondary withSystemIpAddress(String systemIpAddress) {
            this.systemIpAddress = systemIpAddress;
            return this;
        }

        /**
         * Typed variant of {@code withSystemIpAddress(String)}. Serializes the address via
         * {@link IPAddress#toCidr()}, producing the identical wire value to the String setter.
         */
        public DeviceBuilderSecondary withSystemIpAddress(IPAddress systemIpAddress) {
            return withSystemIpAddress(systemIpAddress == null ? null : systemIpAddress.toCidr());
        }

        public DeviceBuilderSecondary withAdditionalBandwidth(Integer additionalBandwidth) {
            this.additionalBandwidth = additionalBandwidth;
            return this;
        }

        public DeviceBuilderSecondary withNewSSHUser(String username, String password) {
            if (this.sshUsers == null) {
                this.sshUsers = new ArrayList<>();
            }
            this.sshUsers.add(new SSHUserJson(username, password));
            return this;
        }

        public DeviceBuilderSecondary withExistingSSHUser(String sshUserUuid) {
            if (this.sshUsers == null) {
                this.sshUsers = new ArrayList<>();
            }
            this.sshUsers.add(new SSHUserJson(sshUserUuid));
            return this;
        }

        public Device create() {
            //TODO This is messy.
            if(deviceCreatorJson != null) {
                DeviceCreatorJson secondary = new DeviceCreatorJson(this);
                deviceCreatorJson.setSecondary(secondary);
            }
            else {
                deviceCreatorJson = new DeviceCreatorJson(this);
            }

            DeviceJson deviceJson = ((DeviceClientImpl) DeviceOperator.this.getServiceClient()).create(deviceCreatorJson, false);
            return new DeviceWrapper(deviceJson, DeviceOperator.this.getServiceClient());
        }

        public Device saveAsDraft() {
            //TODO This is messy.
            if(deviceCreatorJson != null) {
                DeviceCreatorJson secondary = new DeviceCreatorJson(this);
                deviceCreatorJson.setSecondary(secondary);
            }
            else {
                deviceCreatorJson = new DeviceCreatorJson(this);
            }

            DeviceJson deviceJson = ((DeviceClientImpl) DeviceOperator.this.getServiceClient()).create(deviceCreatorJson, true);
            return new DeviceWrapper(deviceJson, DeviceOperator.this.getServiceClient());
        }
    }

    public class DeviceUpdater {

        private DeviceJson json;
        private DeviceUpdaterJson updaterJson;

        protected DeviceUpdater(DeviceJson json) {
            this.json = json;
            this.updaterJson = Constants.converter().convertValue(this.json, DeviceUpdaterJson.class);
        }

        public DeviceOperator.DeviceUpdater withDeviceName(String deviceName) {
            this.updaterJson.setVirtualDeviceName(deviceName);
            return this;
        }

        public DeviceOperator.DeviceUpdater withClusterName(String clusterName) {
            this.updaterJson.setClusterName(clusterName);
            return this;
        }

        public DeviceOperator.DeviceUpdater increaseTermLength(String termLength) {
            this.updaterJson.setTermLength(termLength);
            return this;
        }

        public DeviceOperator.DeviceUpdater increaseTermLength(Integer termLength) {
            return increaseTermLength(termLength != null ? termLength.toString() : null);
        }

        public DeviceOperator.DeviceUpdater withCore(Integer core) {
            this.updaterJson.setCore(core);
            return this;
        }

        public DeviceOperator.DeviceUpdater withTermLengthEffectiveImmediate(Boolean termLengthEffectiveImmediate) {
            this.updaterJson.setTermLengthEffectiveImmediate(termLengthEffectiveImmediate);
            return this;
        }

        public DeviceOperator.DeviceUpdater withAutoRenewalOptOut(Boolean autoRenewalOptOut) {
            this.updaterJson.setAutoRenewalOptOut(autoRenewalOptOut);
            return this;
        }

        /**
         * <p>Updates the license status of the device ({@code VirtualDeviceInternalPatchRequest.status}).
         * The API accepts {@code PROVISIONED}, {@code PROVISIONING}, {@code DEPROVISIONED},
         * {@code DEPROVISIONING} and {@code FAILED}.</p>
         */
        public DeviceOperator.DeviceUpdater withStatus(DeviceStatus status) {
            this.updaterJson.setStatus(status);
            return this;
        }

        public DeviceOperator.DeviceUpdater withDisablePassword(Boolean disablePassword) {
            this.updaterJson.setVendorConfig(new DeviceUpdaterJson.VendorConfigPatch(disablePassword));
            return this;
        }

        public DeviceOperator.DeviceUpdater addNotification(String emailAddress) {
            ArrayList<String> notifications = updaterJson.getNotifications();
            if(notifications == null) {
                notifications = new ArrayList<>();
            }

            notifications.add(emailAddress);
            updaterJson.setNotifications(notifications);
            return this;
        }

        public DeviceOperator.DeviceUpdater removeNotification(String emailAddress) {
            ArrayList<String> notifications = updaterJson.getNotifications();
            if(notifications == null) {
                return this;
            }

            notifications.remove(emailAddress);
            updaterJson.setNotifications(notifications);
            return this;
        }

        public Device save() {
            json = ((DeviceClientImpl) DeviceOperator.this.getServiceClient()).update(this.json.getUuid(), this.updaterJson);
            return new DeviceWrapper(json, DeviceOperator.this.getServiceClient());
        }
    }

    public static class PublicKeyJson {
        @JsonProperty("keyName")
        private final String keyName;

        @JsonProperty("keyValue")
        private final String keyValue;

        @JsonProperty("username")
        private final String username;

        public PublicKeyJson(String keyName, String keyValue, String username) {
            this.keyName = keyName;
            this.keyValue = keyValue;
            this.username = username;
        }
    }

    public static class SSHUserJson {
        @JsonProperty("sshUsername")
        private String sshUsername;

        @JsonProperty("sshPassword")
        private String sshPassword;

        @JsonProperty("sshUserUuid")
        private String sshUserUuid;

        @JsonProperty("action")
        private SSHUserAction action;

        public SSHUserJson(String sshUsername, String sshPassword) {
            this.sshUsername = sshUsername;
            this.sshPassword = sshPassword;
            this.action = SSHUserAction.CREATE;
        }

        public SSHUserJson(String sshUserUuid) {
            this.sshUserUuid = sshUserUuid;
            this.action = SSHUserAction.REUSE;
        }
    }
}
