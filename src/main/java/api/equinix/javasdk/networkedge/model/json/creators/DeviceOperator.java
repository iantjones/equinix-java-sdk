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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.client.internal.implementation.DeviceClientImpl;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.IPAssignment;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.enums.SSHUserAction;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.DeviceVendorConfig;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>DeviceOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceOperator extends ResourceImpl<Device> {

    @Getter
    private final Pageable<Device> serviceClient;

    /**
     * <p>Constructor for DeviceOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public DeviceOperator(Pageable<Device> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceBuilder} object.
     * @param deviceName a {@link java.lang.String} object.
     */
    public DeviceBuilder create(String deviceName) {
        return new DeviceBuilder(deviceName);
    }

    /**
     * <p>createRedundantDevice.</p>
     *
     * @param secondaryDeviceName a {@link java.lang.String} object.
     * @param primaryDeviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceBuilderSecondary} object.
     */
    public DeviceBuilderSecondary createRedundantDevice(String secondaryDeviceName, String primaryDeviceUuid) {
        return new DeviceBuilderSecondary(secondaryDeviceName, primaryDeviceUuid);
    }

    public DeviceBuilderSecondary createRedundantDevice(String secondaryDeviceName, Device primaryDevice) {
        return createRedundantDevice(secondaryDeviceName, primaryDevice.getUuid());
    }

    /**
     * <p>update.</p>
     *
     * @param json a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceUpdater} object.
     */
    public DeviceOperator.DeviceUpdater update(DeviceJson json) {
        return new DeviceOperator.DeviceUpdater(json);
    }

    @Getter
    public class DeviceBuilder {

        private String deviceName;
        private Integer accountNumber;
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
        private String aclTemplateUuid;
        private String siteId;
        private IPAssignment ipType;
        private String systemIpAddress;
        private Integer sshInterfaceId;
        private Integer interfaceCount;
        private Integer additionalBandwidth;

        private DeviceVendorConfig vendorConfig;
        private List<SSHUserJson> sshUsers;
        private PublicKeyJson userPublicKey;

        protected DeviceBuilder(String deviceName) {
            this.deviceName = deviceName;
        }

        public DeviceBuilder withAccountNumber(Integer accountNumber) {
            this.accountNumber = accountNumber;
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
            this.aclTemplateUuid = aclTemplateUuid;
            return this;
        }

        public DeviceBuilder withAclTemplate(ACLTemplate aclTemplate) {
            return withAclTemplateUuid(aclTemplate.getUuid());
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
        private Integer accountNumber;
        private String accountReferenceId;
        private Integer additionalBandwidth;
        private String licenseFileId;
        private String licenseToken;
        private MetroCode metroCode;
        private ArrayList<String> notifications;
        private String aclTemplateUuid;
        private List<SSHUserJson> sshUsers;
        private String hostNamePrefix;
        private String siteId;
        private String systemIpAddress;
        private DeviceVendorConfig vendorConfig;
        private Integer sshInterfaceId;
        private String smartLicenseUrl;

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

        public DeviceBuilderSecondary withAccountNumber(Integer accountNumber) {
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

        public DeviceBuilderSecondary withAclTemplateUuid(String aclTemplateUuid) {
            this.aclTemplateUuid = aclTemplateUuid;
            return this;
        }

        public DeviceBuilderSecondary withAclTemplate(ACLTemplate aclTemplate) {
            return withAclTemplateUuid(aclTemplate.getUuid());
        }

        public DeviceBuilderSecondary withSiteId(String siteId) {
            this.siteId = siteId;
            return this;
        }

        public DeviceBuilderSecondary withSystemIpAddress(String systemIpAddress) {
            this.systemIpAddress = systemIpAddress;
            return this;
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
            this.updaterJson = Constants.JSON_CONVERTOR.convertValue(this.json, DeviceUpdaterJson.class);
        }

        public DeviceOperator.DeviceUpdater withDeviceName(String deviceName) {
            this.updaterJson.setVirtualDeviceName(deviceName);
            return this;
        }

        public DeviceOperator.DeviceUpdater withClusterName(String clusterName) {
            this.updaterJson.setClusterName(clusterName);
            return this;
        }

        public DeviceOperator.DeviceUpdater increaseTermLength(Integer termLength) {
            this.updaterJson.setTermLength(termLength);
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
