package com.eqixiac.equinix;

import com.eqixiac.equinix.core.IntegrationTestBase;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.enums.BackupStatus;
import com.eqixiac.equinix.networkedge.enums.DeviceManagementType;
import com.eqixiac.equinix.networkedge.enums.DeviceStatus;
import com.eqixiac.equinix.networkedge.enums.LicenseType;
import com.eqixiac.equinix.networkedge.enums.PackageCode;
import com.eqixiac.equinix.networkedge.model.ACLTemplate;
import com.eqixiac.equinix.networkedge.model.Account;
import com.eqixiac.equinix.networkedge.model.BGPPeering;
import com.eqixiac.equinix.networkedge.model.Backup;
import com.eqixiac.equinix.networkedge.model.Device;
import com.eqixiac.equinix.networkedge.model.DeviceLink;
import com.eqixiac.equinix.networkedge.model.DeviceType;
import com.eqixiac.equinix.networkedge.model.Metro;
import com.eqixiac.equinix.networkedge.model.PublicKey;
import com.eqixiac.equinix.networkedge.model.RestoreFeasibility;
import com.eqixiac.equinix.networkedge.model.VPN;
import com.eqixiac.equinix.networkedge.model.implementation.AgreementStatus;
import com.eqixiac.equinix.networkedge.model.implementation.AllowedInterfaceResponse;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceACL;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceReboot;
import com.eqixiac.equinix.networkedge.model.implementation.DeviceUpgrade;
import com.eqixiac.equinix.networkedge.model.implementation.DownloadableImage;
import com.eqixiac.equinix.networkedge.model.implementation.DowntimeNotification;
import com.eqixiac.equinix.networkedge.model.implementation.InterfaceStats;
import com.eqixiac.equinix.networkedge.model.implementation.NetworkInterface;
import com.eqixiac.equinix.networkedge.model.json.Pricing;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration tests for the Network Edge domain of the Equinix Java SDK.
 *
 * <p>The live tier proves spec-vs-reality: every safe operation the SDK exposes for
 * {@code network-edgev1.yaml} is called against the real API, and the response is
 * deserialized into the SDK's models. A read that fails for any reason other than an
 * entitlement gap (401/403) fails the test — see
 * {@code IntegrationTestBase.requireEntitled(String, String, String, String, ApiCall)}.
 *
 * <p>Two tiers of tests are provided here:
 * <ul>
 *     <li><b>integration-readonly</b> - Safe read-only operations (list, get, printable
 *         order summary, pricing lookups, diagnostics).</li>
 *     <li><b>integration-dryrun</b> - Documented validation-only calls; no real mutations
 *         (restore feasibility analysis).</li>
 * </ul>
 * Irreversible Network Edge mutations (device creation, reboot, RMA, restore) are
 * deliberately not exercised outside {@code integration-full}, and none are added here.
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -DaccessKey=ID -DsecretKey=SECRET
 * mvn test -Pintegration-dryrun   -DaccessKey=ID -DsecretKey=SECRET -DtestMode=dryrun
 * </pre>
 */
@Tag("integration-readonly")
@DisplayName("Network Edge Integration Tests")
class NetworkEdgeIntegrationTest extends IntegrationTestBase {

    private static final DateTimeFormatter STATS_WINDOW_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    static NetworkEdge networkEdge;

    // Memoized live discoveries, shared across tests to avoid re-listing per test.
    private static PaginatedList<Device> cachedDevices;
    private static PaginatedList<DeviceType> cachedDeviceTypes;
    private static PaginatedList<Metro> cachedMetros;
    private static List<Account> cachedAccounts;
    private static PaginatedList<Backup> cachedBackups;
    private static String cachedBackupsDeviceUuid;

    @BeforeAll
    static void setUpNetworkEdge() {
        networkEdge = new NetworkEdge(testCredentials());
        networkEdge.authenticate();
    }

    // ── Live discovery helpers ─────────────────────────────────────────

    /** Lists devices once (getVirtualDevices) and caches the result. */
    static PaginatedList<Device> devicesList() {
        if (cachedDevices == null) {
            cachedDevices = requireEntitled("NetworkEdge", "list", "Device", "GET",
                    () -> networkEdge.devices().list());
        }
        return cachedDevices;
    }

