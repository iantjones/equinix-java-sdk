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

package api.equinix.javasdk.networkedge.client;

import api.equinix.javasdk.core.model.APIParam;
import api.equinix.javasdk.core.model.RequestBuilderBase;
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.util.ModelUtils;
import api.equinix.javasdk.networkedge.enums.*;
import api.equinix.javasdk.networkedge.model.implementation.SoftwarePackage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class RequestBuilder {

    public static RequestBuilder.Device device() {
        return new RequestBuilder.Device();
    }

    public static VPN vpn() {
        return new VPN();
    }
    
    public static Backup backup() {
        return new Backup();
    }

    public static OrderSummary orderSummary() {
        return new OrderSummary();
    }

    public static Pricing pricing() {
        return new Pricing();
    }

    public static BGP bgp() {
        return new BGP();
    }

    public static DeviceLink deviceLink() {
        return new DeviceLink();
    }

    /**
     *
     * @param deviceType the device type code (path parameter).
     * @param deviceManagementType the required device management type.
     */
    public static AllowedInterfaces allowedInterfaces(String deviceType, DeviceManagementType deviceManagementType) {
        return new AllowedInterfaces(deviceType, deviceManagementType);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AllowedInterfaces extends RequestBuilderBase<AllowedInterfaces> {

        private String deviceType;
        private DeviceManagementType deviceManagementType;
        private LicenseType mode;
        private Boolean cluster;
        private Boolean sdwan;
        private Connectivity connectivity;
        private Integer core;
        private Integer memory;
        private String unit;
        private String flavor;
        private String version;
        private String softwarePkg;

        protected AllowedInterfaces(String deviceType, DeviceManagementType deviceManagementType) {
            this.deviceType = deviceType;
            this.deviceManagementType = deviceManagementType;
        }

        protected AllowedInterfaces builder() {
            return new AllowedInterfaces();
        }

        public String getDeviceType() {
            return deviceType;
        }

        public AllowedInterfaces withMode(LicenseType mode) {
            this.mode = mode;
            return this;
        }

        public AllowedInterfaces withCluster(Boolean cluster) {
            this.cluster = cluster;
            return this;
        }

        public AllowedInterfaces withSdwan(Boolean sdwan) {
            this.sdwan = sdwan;
            return this;
        }

        public AllowedInterfaces withConnectivity(Connectivity connectivity) {
            this.connectivity = connectivity;
            return this;
        }

        public AllowedInterfaces withCore(Integer core) {
            this.core = core;
            return this;
        }

        public AllowedInterfaces withMemory(Integer memory) {
            this.memory = memory;
            return this;
        }

        public AllowedInterfaces withUnit(String unit) {
            this.unit = unit;
            return this;
        }

        public AllowedInterfaces withFlavor(String flavor) {
            this.flavor = flavor;
            return this;
        }

        public AllowedInterfaces withVersion(String version) {
            this.version = version;
            return this;
        }

        public AllowedInterfaces withSoftwarePkg(String softwarePkg) {
            this.softwarePkg = softwarePkg;
            return this;
        }

        public AllowedInterfaces build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("deviceManagementType", ModelUtils.singleValueList(this.deviceManagementType.getJsonValue()));
            this.queryParameters.put("mode", ModelUtils.singleValueList(this.mode != null ? this.mode.getQueryValue() : null));
            this.queryParameters.put("cluster", ModelUtils.singleValueList(this.cluster));
            this.queryParameters.put("sdwan", ModelUtils.singleValueList(this.sdwan));
            this.queryParameters.put("connectivity", ModelUtils.singleValueList(this.connectivity));
            this.queryParameters.put("core", ModelUtils.singleValueList(this.core));
            this.queryParameters.put("memory", ModelUtils.singleValueList(this.memory));
            this.queryParameters.put("unit", ModelUtils.singleValueList(this.unit));
            this.queryParameters.put("flavor", ModelUtils.singleValueList(this.flavor));
            this.queryParameters.put("version", ModelUtils.singleValueList(this.version));
            this.queryParameters.put("softwarePkg", ModelUtils.singleValueList(this.softwarePkg));

            this.wasBuilt = true;
            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Device extends RequestBuilderBase<Device> {

        private List<APIParam> metroCodes;
        private List<APIParam> deviceStatuses;
        private String accountUcmId;
        private Boolean showOnlySubCustomerDevices;

        protected Device builder() {
            return new Device();
        }

        public Device inMetro(MetroCode metroCode) {
            if(this.metroCodes == null) {
                this.metroCodes = new ArrayList<>();
            }
            this.metroCodes.add(metroCode);
            return this;
        }

        public Device havingStatus(DeviceStatus deviceStatus) {
            if(this.deviceStatuses == null) {
                this.deviceStatuses = new ArrayList<>();
            }
            this.deviceStatuses.add(deviceStatus);
            return this;
        }

        public Device forAccount(String accountUcmId) {
            this.accountUcmId = accountUcmId;
            return this;
        }

        public Device showOnlySubCustomerDevices(Boolean showOnlySubCustomerDevices) {
            this.showOnlySubCustomerDevices = showOnlySubCustomerDevices;
            return this;
        }

        public Device build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("metroCode", ModelUtils.stringListFromEnumList(this.metroCodes));
            this.queryParameters.put("status", ModelUtils.stringListFromEnumList(this.deviceStatuses));
            this.queryParameters.put("accountUcmId", ModelUtils.singleValueList(this.accountUcmId));
            this.queryParameters.put("showOnlySubCustomerDevices", ModelUtils.singleValueList(this.showOnlySubCustomerDevices));

            this.wasBuilt = true;
            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DeviceLink extends RequestBuilderBase<DeviceLink> {

        private List<APIParam> metroCodes;
        private String deviceUuid;
        private String accountUcmId;
        private String groupUuid;
        private String groupName;

        protected DeviceLink builder() {
            return new DeviceLink();
        }

        public DeviceLink inMetro(MetroCode metroCode) {
            if(this.metroCodes == null) {
                this.metroCodes = new ArrayList<>();
            }
            this.metroCodes.add(metroCode);
            return this;
        }

        public DeviceLink forDeviceUuid(String deviceUuid) {
            this.deviceUuid = deviceUuid;
            return this;
        }

        public DeviceLink forAccount(String accountUcmId) {
            this.accountUcmId = accountUcmId;
            return this;
        }

        public DeviceLink forGroupUuid(String groupUuid) {
            this.groupUuid = groupUuid;
            return this;
        }

        public DeviceLink forGroupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public DeviceLink build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("metro", ModelUtils.stringListFromEnumList(this.metroCodes));
            this.queryParameters.put("virtualDeviceUuid", ModelUtils.singleValueList(this.deviceUuid));
            this.queryParameters.put("accountUcmId", ModelUtils.singleValueList(this.accountUcmId));
            this.queryParameters.put("groupUuid", ModelUtils.singleValueList(this.groupUuid));
            this.queryParameters.put("groupName", ModelUtils.singleValueList(this.groupName));

            this.wasBuilt = true;
            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class VPN extends RequestBuilderBase<VPN> {

        private List<APIParam> statusList;
        private String virtualDeviceUuid;

        protected VPN builder() {
            return new VPN();
        }

        public VPN withStatus(VPNStatus vpnStatus) {
            if(this.statusList == null) {
                this.statusList = new ArrayList<>();
            }
            this.statusList.add(vpnStatus);
            return this;
        }

        public VPN forDeviceUuid(String deviceUuid) {
            this.virtualDeviceUuid = deviceUuid;
            return this;
        }

        public VPN build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("statusList", ModelUtils.stringListFromEnumList(this.statusList));
            this.queryParameters.put("virtualDeviceUuid", ModelUtils.singleValueList(this.virtualDeviceUuid));

            this.wasBuilt = true;
            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Backup extends RequestBuilderBase<Backup> {

        private List<APIParam> status;

        protected Backup builder() {
            return new Backup();
        }

        public Backup withStatus(BackupStatus status) {
            if(this.status == null) {
                this.status = new ArrayList<>();
            }
            this.status.add(status);
            return this;
        }

        public Backup build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("status", ModelUtils.stringListFromEnumList(this.status));

            this.wasBuilt = true;
            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BGP extends RequestBuilderBase<BGP> {

        private String virtualDeviceUuid;
        private String connectionUuid;
        private BGPStatus status;
        private String accountUcmId;

        protected BGP builder() {
            return new BGP();
        }

        public BGP forDevice(String deviceUuid) {
            this. virtualDeviceUuid = deviceUuid;
            return this;
        }

        public BGP forConnection(String connectionUuid) {
            this. connectionUuid = connectionUuid;
            return this;
        }

        public BGP havingStatus(BGPStatus status) {
            this. status = status;
            return this;
        }

        public BGP forAccount(String accountUcmId) {
            this. accountUcmId = accountUcmId;
            return this;
        }

        public BGP build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("virtualDeviceUuid", ModelUtils.singleValueList(this.virtualDeviceUuid));
            this.queryParameters.put("connectionUuid", ModelUtils.singleValueList(this.connectionUuid));
            this.queryParameters.put("status", ModelUtils.singleValueList(this.status));
            this.queryParameters.put("accountUcmId", ModelUtils.singleValueList(this.accountUcmId));

            this.wasBuilt = true;
            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Pricing extends RequestBuilderBase<Pricing> {

        private Integer accountNumber;
        private MetroCode metro;
        private String vendorPackage;
        private LicenseType licenseType;
        private SoftwarePackage softwarePackage;
        private Integer throughput;
        private BandwidthUnit throughputUnit;
        private Integer termLength;
        private Integer additionalBandwidth;
        private DeviceManagementType deviceManagementType;
        private Integer core;

        private Integer secondaryAccountNumber;
        private MetroCode secondaryMetro;
        private Integer secondaryAdditionalBandwidth;
        private String accountUcmId;

        protected Pricing builder() {
            return new Pricing();
        }

        public Pricing withAccountNumber(Integer accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Pricing withMetro(MetroCode metro) {
            this.metro = metro;
            return this;
        }

        public Pricing withVendorPackage(String vendorPackage) {
            this.vendorPackage = vendorPackage;
            return this;
        }

        public Pricing withLicenseType(LicenseType licenseType) {
            this.licenseType = licenseType;
            return this;
        }

        public Pricing withSoftwarePackage(SoftwarePackage softwarePackage) {
            this.softwarePackage = softwarePackage;
            return this;
        }

        public Pricing withThroughput(Integer throughput) {
            this.throughput = throughput;
            return this;
        }

        public Pricing withThroughputUnit(BandwidthUnit throughputUnit) {
            this.throughputUnit = throughputUnit;
            return this;
        }

        public Pricing withTermLength(Integer termLength) {
            this.termLength = termLength;
            return this;
        }

        public Pricing withAdditionalBandwidth(Integer additionalBandwidth) {
            this.additionalBandwidth = additionalBandwidth;
            return this;
        }

        public Pricing withDeviceManagementType(DeviceManagementType deviceManagementType) {
            this.deviceManagementType = deviceManagementType;
            return this;
        }

        public Pricing withCore(Integer core) {
            this.core = core;
            return this;
        }

        public Pricing withSecondary(Integer secondaryAccountNumber, MetroCode secondaryMetro, Integer secondaryAdditionalBandwidth) {
            this.secondaryAccountNumber = secondaryAccountNumber;
            this.secondaryMetro = secondaryMetro;
            this.secondaryAdditionalBandwidth = secondaryAdditionalBandwidth;
            return this;
        }

        public Pricing withAccountUcmId(String accountUcmId) {
            this.accountUcmId = accountUcmId;
            return this;
        }

        public Pricing build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("accountNumber", ModelUtils.singleValueList(this.accountNumber));
            this.queryParameters.put("metro", ModelUtils.singleValueList(this.metro));
            this.queryParameters.put("vendorPackage", ModelUtils.singleValueList(this.vendorPackage));
            this.queryParameters.put("licenseType", ModelUtils.singleValueList(this.licenseType != null ? this.licenseType.getQueryValue() : null));
            this.queryParameters.put("softwarePackage", ModelUtils.singleValueList(this.softwarePackage));
            this.queryParameters.put("throughput", ModelUtils.singleValueList(this.throughput));
            this.queryParameters.put("throughputUnit", ModelUtils.singleValueList(this.throughputUnit));
            this.queryParameters.put("termLength", ModelUtils.singleValueList(this.termLength));
            this.queryParameters.put("additionalBandwidth", ModelUtils.singleValueList(this.additionalBandwidth));
            this.queryParameters.put("deviceManagementType", ModelUtils.singleValueList(this.deviceManagementType != null ? this.deviceManagementType.getJsonValue() : null));
            this.queryParameters.put("core", ModelUtils.singleValueList(this.core));

            this.queryParameters.put("secondaryAccountNumber", ModelUtils.singleValueList(this.secondaryAccountNumber));
            this.queryParameters.put("secondaryMetro", ModelUtils.singleValueList(this.secondaryMetro));
            this.queryParameters.put("secondaryAdditionalBandwidth", ModelUtils.singleValueList(this.secondaryAdditionalBandwidth));
            this.queryParameters.put("accountUcmId", ModelUtils.singleValueList(this.accountUcmId));


            this.wasBuilt = true;
            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OrderSummary extends RequestBuilderBase<OrderSummary> {

        private Integer accountNumber;
        private String accountUcmId;
        private MetroCode metro;
        private String vendorPackage;
        private LicenseType licenseType;
        private Integer throughput;
        private BandwidthUnit throughputUnit;
        private Integer termLength;
        private Integer core;
        private DeviceManagementType deviceManagementType;
        private PackageCode softwarePackage;

        protected OrderSummary builder() {
            return new OrderSummary();
        }

        public OrderSummary withAccountNumber(Integer accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public OrderSummary withMetro(MetroCode metro) {
            this.metro = metro;
            return this;
        }

        public OrderSummary withVendorPackage(String vendorPackage) {
            this.vendorPackage = vendorPackage;
            return this;
        }

        public OrderSummary withLicenseType(LicenseType licenseType) {
            this.licenseType = licenseType;
            return this;
        }

        public OrderSummary withSoftwarePackage(PackageCode softwarePackage) {
            this.softwarePackage = softwarePackage;
            return this;
        }

        public OrderSummary withThroughput(Integer throughput) {
            this.throughput = throughput;
            return this;
        }

        public OrderSummary withThroughputUnit(BandwidthUnit throughputUnit) {
            this.throughputUnit = throughputUnit;
            return this;
        }

        public OrderSummary withTermLength(Integer termLength) {
            this.termLength = termLength;
            return this;
        }

        public OrderSummary withDeviceManagementType(DeviceManagementType deviceManagementType) {
            this.deviceManagementType = deviceManagementType;
            return this;
        }

        public OrderSummary withCore(Integer core) {
            this.core = core;
            return this;
        }

        public OrderSummary withAccountUcmId(String accountUcmId) {
            this.accountUcmId = accountUcmId;
            return this;
        }

        public OrderSummary build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("accountNumber", ModelUtils.singleValueList(this.accountNumber));
            this.queryParameters.put("metro", ModelUtils.singleValueList(this.metro));
            this.queryParameters.put("vendorPackage", ModelUtils.singleValueList(this.vendorPackage));
            this.queryParameters.put("licenseType", ModelUtils.singleValueList(this.licenseType != null ? this.licenseType.getQueryValue() : null));
            this.queryParameters.put("softwarePackage", ModelUtils.singleValueList(this.softwarePackage));
            this.queryParameters.put("throughput", ModelUtils.singleValueList(this.throughput));
            this.queryParameters.put("throughputUnit", ModelUtils.singleValueList(this.throughputUnit));
            this.queryParameters.put("termLength", ModelUtils.singleValueList(this.termLength));
            this.queryParameters.put("deviceManagementType", ModelUtils.singleValueList(this.deviceManagementType != null ? this.deviceManagementType.getJsonValue() : null));
            this.queryParameters.put("core", ModelUtils.singleValueList(this.core));
            this.queryParameters.put("accountUcmId", ModelUtils.singleValueList(this.accountUcmId));

            this.wasBuilt = true;
            return this;
        }
    }
}
