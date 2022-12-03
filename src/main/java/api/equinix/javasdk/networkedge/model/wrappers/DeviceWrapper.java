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

package api.equinix.javasdk.networkedge.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.networkedge.client.internal.implementation.DeviceClientImpl;
import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.DeviceStatus;
import api.equinix.javasdk.networkedge.enums.IPAssignment;
import api.equinix.javasdk.networkedge.enums.LicenseStatus;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.enums.NetworkScope;
import api.equinix.javasdk.networkedge.enums.RedundancyType;
import api.equinix.javasdk.networkedge.enums.Vendor;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.ClusterDetail;
import api.equinix.javasdk.networkedge.model.implementation.Contact;
import api.equinix.javasdk.networkedge.model.implementation.DeviceCore;
import api.equinix.javasdk.networkedge.model.implementation.DeviceVendorConfig;
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.implementation.SupportDetail;
import api.equinix.javasdk.networkedge.model.implementation.UserPublicKey;
import api.equinix.javasdk.networkedge.model.json.DeviceJson;
import api.equinix.javasdk.networkedge.model.json.Pricing;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceUpdaterJson;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>DeviceWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceWrapper extends ResourceImpl<Device> implements Device {

    private DeviceJson json;
    @Getter
    private final Pageable<Device> serviceClient;

    /**
     * <p>Constructor for DeviceWrapper.</p>
     *
     * @param deviceJson a {@link api.equinix.javasdk.networkedge.model.json.DeviceJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public DeviceWrapper(DeviceJson deviceJson, Pageable<Device> serviceClient) {
        this.json = deviceJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUuid() {
        return this.json.getUuid();
    }

    public String getServiceId() {
        return this.json.getServiceId();
    }

    public String getPodName() {
        return this.json.getPodName();
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.json.getName();
    }

    /**
     * <p>getIpType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.IPAssignment} object.
     */
    public IPAssignment getIpType() {
        return this.json.getIpType();
    }

    /**
     * <p>getDeviceTypeCode.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDeviceTypeCode() {
        return this.json.getDeviceTypeCode();
    }

    /**
     * <p>getDeviceTypeName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDeviceTypeName() {
        return this.json.getDeviceTypeName();
    }

    /**
     * <p>getDeviceTypeVendor.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.Vendor} object.
     */
    public Vendor getDeviceTypeVendor() {
        return this.json.getDeviceTypeVendor();
    }

    /**
     * <p>getDeviceTypeCategory.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceCategory} object.
     */
    public DeviceCategory getDeviceTypeCategory() {
        return this.json.getDeviceTypeCategory();
    }

    /**
     * <p>getStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceStatus} object.
     */
    public DeviceStatus getStatus() {
        return this.json.getStatus();
    }

    /**
     * <p>getLicenseStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.LicenseStatus} object.
     */
    public LicenseStatus getLicenseStatus() {
        return this.json.getLicenseStatus();
    }

    /**
     * <p>getClusterSupported.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getClusterSupported() {
        return this.json.getClusterSupported();
    }

    /**
     * <p>getSiblingCustOrgFlag.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getSiblingCustOrgFlag() {
        return this.json.getSiblingCustOrgFlag();
    }

    public Boolean getSupportServicesEnabled() {
        return this.json.getSupportServicesEnabled();
    }

    public List<String> getSupportServicesNotification() {
        return this.json.getSupportServicesNotification();
    }

    /**
     * <p>getIsSubCustomerDevice.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getIsSubCustomerDevice() {
        return this.json.getIsSubCustomerDevice();
    }

    /**
     * <p>getMetroCode.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     */
    public MetroCode getMetroCode() {
        return this.json.getMetroCode();
    }

    /**
     * <p>getMetroName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetroName() {
        return this.json.getMetroName();
    }

    /**
     * <p>getIbx.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getIbx() {
        return this.json.getIbx();
    }

    /**
     * <p>getAclTemplateUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAclTemplateUuid() {
        return this.json.getAclTemplateUuid();
    }

    /**
     * <p>getLicenseFileId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLicenseFileId() {
        return this.json.getLicenseFileId();
    }

    /**
     * <p>getRegion.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.Region} object.
     */
    public Region getRegion() {
        return this.json.getRegion();
    }

    /**
     * <p>getClusterDetails.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.ClusterDetail} object.
     */
    public ClusterDetail getClusterDetails() {
        return this.json.getClusterDetails();
    }

    /**
     * <p>getThroughput.</p>
     *
     * @return a {@link java.lang.Double} object.
     */
    public Double getThroughput() {
        return this.json.getThroughput();
    }

    /**
     * <p>getThroughputUnit.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.BandwidthUnit} object.
     */
    public BandwidthUnit getThroughputUnit() {
        return this.json.getThroughputUnit();
    }

    /**
     * <p>getHostName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getHostName() {
        return this.json.getHostName();
    }

    /**
     * <p>getPackageCode.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPackageCode() {
        return this.json.getPackageCode();
    }

    /**
     * <p>getPackageName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPackageName() {
        return this.json.getPackageName();
    }

    /**
     * <p>getVersion.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVersion() {
        return this.json.getVersion();
    }

    /**
     * <p>getLicenseType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.LicenseType} object.
     */
    public LicenseType getLicenseType() {
        return this.json.getLicenseType();
    }

    /**
     * <p>getDeviceOrderNumber.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDeviceOrderNumber() {
        return this.json.getDeviceOrderNumber();
    }

    /**
     * <p>getLicenseName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLicenseName() {
        return this.json.getLicenseName();
    }

    /**
     * <p>getSshIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSshIpAddress() {
        return this.json.getSshIpAddress();
    }

    /**
     * <p>getSystemIpAddress.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSystemIpAddress() {
        return this.json.getSystemIpAddress();
    }

    /**
     * <p>getPublicIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPublicIp() {
        return this.json.getPublicIp();
    }

    /**
     * <p>getPublicGatewayIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPublicGatewayIp() {
        return this.json.getPublicGatewayIp();
    }

    /**
     * <p>getManagementIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getManagementIp() {
        return this.json.getManagementIp();
    }

    /**
     * <p>getManagementGatewayIp.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getManagementGatewayIp() {
        return this.json.getManagementGatewayIp();
    }

    /**
     * <p>getDeviceSerialNo.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDeviceSerialNo() {
        return this.json.getDeviceSerialNo();
    }

    /**
     * <p>getSshIpFqdn.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSshIpFqdn() {
        return this.json.getSshIpFqdn();
    }

    /**
     * <p>getPrimaryDnsName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPrimaryDnsName() {
        return this.json.getPrimaryDnsName();
    }

    /**
     * <p>getSecondaryDnsName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSecondaryDnsName() {
        return this.json.getSecondaryDnsName();
    }

    /**
     * <p>getAccountNumber.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getAccountNumber() {
        return this.json.getAccountNumber();
    }

    /**
     * <p>getAccountName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAccountName() {
        return this.json.getAccountName();
    }

    /**
     * <p>getSdwanHostname.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSdwanHostname() {
        return this.json.getSdwanHostname();
    }

    /**
     * <p>getSdwanAccountName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSdwanAccountName() {
        return this.json.getSdwanAccountName();
    }

    /**
     * <p>getSiteId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSiteId() {
        return this.json.getSiteId();
    }

    /**
     * <p>getApplianceTag.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getApplianceTag() {
        return this.json.getApplianceTag();
    }

    /**
     * <p>getOrderingContact.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.Contact} object.
     */
    public Contact getOrderingContact() {
        return this.json.getOrderingContact();
    }

    /**
     * <p>getUserPublicKey.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.UserPublicKey} object.
     */
    public UserPublicKey getUserPublicKey() {
        return this.json.getUserPublicKey();
    }

    /**
     * <p>getNotifications.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getNotifications() {
        return this.json.getNotifications();
    }

    /**
     * <p>getVendorConfig.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceVendorConfig} object.
     */
    public DeviceVendorConfig getVendorConfig() {
        return this.json.getVendorConfig();
    }

    /**
     * <p>getRedundancyType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.RedundancyType} object.
     */
    public RedundancyType getRedundancyType() {
        return this.json.getRedundancyType();
    }

    /**
     * <p>getRedundantUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRedundantUuid() {
        return this.json.getRedundantUuid();
    }

    /**
     * <p>getAccountReferenceId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAccountReferenceId() {
        return this.json.getAccountReferenceId();
    }

    /**
     * <p>getPurchaseOrderNumber.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPurchaseOrderNumber() {
        return this.json.getPurchaseOrderNumber();
    }

    /**
     * <p>getOrderReference.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getOrderReference() {
        return this.json.getOrderReference();
    }

    /**
     * <p>getDealId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDealId() {
        return this.json.getDealId();
    }

    /**
     * <p>getTermLength.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getTermLength() {
        return this.json.getTermLength();
    }

    /**
     * <p>getBillingCommencementDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    public LocalDateTime getBillingCommencementDate() {
        return this.json.getBillingCommencementDate();
    }

    /**
     * <p>getBillingEnabled.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getBillingEnabled() {
        return this.json.getBillingEnabled();
    }

    /**
     * <p>getAdditionalBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getAdditionalBandwidth() {
        return this.json.getAdditionalBandwidth();
    }

    /**
     * <p>getInterfaceCount.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getInterfaceCount() {
        return this.json.getInterfaceCount();
    }

    /**
     * <p>getCore.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.DeviceCore} object.
     */
    public DeviceCore getCore() {
        return this.json.getCore();
    }

    /**
     * <p>getDeviceManagementType.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceManagementType} object.
     */
    public DeviceManagementType getDeviceManagementType() {
        return this.json.getDeviceManagementType();
    }

    /**
     * <p>getNetworkScope.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.NetworkScope} object.
     */
    public NetworkScope getNetworkScope() {
        return this.json.getNetworkScope();
    }

    /**
     * <p>getInterfaces.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<NetworkInterface> getInterfaces() {
        return this.json.getInterfaces();
    }

    /**
     * <p>getAsn.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getAsn() {
        return this.json.getAsn();
    }

    /**
     * <p>getSupportDetails.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.SupportDetail} object.
     */
    public SupportDetail getSupportDetails() {
        return this.json.getSupportDetails();
    }

    /**
     * <p>getCreatedBy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCreatedBy() {
        return this.json.getCreatedBy();
    }

    /**
     * <p>getCreatedDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    public LocalDateTime getCreatedDate() {
        return this.json.getCreatedDate();
    }

    /**
     * <p>getLastUpdatedBy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLastUpdatedBy() {
        return this.json.getLastUpdatedBy();
    }

    /**
     * <p>getLastUpdatedDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    public LocalDateTime getLastUpdatedDate() {
        return this.json.getLastUpdatedDate();
    }

    /**
     * <p>getDiverseFromDeviceUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDiverseFromDeviceUuid() {
        return this.json.getDiverseFromDeviceUuid();
    }

    /**
     * <p>getDiverseFromDeviceName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDiverseFromDeviceName() {
        return this.json.getDiverseFromDeviceName();
    }

    /**
     * <p>getPricing.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    public Pricing getPricing() {
        return ((DeviceClientImpl)this.serviceClient).getPricing(this.getUuid());
    }

    /**
     * {@inheritDoc}
     *
     * <p>restoreFromBackup.</p>
     *
     * @param backup a {@link api.equinix.javasdk.networkedge.model.Backup} object.
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean restoreFromBackup(Backup backup){
        return restoreFromBackup(backup.getUuid());
    }

    /** {@inheritDoc} */
    public Boolean restoreFromBackup(String backupUuid){
        return ((DeviceClientImpl)this.serviceClient).restore(this.getUuid(), backupUuid);
    }

    /** {@inheritDoc} */
    public Boolean updateAdditionalBandwidth(Integer additionalBandwidth) {
        this.json =  ((DeviceClientImpl)this.serviceClient).updateAdditionalBandwidth(this.getUuid(), additionalBandwidth);
        return true;
    }

    /**
     * <p>postLicenseFile.</p>
     *
     * @param fileContents a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String postLicenseFile(String fileContents) {
        return ((DeviceClientImpl)this.serviceClient).postLicenseFile(this.getUuid(), fileContents);
    }

    /**
     * <p>updateLicenseToken.</p>
     *
     * @param licenseToken a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String updateLicenseToken(String licenseToken) {
        return ((DeviceClientImpl)this.serviceClient).updateLicenseToken(this.getUuid(), licenseToken);
    }

    /**
     * <p>ping.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean ping() {
        return ((DeviceClientImpl)this.serviceClient).ping(this.getUuid());
    }

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceOperator.DeviceUpdater} object.
     */
    public DeviceOperator.DeviceUpdater update() {
        return new DeviceOperator(this.serviceClient).update(this.json);
    }

    /** {@inheritDoc} */
    public Boolean save(DeviceUpdaterJson updaterJson) {
        this.json = ((DeviceClientImpl)this.serviceClient).update(this.getUuid(), updaterJson);
        return true;
    }

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean delete() {
        return ((DeviceClientImpl)this.serviceClient).delete(this.getUuid());
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean refresh() {
        this.json = ((DeviceClientImpl)this.serviceClient).refresh(this.getUuid());
        return true;
    }
}