    /** Lists device types once (getVirtualDeviceTypes) and caches the result. */
    static PaginatedList<DeviceType> deviceTypesList() {
        if (cachedDeviceTypes == null) {
            cachedDeviceTypes = requireEntitled("NetworkEdge", "listDeviceTypes", "DeviceType", "GET",
                    () -> networkEdge.devices().listDeviceTypes());
        }
        return cachedDeviceTypes;
    }

    /** Lists Network Edge metros once (getMetros) and caches the result. */
    static PaginatedList<Metro> metrosList() {
        if (cachedMetros == null) {
            cachedMetros = requireEntitled("NetworkEdge", "listMetros", "Metro", "GET",
                    () -> networkEdge.setup().listMetros());
        }
        return cachedMetros;
    }

    /**
     * Picks the metro to query accounts in: the metro of an existing device when one exists
     * (an account is guaranteed there), otherwise the first Network Edge metro.
     */
    static MetroCode accountMetro() {
        PaginatedList<Device> devices = devicesList();
        if (devices.size() > 0 && devices.get(0).getMetroCode() != null) {
            return devices.get(0).getMetroCode();
        }
        PaginatedList<Metro> metros = metrosList();
        Assumptions.assumeTrue(metros.size() > 0, "No Network Edge metros returned; skipping");
        return metros.get(0).getMetroCode();
    }

    /** Lists accounts once (getAccountsWithStatus) in {@code accountMetro()} and caches the result. */
    static List<Account> accountsList() {
        if (cachedAccounts == null) {
            MetroCode metro = accountMetro();
            cachedAccounts = requireEntitled("NetworkEdge", "listAccounts", "Account", "GET",
                    () -> networkEdge.setup().listAccounts(metro));
        }
        return cachedAccounts;
    }

    /** Returns the first device on the account or skips the calling test when there are none. */
    static Device requireDevice() {
        PaginatedList<Device> devices = devicesList();
        Assumptions.assumeTrue(devices.size() > 0, "No Network Edge devices on this account; skipping");
        return devices.get(0);
    }

    /** Returns the first PROVISIONED device or skips the calling test when there is none. */
    static Device requireProvisionedDevice() {
        for (Device device : devicesList()) {
            if (device.getStatus() == DeviceStatus.PROVISIONED) {
                return device;
            }
        }
        return Assumptions.abort("No PROVISIONED Network Edge device on this account; skipping");
    }

    /** Lists backups once (getDeviceBackups) for the first device and caches the result. */
    static PaginatedList<Backup> backupsForFirstDevice() {
        Device device = requireDevice();
        if (cachedBackups == null || !device.getUuid().equals(cachedBackupsDeviceUuid)) {
            cachedBackupsDeviceUuid = device.getUuid();
            cachedBackups = requireEntitled("NetworkEdge", "list", "Backup", "GET",
                    () -> networkEdge.backups().list(device.getUuid()));
        }
        return cachedBackups;
    }

    /** Returns the first COMPLETED backup of the first device or skips the calling test. */
    static Backup requireCompletedBackup() {
        for (Backup backup : backupsForFirstDevice()) {
            if (backup.getStatus() == BackupStatus.COMPLETED) {
                return backup;
            }
        }
        return Assumptions.abort("No COMPLETED backup on the first device; skipping");
    }

