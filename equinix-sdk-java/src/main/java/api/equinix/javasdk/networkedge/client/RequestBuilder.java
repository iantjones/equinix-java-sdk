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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>RequestBuilder class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class RequestBuilder {

    /**
     * <p>device.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.Device} object.
     */
    public static RequestBuilder.Device device() {
        return new RequestBuilder.Device();
    }

    /**
     * <p>sshUser.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.SSHUser} object.
     */
    public static SSHUser sshUser() {
        return new SSHUser();
    }

    /**
     * <p>dnsLookup.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.DNSLookup} object.
     */
    public static DNSLookup dnsLookup() {
        return new DNSLookup();
    }

    /**
     * <p>vpn.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.VPN} object.
     */
    public static VPN vpn() {
        return new VPN();
    }
    
    /**
     * <p>backup.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.Backup} object.
     */
    public static Backup backup() {
        return new Backup();
    }

    public static OrderSummary orderSummary() {
        return new OrderSummary();
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
    public static class SSHUser extends RequestBuilderBase<SSHUser> {

        private String username;
        private String deviceUuid;
        private Boolean verbose;
        private String accountUcmId;

        protected SSHUser builder() {
            return new SSHUser();
        }

        public SSHUser forUsername(String username) {
            this.username = username;
            return this;
        }

        public SSHUser forDeviceUuid(String deviceUuid) {
            this.deviceUuid = deviceUuid;
            return this;
        }

        public SSHUser verbose() {
            this.verbose = true;
            return this;
        }

        public SSHUser forAccount(String accountUcmId) {
            this.accountUcmId = accountUcmId;
            return this;
        }

        public SSHUser build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("username", ModelUtils.singleValueList(this.username));
            this.queryParameters.put("virtualDeviceUuid", ModelUtils.singleValueList(this.deviceUuid));
            this.queryParameters.put("verbose", ModelUtils.singleValueList(this.verbose));
            this.queryParameters.put("accountUcmId", ModelUtils.singleValueList(this.accountUcmId));

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

            this.queryParameters.put("metroCode", ModelUtils.stringListFromEnumList(this.metroCodes));
            this.queryParameters.put("virtualDeviceUuid", ModelUtils.singleValueList(this.deviceUuid));
            this.queryParameters.put("accountUcmId", ModelUtils.singleValueList(this.accountUcmId));
            this.queryParameters.put("groupUuid", ModelUtils.singleValueList(this.groupUuid));
            this.queryParameters.put("groupName", ModelUtils.singleValueList(this.groupName));

            this.wasBuilt = true;
            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DNSLookup extends RequestBuilderBase<DNSLookup> {

        private List<String> fqdns;
        private MetroCode metroCode;
        @Getter  private DNSLookupRequest dnsLookupRequest;

        protected DNSLookup builder() {
            return new DNSLookup();
        }

        public DNSLookup forFqdn(String fqdn) {
            if(this.fqdns == null) {
                this.fqdns = new ArrayList<>();
            }
            this.fqdns.add(fqdn);
            return this;
        }

        public DNSLookup inMetro(MetroCode metroCode) {
            this.metroCode = metroCode;
            return this;
        }

        public DNSLookup build() {
            dnsLookupRequest = new DNSLookupRequest(this.fqdns, this.metroCode);
            this.wasBuilt = true;
            return this;
        }

        @RequiredArgsConstructor
        @Getter
        public static class DNSLookupRequest {
            private final List<String> fqdns;
            private final MetroCode metroCode;
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
            this.queryParameters.put("licenseType", ModelUtils.singleValueList(this.licenseType));
            this.queryParameters.put("softwarePackage", ModelUtils.singleValueList(this.softwarePackage));
            this.queryParameters.put("throughput", ModelUtils.singleValueList(this.throughput));
            this.queryParameters.put("throughputUnit", ModelUtils.singleValueList(this.throughputUnit));
            this.queryParameters.put("termLength", ModelUtils.singleValueList(this.termLength));
            this.queryParameters.put("additionalBandwidth", ModelUtils.singleValueList(this.additionalBandwidth));
            this.queryParameters.put("deviceManagementType", ModelUtils.singleValueList(this.deviceManagementType));
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
            this.queryParameters.put("licenseType", ModelUtils.singleValueList(this.licenseType));
            this.queryParameters.put("softwarePackage", ModelUtils.singleValueList(this.softwarePackage));
            this.queryParameters.put("throughput", ModelUtils.singleValueList(this.throughput));
            this.queryParameters.put("throughputUnit", ModelUtils.singleValueList(this.throughputUnit));
            this.queryParameters.put("termLength", ModelUtils.singleValueList(this.termLength));
            this.queryParameters.put("deviceManagementType", ModelUtils.singleValueList(this.deviceManagementType.toString().replace("_", "-")));
            this.queryParameters.put("core", ModelUtils.singleValueList(this.core));
            this.queryParameters.put("accountUcmId", ModelUtils.singleValueList(this.accountUcmId));

            this.wasBuilt = true;
            return this;
        }
    }
}
