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

package com.eqixiac.equinix.networkedge.model;

import com.eqixiac.equinix.core.enums.BandwidthUnit;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.networkedge.enums.Connectivity;
import com.eqixiac.equinix.networkedge.enums.DeviceCategory;
import com.eqixiac.equinix.networkedge.enums.DeviceManagementType;
import com.eqixiac.equinix.networkedge.enums.DevicePlane;
import com.eqixiac.equinix.networkedge.enums.DeviceStatus;
import com.eqixiac.equinix.networkedge.enums.LicenseStatus;
import com.eqixiac.equinix.networkedge.enums.LicenseType;
import com.eqixiac.equinix.networkedge.enums.RedundancyType;
import com.eqixiac.equinix.networkedge.enums.SshIpFqdnStatus;
import com.eqixiac.equinix.networkedge.enums.Vendor;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceCore;
import com.eqixiac.equinix.networkedge.model.implementation.DevicePricingDetail;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceVendorConfig;
import com.eqixiac.equinix.networkedge.model.implementation.NetworkInterface;
import com.eqixiac.equinix.networkedge.model.implementation.UserPublicKey;
import com.eqixiac.equinix.networkedge.model.json.Pricing;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceOperator;
import com.eqixiac.equinix.networkedge.model.json.creators.DeviceUpdaterJson;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * A Network Edge virtual device. Read-side accessors mirror the catalog spec's
 * {@code VirtualDeviceDetailsResponse} schema.
 *
 * @author ianjones
 */
public interface Device {
    
    String getUuid();

    String getName();

    String getDeviceTypeCode();

    String getDeviceTypeName();

    Vendor getDeviceTypeVendor();

    DeviceCategory getDeviceTypeCategory();

    DeviceStatus getStatus();

    LicenseStatus getLicenseStatus();

    MetroCode getMetroCode();

    String getMetroName();

    String getLicenseFileId();

    Region getRegion();

    Double getThroughput();

    BandwidthUnit getThroughputUnit();

    String getHostName();

    String getPackageCode();

    String getPackageName();

    String getVersion();

    LicenseType getLicenseType();

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

    String getSiteId();

    UserPublicKey getUserPublicKey();

    ArrayList<String> getNotifications();

    DeviceVendorConfig getVendorConfig();

    RedundancyType getRedundancyType();

    String getRedundantUuid();

    String getPurchaseOrderNumber();

    Integer getTermLength();

    Integer getAdditionalBandwidth();

    Integer getInterfaceCount();

    DeviceCore getCore();

    DeviceManagementType getDeviceManagementType();

    ArrayList<NetworkInterface> getInterfaces();

    Long getAsn();

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