    /** Maps a device's package code string onto the {@code PackageCode} enum, or null if unmapped. */
    private static PackageCode packageCodeOf(Device device) {
        if (device.getPackageCode() == null) {
            return null;
        }
        try {
            return PackageCode.valueOf(device.getPackageCode());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS - Safe GET/list operations
    // ════════════════════════════════════════════════════════════════════

    /**
     * Setup/provisioning reads: metros, accounts, agreements, terms, pricing,
     * order summaries and downtime notifications.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Setup Read-Only Tests")
    class SetupTests {

        @Test
        @DisplayName("metros_list - List Network Edge metros (getMetros)")
        void metros_list() {
            PaginatedList<Metro> metros = metrosList();
            assertNotNull(metros);
            assertTrue(metros.size() > 0, "Expected at least one Network Edge metro");
            Metro first = metros.get(0);
            assertNotNull(first.getMetroCode());
            first.getRegion();
            first.getMetroDescription();
        }

        @Test
        @DisplayName("accounts_listByMetro - List accounts in a metro (getAccountsWithStatus)")
        void accounts_listByMetro() {
            List<Account> accounts = accountsList();
            assertNotNull(accounts);
            if (!accounts.isEmpty()) {
                Account first = accounts.get(0);
                assertNotNull(first.getAccountNumber());
                first.getAccountName();
                first.getAccountStatus();
                first.getAccountUcmId();
            }
        }

        @Test
        @DisplayName("agreementStatus_get - Agreement status for an account (getAgreementStatus)")
        void agreementStatus_get() {
            List<Account> accounts = accountsList();
            Assumptions.assumeTrue(!accounts.isEmpty(), "No accounts in the probed metro; skipping");
            String accountNumber = String.valueOf(accounts.get(0).getAccountNumber());

            AgreementStatus status = requireEntitled("NetworkEdge", "getAgreementStatus", "AgreementStatus", "GET",
                    () -> networkEdge.setup().getAgreementStatus(accountNumber));
            assertNotNull(status);
            status.getValid();
            status.getTermsVersionId();
            status.getStatus();
        }

        @Test
        @DisplayName("vendorTerms_get - Vendor terms link (getVendorTerms)")
        void vendorTerms_get() {
            String vendorPackage;
            LicenseType licenseType;
            PaginatedList<Device> devices = devicesList();
            if (devices.size() > 0 && devices.get(0).getDeviceTypeCode() != null) {
                vendorPackage = devices.get(0).getDeviceTypeCode();
                licenseType = devices.get(0).getLicenseType() != null
                        ? devices.get(0).getLicenseType() : LicenseType.SUB;
            } else {
                PaginatedList<DeviceType> types = deviceTypesList();
                Assumptions.assumeTrue(types.size() > 0, "No device types in the catalog; skipping");
                vendorPackage = types.get(0).getDeviceTypeCode();
                licenseType = LicenseType.SUB;
            }
            final String pkg = vendorPackage;
            final LicenseType license = licenseType;

            String terms;
            try {
                terms = requireEntitled("NetworkEdge", "getVendorsTerms", "VendorTerms", "GET",
                        () -> networkEdge.setup().getVendorsTerms(pkg, license));
            } catch (EquinixNotFoundException e) {
                // Not every vendor package/license combination publishes terms — data dependence.
                terms = Assumptions.abort("No vendor terms published for " + pkg + "/" + license
                        + ": " + e.getMessage());
            }
            assertNotNull(terms);
            assertFalse(terms.isBlank(), "Vendor terms should not be blank");
        }

        @Test
        @DisplayName("orderTerms_get - Order terms and conditions (getOrderTerms)")
        void orderTerms_get() {
            String terms = requireEntitled("NetworkEdge", "getOrderTerms", "OrderTerms", "GET",
                    () -> networkEdge.setup().getOrderTerms());
            assertNotNull(terms);
            assertFalse(terms.isBlank(), "Order terms should not be blank");
        }

        @Test
        @DisplayName("pricing_getForDevice - Price lookup for an existing device (retrievePrice)")
        void pricing_getForDevice() {
            Device device = requireDevice();
            Pricing pricing = requireEntitled("NetworkEdge", "getPricing", "Pricing", "GET",
                    () -> networkEdge.setup().getPricing(device.getUuid()));
            assertNotNull(pricing);
            pricing.getTermLength();
            pricing.getPrimary();
        }

        @Test
        @DisplayName("orderSummary_get - Printable order summary PDF (getOrderSummary)")
        void orderSummary_get() {
            // Derive an orderable configuration from a live device so the request is
            // known-valid for this account rather than guessed from catalog constants.
            Device device = requireDevice();
            Assumptions.assumeTrue(device.getAccountNumber() != null && device.getMetroCode() != null
                            && device.getDeviceTypeCode() != null && device.getLicenseType() != null,
                    "First device is missing attributes needed to request an order summary; skipping");

            Integer accountNumber;
            try {
                accountNumber = Integer.parseInt(device.getAccountNumber());
            } catch (NumberFormatException e) {
                accountNumber = Assumptions.abort(
                        "Device accountNumber is not numeric: " + device.getAccountNumber());
            }

            RequestBuilder.OrderSummary request = RequestBuilder.orderSummary()
                    .withAccountNumber(accountNumber)
                    .withMetro(device.getMetroCode())
                    .withVendorPackage(device.getDeviceTypeCode())
                    .withLicenseType(device.getLicenseType());
            if (device.getDeviceManagementType() != null) {
                request.withDeviceManagementType(device.getDeviceManagementType());
            }
            if (device.getThroughput() != null) {
                request.withThroughput(device.getThroughput().intValue());
            }
            if (device.getThroughputUnit() != null) {
                request.withThroughputUnit(device.getThroughputUnit());
            }
            if (device.getTermLength() != null) {
                request.withTermLength(device.getTermLength());
            }
            if (device.getCore() != null && device.getCore().getCore() != null) {
                request.withCore(device.getCore().getCore());
            }
            PackageCode packageCode = packageCodeOf(device);
            if (packageCode != null) {
                request.withSoftwarePackage(packageCode);
            }

            byte[] pdf = requireEntitled("NetworkEdge", "getOrderSummary", "OrderSummary", "GET",
                    () -> networkEdge.setup().getOrderSummary(request));
            assertNotNull(pdf);
            assertTrue(pdf.length > 0, "Order summary PDF should not be empty");
        }

        @Test
        @DisplayName("downtimeNotifications_get - Planned/unplanned downtime notifications (getNotifications)")
        void downtimeNotifications_get() {
            DowntimeNotification notification = requireEntitled(
                    "NetworkEdge", "listDowntimeNotifications", "DowntimeNotification", "GET",
                    () -> networkEdge.setup().listDowntimeNotifications());
            assertNotNull(notification);
            notification.getNotificationType();
            notification.getImpactedServices();
        }
    }

    /**
     * Virtual device reads: listings, types, interfaces, diagnostics and per-device
     * histories.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Device Read-Only Tests")
    class DeviceTests {

        @Test
        @DisplayName("devices_listAndGetByUuid - List devices and get by UUID (getVirtualDevices, getVirtualDeviceByUuid)")
        void devices_listAndGetByUuid() {
            PaginatedList<Device> devices = devicesList();
            assertNotNull(devices);

            if (devices.size() > 0) {
                Device first = devices.get(0);
                assertNotNull(first.getUuid());
                first.getName();
                first.getStatus();
                first.getMetroCode();
                first.getDeviceTypeCode();

                Device item = requireEntitled("NetworkEdge", "getByUuid", "Device", "GET",
                        () -> networkEdge.devices().getByUuid(first.getUuid()));
                assertNotNull(item);
                assertEquals(first.getUuid(), item.getUuid());
            }
        }

        @Test
        @DisplayName("deviceTypes_list - List device types (getVirtualDeviceTypes)")
        void deviceTypes_list() {
            PaginatedList<DeviceType> types = deviceTypesList();
            assertNotNull(types);
            assertTrue(types.size() > 0, "Expected at least one device type in the catalog");
            DeviceType first = types.get(0);
            assertNotNull(first.getDeviceTypeCode());
            first.getVendor();
            first.getCategory();
            first.getAvailableMetros();
        }

        @Test
        @DisplayName("allowedInterfaces_list - Allowed interfaces for a device type (getAllowedInterfaces)")
        void allowedInterfaces_list() {
            PaginatedList<DeviceType> types = deviceTypesList();
            Assumptions.assumeTrue(types.size() > 0, "No device types in the catalog; skipping");
            DeviceType type = types.get(0);
            DeviceManagementType managementType =
                    (type.getDeviceManagementTypes() != null && type.getDeviceManagementTypes().getSelfConfigured() != null)
                            ? DeviceManagementType.SELF_CONFIGURED
                            : DeviceManagementType.EQUINIX_CONFIGURED;

            AllowedInterfaceResponse response = requireEntitled(
                    "NetworkEdge", "listAllowedInterfaces", "AllowedInterface", "GET",
                    () -> networkEdge.devices().listAllowedInterfaces(
                            RequestBuilder.allowedInterfaces(type.getDeviceTypeCode(), managementType)));
            assertNotNull(response);
            if (response.getInterfaceProfiles() != null && !response.getInterfaceProfiles().isEmpty()) {
                response.getInterfaceProfiles().get(0).getCount();
            }
        }

        @Test
        @DisplayName("deviceInterfaces_list - Interface status of a device (getVirtualDeviceInterfacesByUuid)")
        void deviceInterfaces_list() {
            Device device = requireDevice();
            List<NetworkInterface> interfaces = requireEntitled(
                    "NetworkEdge", "listInterfaces", "NetworkInterface", "GET",
                    () -> networkEdge.devices().listInterfaces(device.getUuid()));
            assertNotNull(interfaces);
            if (!interfaces.isEmpty()) {
                NetworkInterface first = interfaces.get(0);
                first.getId();
                first.getName();
                first.getType();
                first.getStatus();
            }
        }

        @Test
        @DisplayName("interfaceStats_get - Interface throughput statistics (getInterfaceStatisticsByUuid)")
        void interfaceStats_get() {
            Device device = requireProvisionedDevice();
            List<NetworkInterface> interfaces = requireEntitled(
                    "NetworkEdge", "listInterfaces", "NetworkInterface", "GET",
                    () -> networkEdge.devices().listInterfaces(device.getUuid()));
            Assumptions.assumeTrue(interfaces != null && !interfaces.isEmpty(),
                    "Device has no interfaces; skipping stats");
            NetworkInterface nic = interfaces.get(0);
            Assumptions.assumeTrue(nic.getId() != null, "First interface has no id; skipping stats");

            Instant now = Instant.now();
            String start = STATS_WINDOW_FORMAT.format(now.minus(Duration.ofDays(1)));
            String end = STATS_WINDOW_FORMAT.format(now);

            InterfaceStats stats = requireEntitled(
                    "NetworkEdge", "getInterfaceStatistics", "InterfaceStats", "GET",
                    () -> networkEdge.devices().getInterfaceStatistics(device.getUuid(), nic.getId(), start, end));
            assertNotNull(stats);
            if (stats.getStats() != null) {
                stats.getStats().getInbound();
                stats.getStats().getOutbound();
            }
        }

        @Test
        @DisplayName("reloadHistory_list - Soft-reboot/reload history of a device (getDeviceReloadByUuid)")
        void reloadHistory_list() {
            Device device = requireDevice();
            // The SDK maps an empty history body to null, so only the shape of a non-empty
            // response is asserted here.
            List<DeviceReboot> history = requireEntitled(
                    "NetworkEdge", "listReloadHistory", "DeviceReboot", "GET",
                    () -> networkEdge.devices().listReloadHistory(device.getUuid()));
            if (history != null && !history.isEmpty()) {
                DeviceReboot first = history.get(0);
                first.getStatus();
                first.getRequestedBy();
                first.getRequestedDateTime();
            }
        }

        @Test
        @DisplayName("upgradeHistory_list - Resource-upgrade history of a device (getDeviceUpgradeByUuid)")
        void upgradeHistory_list() {
            Device device = requireDevice();
            // The SDK maps an empty history body to null, so only the shape of a non-empty
            // response is asserted here.
            List<DeviceUpgrade> history = requireEntitled(
                    "NetworkEdge", "listUpgradeHistory", "DeviceUpgrade", "GET",
                    () -> networkEdge.devices().listUpgradeHistory(device.getUuid()));
            if (history != null && !history.isEmpty()) {
                DeviceUpgrade first = history.get(0);
                first.getStatus();
                first.getRequestedBy();
                first.getRequestedDateTime();
            }
        }

        @Test
        @DisplayName("deviceAcl_get - ACL templates associated with a device (getDeviceTemplatesByUuid)")
        void deviceAcl_get() {
            Device device = requireDevice();
            DeviceACL acl;
            try {
                acl = requireEntitled("NetworkEdge", "getDeviceAcl", "DeviceACL", "GET",
                        () -> networkEdge.devices().getDeviceAcl(device.getUuid()));
            } catch (EquinixNotFoundException e) {
                // A device with no ACL templates attached is data dependence, not a defect.
                acl = Assumptions.abort("Device has no ACL templates associated: " + e.getMessage());
            }
            assertNotNull(acl);
            acl.getAclTemplate();
            acl.getMgmtAclTemplate();
        }

        @Test
        @DisplayName("device_ping - Reachability diagnostic for a self-configured device (pingDeviceByUuid)")
        void device_ping() {
            // Per spec only SELF-CONFIGURED devices support ping, so pick one that is
            // provisioned; skip when the account has none.
            Device pingable = null;
            for (Device device : devicesList()) {
                if (device.getDeviceManagementType() == DeviceManagementType.SELF_CONFIGURED
                        && device.getStatus() == DeviceStatus.PROVISIONED) {
                    pingable = device;
                    break;
                }
            }
            Assumptions.assumeTrue(pingable != null,
                    "No provisioned self-configured device to ping; skipping");
            final Device target = pingable;

            Boolean reachable = requireEntitled("NetworkEdge", "ping", "Device", "GET", target::ping);
            assertNotNull(reachable);
        }

        @Test
        @DisplayName("downloadableImages_list - Downloadable images of a device type (getDownloadableImagesByDeviceType)")
        void downloadableImages_list() {
            PaginatedList<DeviceType> types = deviceTypesList();
            Assumptions.assumeTrue(types.size() > 0, "No device types in the catalog; skipping");
            String deviceTypeCode = types.get(0).getDeviceTypeCode();

            List<DownloadableImage> images;
            try {
                images = requireEntitled("NetworkEdge", "listDownloadableImages", "DownloadableImage", "GET",
                        () -> networkEdge.devices().listDownloadableImages(deviceTypeCode));
            } catch (EquinixNotFoundException e) {
                // Not every device type publishes downloadable images — data dependence.
                images = Assumptions.abort("No downloadable images for device type " + deviceTypeCode
                        + ": " + e.getMessage());
            }
            assertNotNull(images);
            if (!images.isEmpty()) {
                DownloadableImage first = images.get(0);
                first.getImageName();
                first.getVersion();
                first.getDeviceType();
            }
        }
    }

    /** ACL template reads. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("ACL Template Read-Only Tests")
    class ACLTemplateTests {

        @Test
        @DisplayName("aclTemplates_listAndGetByUuid - List ACL templates and get by UUID (getDeviceACLTemplate, getDeviceTemplateByUuid)")
        void aclTemplates_listAndGetByUuid() {
            PaginatedList<ACLTemplate> templates = requireEntitled("NetworkEdge", "list", "ACLTemplate", "GET",
                    () -> networkEdge.aclTemplates().list());
            assertNotNull(templates);

            if (templates.size() > 0) {
                ACLTemplate first = templates.get(0);
                assertNotNull(first.getUuid());
                first.getName();
                first.getStatus();

                ACLTemplate item = requireEntitled("NetworkEdge", "getByUuid", "ACLTemplate", "GET",
                        () -> networkEdge.aclTemplates().getByUuid(first.getUuid()));
                assertNotNull(item);
                assertEquals(first.getUuid(), item.getUuid());
            }
        }
    }

    /** Device link group reads. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Device Link Read-Only Tests")
    class DeviceLinkTests {

        @Test
        @DisplayName("deviceLinks_listAndGetByUuid - List device links and get by UUID (getDeviceLinkGroups, getDeviceLinkGroupByUUID)")
        void deviceLinks_listAndGetByUuid() {
            PaginatedList<DeviceLink> links = requireEntitled("NetworkEdge", "list", "DeviceLink", "GET",
                    () -> networkEdge.deviceLinks().list());
            assertNotNull(links);

            if (links.size() > 0) {
                DeviceLink first = links.get(0);
                assertNotNull(first.getUuid());
                first.getGroupName();
                first.getStatus();

                DeviceLink item = requireEntitled("NetworkEdge", "getByUuid", "DeviceLink", "GET",
                        () -> networkEdge.deviceLinks().getByUuid(first.getUuid()));
                assertNotNull(item);
                assertEquals(first.getUuid(), item.getUuid());
            }
        }
    }

    /** VPN configuration reads. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("VPN Read-Only Tests")
    class VPNTests {

        @Test
        @DisplayName("vpns_listAndGetByUuid - List VPNs and get by UUID (getVpns, getVpnByUuid)")
        void vpns_listAndGetByUuid() {
            PaginatedList<VPN> vpns = requireEntitled("NetworkEdge", "list", "VPN", "GET",
                    () -> networkEdge.vpns().list());
            assertNotNull(vpns);

            if (vpns.size() > 0) {
                VPN first = vpns.get(0);
                assertNotNull(first.getUuid());
                first.getConfigName();
                first.getPeerIp();

                VPN item = requireEntitled("NetworkEdge", "getByUuid", "VPN", "GET",
                        () -> networkEdge.vpns().getByUuid(first.getUuid()));
                assertNotNull(item);
                assertEquals(first.getUuid(), item.getUuid());
            }
        }
    }

    /** BGP peering reads. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("BGP Peering Read-Only Tests")
    class BGPTests {

        @Test
        @DisplayName("bgpPeerings_listAndGetByUuid - List BGP peerings and get by UUID (getBgpConfigurations, getBgpConfigurationByUuid)")
        void bgpPeerings_listAndGetByUuid() {
            PaginatedList<BGPPeering> peerings = requireEntitled("NetworkEdge", "list", "BGPPeering", "GET",
                    () -> networkEdge.bgpPeerings().list());
            assertNotNull(peerings);

            if (peerings.size() > 0) {
                BGPPeering first = peerings.get(0);
                assertNotNull(first.getUuid());
                first.getLocalIpAddress();
                first.getProvisioningStatus();

                BGPPeering item = requireEntitled("NetworkEdge", "getByUuid", "BGPPeering", "GET",
                        () -> networkEdge.bgpPeerings().getByUuid(first.getUuid()));
                assertNotNull(item);
                assertEquals(first.getUuid(), item.getUuid());
            }
        }
    }

    /** SSH public key reads. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Public Key Read-Only Tests")
    class PublicKeyTests {

        @Test
        @DisplayName("publicKeys_list - List SSH public keys (getPublicKeys)")
        void publicKeys_list() {
            List<PublicKey> keys = requireEntitled("NetworkEdge", "list", "PublicKey", "GET",
                    () -> networkEdge.publicKeys().list());
            assertNotNull(keys);
            if (!keys.isEmpty()) {
                PublicKey first = keys.get(0);
                assertNotNull(first.getUuid());
                first.getKeyName();
                first.getKeyType();
            }
        }
    }

    /** Device backup reads. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Backup Read-Only Tests")
    class BackupTests {

        @Test
        @DisplayName("backups_listAndGetByUuid - List backups of a device and get by UUID (getDeviceBackups, getDetailsOfBackupsByUuid)")
        void backups_listAndGetByUuid() {
            PaginatedList<Backup> backups = backupsForFirstDevice();
            assertNotNull(backups);

            if (backups.size() > 0) {
                Backup first = backups.get(0);
                assertNotNull(first.getUuid());
                first.getName();
                first.getType();
                first.getStatus();

                Backup item = requireEntitled("NetworkEdge", "getByUuid", "Backup", "GET",
                        () -> networkEdge.backups().getByUuid(first.getUuid()));
                assertNotNull(item);
                assertEquals(first.getUuid(), item.getUuid());
            }
        }

        @Test
        @DisplayName("backup_download - Download the contents of a completed backup (downloadDeviceBackupByUuid)")
        void backup_download() {
            Backup backup = requireCompletedBackup();
            String contents = requireEntitled("NetworkEdge", "download", "Backup", "GET",
                    () -> networkEdge.backups().download(backup.getUuid()));
            assertNotNull(contents);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  DRYRUN TESTS - Documented validation-only calls; no real mutations
    // ════════════════════════════════════════════════════════════════════

    /**
     * Validation-only operations. The restore feasibility analysis is a dedicated
     * check endpoint ({@code GET /ne/v1/devices/{uuid}/restoreAnalysis}); the actual
     * restore is a separate PATCH that is never called here.
     */
    @Nested
    @Tag("integration-dryrun")
    @DisplayName("Network Edge Dry-Run Tests")
    class DryRunTests {

        @Test
        @DisplayName("restoreAnalysis_check - Feasibility of restoring a backup (checkRestoreAnalysisOfBackupByUuid)")
        void restoreAnalysis_check() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");

            Device device = requireDevice();
            Backup backup = requireCompletedBackup();
            String deviceUuid = backup.getDeviceUuid() != null ? backup.getDeviceUuid() : device.getUuid();

            RestoreFeasibility feasibility = requireEntitled(
                    "NetworkEdge", "checkRestoreFeasibility", "RestoreFeasibility", "GET",
                    () -> networkEdge.backups().checkRestoreFeasibility(backup.getUuid(), deviceUuid));
            assertNotNull(feasibility);
            feasibility.getRestoreAllowedAfterDeleteOrEdit();
            feasibility.getServices();
        }
    }
}
