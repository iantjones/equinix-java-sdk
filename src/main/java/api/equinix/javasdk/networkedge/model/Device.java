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

package api.equinix.javasdk.networkedge.model;

import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.networkedge.enums.Connectivity;
import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.DevicePlane;
import api.equinix.javasdk.networkedge.enums.DeviceStatus;
import api.equinix.javasdk.networkedge.enums.IPAssignment;
import api.equinix.javasdk.networkedge.enums.LicenseStatus;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.enums.NetworkScope;
import api.equinix.javasdk.networkedge.enums.RedundancyType;
import api.equinix.javasdk.networkedge.enums.SshIpFqdnStatus;
import api.equinix.javasdk.networkedge.enums.Vendor;
import api.equinix.javasdk.networkedge.model.implementation.ClusterDetail;
import api.equinix.javasdk.networkedge.model.implementation.Contact;
import api.equinix.javasdk.networkedge.model.implementation.DeviceCore;
import api.equinix.javasdk.networkedge.model.implementation.DevicePricingDetail;
import api.equinix.javasdk.networkedge.model.implementation.DeviceVendorConfig;
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.implementation.SupportDetail;
import api.equinix.javasdk.networkedge.model.implementation.UserPublicKey;
import api.equinix.javasdk.networkedge.model.json.Pricing;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianjones
 */
public interface Device {
    
    String getUuid();

    String getServiceId();

    String getPodName();

    String getName();

    IPAssignment getIpType();

    String getDeviceTypeCode();

    String getDeviceTypeName();

    Vendor getDeviceTypeVendor();

    DeviceCategory getDeviceTypeCategory();

    DeviceStatus getStatus();

    LicenseStatus getLicenseStatus();

    Boolean getClusterSupported();

    Boolean getSiblingCustOrgFlag();

    Boolean getIsSubCustomerDevice();

    Boolean getSupportServicesEnabled();

    List<String> getSupportServicesNotification();

    MetroCode getMetroCode();

    String getMetroName();

    String getIbx();

    String getAclTemplateUuid();

    String getLicenseFileId();

    Region getRegion();

    ClusterDetail getClusterDetails();

    Double getThroughput();

    BandwidthUnit getThroughputUnit();

    String getHostName();

    String getPackageCode();

    String getPackageName();

    String getVersion();

    LicenseType getLicenseType();

    String getDeviceOrderNumber();

    String getLicenseName();

    String getSshIpAddress();

    String getSystemIpAddress();

    String getPublicIp();

    String getPublicGatewayIp();

    String getManagementIp();

    String getManagementGatewayIp();

    String getDeviceSerialNo();

    String getSshIpFqdn();

    String getPrimaryDnsName();

    String getSecondaryDnsName();

    String getAccountNumber();

    String getAccountName();

    String getSdwanHostname();

    String getSdwanAccountName();

    String getSiteId();

    String getApplianceTag();

    Contact getOrderingContact();

    UserPublicKey getUserPublicKey();

    ArrayList<String> getNotifications();

    DeviceVendorConfig getVendorConfig();

    RedundancyType getRedundancyType();

    String getRedundantUuid();

    String getAccountReferenceId();

    String getPurchaseOrderNumber();

    String getOrderReference();

    String getDealId();

    Integer getTermLength();

    LocalDateTime getBillingCommencementDate();

    Boolean getBillingEnabled();

    Integer getAdditionalBandwidth();

    Integer getInterfaceCount();

    DeviceCore getCore();

    DeviceManagementType getDeviceManagementType();

    NetworkScope getNetworkScope();

    ArrayList<NetworkInterface> getInterfaces();

    Integer getAsn();

    SupportDetail getSupportDetails();

    String getCreatedBy();

    LocalDateTime getCreatedDate();

    String getLastUpdatedBy();

    LocalDateTime getLastUpdatedDate();

    String getDiverseFromDeviceUuid();

    String getDiverseFromDeviceName();

    String getExpiry();

    SshIpFqdnStatus getSshIpFqdnStatus();

    Connectivity getConnectivity();

    DevicePricingDetail getPricingDetails();

    DevicePlane getPlane();

    String getNewTermLength();

    String getChannelPartner();

    Pricing getPricing();

    Boolean restoreFromBackup(Backup backup);

    /**
     *
     * @param backupName the name of the backup ({@code DeviceBackupUpdateRequest.name}), required by the API.
     */
    Boolean restoreFromBackup(String backupUuid, String backupName);

    Boolean updateAdditionalBandwidth(Integer additionalBandwidth);

    Boolean ping();

    DeviceOperator.DeviceUpdater update();

    Boolean save(DeviceUpdaterJson updaterJson);

    Boolean delete();

    Boolean refresh();
}
