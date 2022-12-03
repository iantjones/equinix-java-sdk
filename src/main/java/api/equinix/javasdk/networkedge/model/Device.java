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
import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.DeviceStatus;
import api.equinix.javasdk.networkedge.enums.IPAssignment;
import api.equinix.javasdk.networkedge.enums.LicenseStatus;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.enums.NetworkScope;
import api.equinix.javasdk.networkedge.enums.RedundancyType;
import api.equinix.javasdk.networkedge.enums.Vendor;
import api.equinix.javasdk.networkedge.model.implementation.ClusterDetail;
import api.equinix.javasdk.networkedge.model.implementation.Contact;
import api.equinix.javasdk.networkedge.model.implementation.DeviceCore;
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
 * <p>Device interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Device {
    
    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    String getServiceId();

    String getPodName();

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getName();

    /**
     * <p>getIpType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.IPAssignment} object.
     */
    IPAssignment getIpType();

    /**
     * <p>getDeviceTypeCode.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDeviceTypeCode();

    /**
     * <p>getDeviceTypeName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDeviceTypeName();

    /**
     * <p>getDeviceTypeVendor.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.Vendor} object.
     */
    Vendor getDeviceTypeVendor();

    /**
     * <p>getDeviceTypeCategory.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceCategory} object.
     */
    DeviceCategory getDeviceTypeCategory();

    /**
     * <p>getStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceStatus} object.
     */
    DeviceStatus getStatus();

    /**
     * <p>getLicenseStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.LicenseStatus} object.
     */
    LicenseStatus getLicenseStatus();

    /**
     * <p>getClusterSupported.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean getClusterSupported();

    /**
     * <p>getSiblingCustOrgFlag.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean getSiblingCustOrgFlag();

    /**
     * <p>getIsSubCustomerDevice.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean getIsSubCustomerDevice();

    Boolean getSupportServicesEnabled();

    List<String> getSupportServicesNotification();

    /**
     * <p>getMetroCode.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     */
    MetroCode getMetroCode();

    /**
     * <p>getMetroName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getMetroName();

    /**
     * <p>getIbx.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getIbx();

    /**
     * <p>getAclTemplateUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getAclTemplateUuid();

    /**
     * <p>getLicenseFileId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getLicenseFileId();

    /**
     * <p>getRegion.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.Region} object.
     */
    Region getRegion();

    /**
     * <p>getClusterDetails.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.ClusterDetail} object.
     */
    ClusterDetail getClusterDetails();

    /**
     * <p>getThroughput.</p>
     *
     * @return a {@link java.lang.Double} object.
     */
    Double getThroughput();

    /**
     * <p>getThroughputUnit.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.BandwidthUnit} object.
     */
    BandwidthUnit getThroughputUnit();

    /**
     * <p>getHostName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getHostName();

    /**
     * <p>getPackageCode.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPackageCode();

    /**
     * <p>getPackageName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPackageName();

    /**
     * <p>getVersion.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getVersion();

    /**
     * <p>getLicenseType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.LicenseType} object.
     */
    LicenseType getLicenseType();

    /**
     * <p>getDeviceOrderNumber.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDeviceOrderNumber();

    /**
     * <p>getLicenseName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getLicenseName();

    /**
     * <p>getSshIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSshIpAddress();

    /**
     * <p>getSystemIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSystemIpAddress();

    /**
     * <p>getPublicIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPublicIp();

    /**
     * <p>getPublicGatewayIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPublicGatewayIp();

    /**
     * <p>getManagementIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getManagementIp();

    /**
     * <p>getManagementGatewayIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getManagementGatewayIp();

    /**
     * <p>getDeviceSerialNo.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDeviceSerialNo();

    /**
     * <p>getSshIpFqdn.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSshIpFqdn();

    /**
     * <p>getPrimaryDnsName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPrimaryDnsName();

    /**
     * <p>getSecondaryDnsName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSecondaryDnsName();

    /**
     * <p>getAccountNumber.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getAccountNumber();

    /**
     * <p>getAccountName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getAccountName();

    /**
     * <p>getSdwanHostname.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSdwanHostname();

    /**
     * <p>getSdwanAccountName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSdwanAccountName();

    /**
     * <p>getSiteId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSiteId();

    /**
     * <p>getApplianceTag.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getApplianceTag();

    /**
     * <p>getOrderingContact.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.Contact} object.
     */
    Contact getOrderingContact();

    /**
     * <p>getUserPublicKey.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.UserPublicKey} object.
     */
    UserPublicKey getUserPublicKey();

    /**
     * <p>getNotifications.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    ArrayList<String> getNotifications();

    /**
     * <p>getVendorConfig.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceVendorConfig} object.
     */
    DeviceVendorConfig getVendorConfig();

    /**
     * <p>getRedundancyType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.RedundancyType} object.
     */
    RedundancyType getRedundancyType();

    /**
     * <p>getRedundantUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getRedundantUuid();

    /**
     * <p>getAccountReferenceId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getAccountReferenceId();

    /**
     * <p>getPurchaseOrderNumber.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPurchaseOrderNumber();

    /**
     * <p>getOrderReference.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getOrderReference();

    /**
     * <p>getDealId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDealId();

    /**
     * <p>getTermLength.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getTermLength();

    /**
     * <p>getBillingCommencementDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    LocalDateTime getBillingCommencementDate();

    /**
     * <p>getBillingEnabled.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean getBillingEnabled();

    /**
     * <p>getAdditionalBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getAdditionalBandwidth();

    /**
     * <p>getInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getInterfaceCount();

    /**
     * <p>getCore.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceCore} object.
     */
    DeviceCore getCore();

    /**
     * <p>getDeviceManagementType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceManagementType} object.
     */
    DeviceManagementType getDeviceManagementType();

    /**
     * <p>getNetworkScope.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.NetworkScope} object.
     */
    NetworkScope getNetworkScope();

    /**
     * <p>getInterfaces.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    ArrayList<NetworkInterface> getInterfaces();

    /**
     * <p>getAsn.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getAsn();

    /**
     * <p>getSupportDetails.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.SupportDetail} object.
     */
    SupportDetail getSupportDetails();

    /**
     * <p>getCreatedBy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getCreatedBy();

    /**
     * <p>getCreatedDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    LocalDateTime getCreatedDate();

    /**
     * <p>getLastUpdatedBy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getLastUpdatedBy();

    /**
     * <p>getLastUpdatedDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    LocalDateTime getLastUpdatedDate();

    /**
     * <p>getDiverseFromDeviceUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDiverseFromDeviceUuid();

    /**
     * <p>getDiverseFromDeviceName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDiverseFromDeviceName();

    /**
     * <p>getPricing.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    Pricing getPricing();

    /**
     * <p>restoreFromBackup.</p>
     *
     * @param backup a {@link api.equinix.javasdk.networkedge.model.Backup} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean restoreFromBackup(Backup backup);

    /**
     * <p>restoreFromBackup.</p>
     *
     * @param backupUuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean restoreFromBackup(String backupUuid);

    /**
     * <p>updateAdditionalBandwidth.</p>
     *
     * @param additionalBandwidth a {@link java.lang.Integer} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean updateAdditionalBandwidth(Integer additionalBandwidth);

    /**
     * <p>ping.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean ping();

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceUpdater} object.
     */
    DeviceOperator.DeviceUpdater update();

    /**
     * <p>save.</p>
     *
     * @param updaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean save(DeviceUpdaterJson updaterJson);

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete();

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean refresh();
}
