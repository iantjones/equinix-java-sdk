package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.enums.ConnectionType;
import api.equinix.javasdk.internetaccess.enums.ExportPolicy;
import api.equinix.javasdk.internetaccess.enums.ServiceState;
import api.equinix.javasdk.internetaccess.enums.ServiceTypeV2;
import api.equinix.javasdk.internetaccess.enums.TermsProduct;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.AccountAgreement;
import api.equinix.javasdk.internetaccess.model.AccountDetails;
import api.equinix.javasdk.internetaccess.model.Cabinet;
import api.equinix.javasdk.internetaccess.model.Cage;
import api.equinix.javasdk.internetaccess.model.ConnectionService;
import api.equinix.javasdk.internetaccess.model.CustomerRouteConfiguration;
import api.equinix.javasdk.internetaccess.model.DedicatedBandwidthConfiguration;
import api.equinix.javasdk.internetaccess.model.DedicatedPortDefaultConfiguration;
import api.equinix.javasdk.internetaccess.model.Ibx;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.OperationalUnit;
import api.equinix.javasdk.internetaccess.model.OrderDetails;
import api.equinix.javasdk.internetaccess.model.PatchPanel;
import api.equinix.javasdk.internetaccess.model.PortConfiguration;
import api.equinix.javasdk.internetaccess.model.Price;
import api.equinix.javasdk.internetaccess.model.PurchaseOrder;
import api.equinix.javasdk.internetaccess.model.RoutingProtocolConfiguration;
import api.equinix.javasdk.internetaccess.model.SignaturePolicy;
import api.equinix.javasdk.internetaccess.model.TermsAndConditions;
import api.equinix.javasdk.internetaccess.model.VirtualBandwidthConfiguration;
import api.equinix.javasdk.internetaccess.model.VirtualConnectionDefaultConfiguration;
import api.equinix.javasdk.internetaccess.model.json.creators.BgpRoutingProtocolRequest;
import api.equinix.javasdk.internetaccess.model.json.creators.ChangeOperationUpdate;
import api.equinix.javasdk.internetaccess.model.json.creators.PriceSearchRequest;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceSearchRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for the InternetAccess (EIA) domain of the Equinix Java SDK.
 *
 * <p>These tests exercise the live Equinix Internet Access API and verify that reality matches
 * the SDK's spec-derived expectations. Coverage spans the full safe-operation inventory of
 * {@code internetaccessv1.yaml} and {@code internetaccessv2.yaml}: the v2 service search/get and
 * IBX availability lookups, and the v1 read surface (accounts, agreements, terms, operational
 * units, signature policies, price search, product/attribute configurations, purchase orders,
 * cages/cabinets/patch panels/connection services, and the single order get).</p>
 *
 * <p>Three tiers of tests are provided:
 * <ul>
 *     <li><b>integration-readonly</b> - Safe read-only operations (list, get, search-POST).
 *         Calls go through {@code requireEntitled}: a 401/403 skips (credential not entitled),
 *         anything else fails.</li>
 *     <li><b>integration-dryrun</b> - the v2 {@code dryRun=true} validation surface (per the
 *         spec, "Setting this parameter to true will perform only request validation without
 *         actually updating/deleting the service"). Still zero real mutations, and the live
 *         update payload is a no-op by construction; the delete dry-run is deliberately
 *         WireMock-proofed only.</li>
 *     <li><b>integration-full</b> - Full CRUD lifecycle with automatic cleanup (folded in from
 *         the legacy {@code InternetAccessTest} live suite).</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -DaccessKey=ID -DsecretKey=SECRET
 * mvn test -Pintegration-dryrun   -DaccessKey=ID -DsecretKey=SECRET -DtestMode=dryrun
 * mvn test -Pintegration-full     -DaccessKey=ID -DsecretKey=SECRET -DtestMode=full -DconfirmDestructive=true \
 *          -DinternetAccessConnectionUuid=UUID
 * </pre>
 */
@Tag("integration-readonly")
@DisplayName("Internet Access Integration Tests")
class InternetAccessIntegrationTest extends IntegrationTestBase {

    static InternetAccess client;

    /** Lazily discovered IBX code where EIA is available; shared by IBX-scoped tests. */
    static String ibxCode;

    /** Lazily discovered billing account number at {@link #ibxCode}; shared by account-scoped tests. */
    static String accountNumber;

    @BeforeAll
    static void setUp() {
        client = new InternetAccess(testCredentials());
    }

    // ── Live-Discovery Helpers ─────────────────────────────────────────

    /**
     * Discovers (and caches) an IBX code where EIA is available, preferring virtual-connection
     * availability and falling back to physical. Skips the calling test when the API returns no
     * EIA-enabled IBXs.
     */
    static String discoverIbx() {
        if (ibxCode == null) {
            PaginatedList<Ibx> ibxs = requireEntitled("InternetAccess", "getIbxs", "Ibx", "GET",
                    () -> client.ibxs().availability(ConnectionType.IA_VC));
            if (ibxs == null || ibxs.isEmpty()) {
                ibxs = requireEntitled("InternetAccess", "getIbxs", "Ibx", "GET",
                        () -> client.ibxs().availability(ConnectionType.IA_C));
            }
            Assumptions.assumeTrue(ibxs != null && !ibxs.isEmpty(),
                    "No EIA-enabled IBXs returned; skipping IBX-dependent test");
            ibxCode = ibxs.get(0).getIbxCode();
        }
        return ibxCode;
    }

    /**
     * Discovers (and caches) a billing account number available for EIA ordering at the
     * discovered IBX. Skips the calling test when the account carries none.
     */
    static String discoverAccountNumber() {
        if (accountNumber == null) {
            String ibx = discoverIbx();
            PaginatedList<AccountDetails> accounts = requireEntitled("InternetAccess", "listAccounts",
                    "AccountDetails", "GET", () -> client.accounts().list(ibx));
            Assumptions.assumeTrue(accounts != null && !accounts.isEmpty(),
                    "No EIA billing accounts at IBX " + ibx + "; skipping account-dependent test");
            accountNumber = accounts.get(0).getAccountNumber();
        }
        return accountNumber;
    }

    /**
     * Searches EIA v2 services with a match-all filter ({@code /type} in {@code SINGLE, DUAL});
     * used both as coverage of the v2 search and as discovery for item-get tests.
     */
    static PaginatedFilteredList<InternetAccessService> searchServices() {
        return requireEntitled("InternetAccess", "getEquinixInternetAccessServices",
                "InternetAccessService", "POST",
                () -> client.services().search(new ServiceSearchRequest().equals("/type", "SINGLE", "DUAL")));
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS - Safe GET/list/search operations
    // ════════════════════════════════════════════════════════════════════

    /**
     * Read-only coverage of the EIA product-availability IBX lookups:
     * v2 {@code getIbxs} and v1 {@code getIbx}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("IBX Availability Read-Only Tests")
    class IbxTests {

        @Test
        @DisplayName("ibxs_availability - List IBXs where EIA is available (v2 getIbxs)")
        void ibxs_availability() {
            PaginatedList<Ibx> ibxs = requireEntitled("InternetAccess", "getIbxs", "Ibx", "GET",
                    () -> client.ibxs().availability(ConnectionType.IA_VC));
            assertNotNull(ibxs);
            if (!ibxs.isEmpty()) {
                Ibx first = ibxs.get(0);
                assertNotNull(first.getIbxCode());
                assertDoesNotThrow(first::getMetroCode);
                assertDoesNotThrow(first::getRegion);
            }
        }

        @Test
        @DisplayName("ibxs_getByCode - Get single IBX detail (v1 getIbx)")
        void ibxs_getByCode() {
            String ibx = discoverIbx();
            Ibx detail = requireEntitled("InternetAccess", "getIbx", "Ibx", "GET",
                    () -> client.ibxs().getByCode(ibx));
            assertNotNull(detail);
            assertEquals(ibx, detail.getIbxCode());
            assertDoesNotThrow(detail::getCountryCode);
        }
    }

    /**
     * Read-only coverage of the v1 account lookups: {@code listAccounts}, {@code singleAccount}
     * and {@code accountAgreements}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Account Read-Only Tests")
    class AccountTests {

        @Test
        @DisplayName("accounts_list - List accounts available for EIA ordering (listAccounts)")
        void accounts_list() {
            String ibx = discoverIbx();
            PaginatedList<AccountDetails> accounts = requireEntitled("InternetAccess", "listAccounts",
                    "AccountDetails", "GET", () -> client.accounts().list(ibx));
            assertNotNull(accounts);
            if (!accounts.isEmpty()) {
                AccountDetails first = accounts.get(0);
                assertNotNull(first.getAccountNumber());
                assertDoesNotThrow(first::getAccountName);
                assertDoesNotThrow(first::getOperationalUnits);
            }
        }

        @Test
        @DisplayName("accounts_getByNumber - Get single account (singleAccount)")
        void accounts_getByNumber() {
            String number = discoverAccountNumber();
            AccountDetails account = requireEntitled("InternetAccess", "singleAccount",
                    "AccountDetails", "GET", () -> client.accounts().getByNumber(number));
            assertNotNull(account);
            assertEquals(number, account.getAccountNumber());
            assertDoesNotThrow(account::getBilling);
        }

        @Test
        @DisplayName("accounts_agreements - List account agreements (accountAgreements)")
        void accounts_agreements() {
            String number = discoverAccountNumber();
            String ibx = discoverIbx();
            PaginatedList<AccountAgreement> agreements = requireEntitled("InternetAccess", "accountAgreements",
                    "AccountAgreement", "GET", () -> client.accounts().agreements(number, ibx));
            assertNotNull(agreements);
            if (!agreements.isEmpty()) {
                AccountAgreement first = agreements.get(0);
                assertDoesNotThrow(first::getType);
                assertDoesNotThrow(first::getValid);
            }
        }
    }

    /**
     * Read-only coverage of the v1 ordering reference data: {@code getTermsAndConditions},
     * {@code getOperationalUnits}, {@code getSignaturePolicies} and the {@code searchPrices}
     * stateless price lookup.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Ordering Reference Data Read-Only Tests")
    class OrderingReferenceTests {

        @Test
        @DisplayName("terms_list - Get terms and conditions (getTermsAndConditions)")
        void terms_list() {
            String number = discoverAccountNumber();
            String ibx = discoverIbx();
            PaginatedList<TermsAndConditions> terms = requireEntitled("InternetAccess", "getTermsAndConditions",
                    "TermsAndConditions", "GET",
                    () -> client.termsAndConditions().list(number, ibx, TermsProduct.IA_VC));
            assertNotNull(terms);
            if (!terms.isEmpty()) {
                TermsAndConditions first = terms.get(0);
                assertNotNull(first.getText());
                assertDoesNotThrow(first::getVersion);
                assertDoesNotThrow(first::getType);
            }
        }

        @Test
        @DisplayName("operationalUnits_list - List operational units (getOperationalUnits)")
        void operationalUnits_list() {
            String ibx = discoverIbx();
            PaginatedList<OperationalUnit> units = requireEntitled("InternetAccess", "getOperationalUnits",
                    "OperationalUnit", "GET", () -> client.operationalUnits().list(ibx));
            assertNotNull(units);
            if (!units.isEmpty()) {
                OperationalUnit first = units.get(0);
                assertNotNull(first.getName());
                assertDoesNotThrow(first::getAddress);
            }
        }

        @Test
        @DisplayName("signaturePolicies_list - List signature policies (getSignaturePolicies)")
        void signaturePolicies_list() {
            PaginatedList<SignaturePolicy> policies = requireEntitled("InternetAccess", "getSignaturePolicies",
                    "SignaturePolicy", "GET", () -> client.signaturePolicies().list());
            assertNotNull(policies);
            if (!policies.isEmpty()) {
                SignaturePolicy first = policies.get(0);
                assertDoesNotThrow(first::getSignature);
                assertDoesNotThrow(first::getClickThroughAllowed);
            }
        }

        @Test
        @DisplayName("prices_search - Search EIA prices (searchPrices)")
        void prices_search() {
            String number = discoverAccountNumber();
            PaginatedFilteredList<Price> prices = requireEntitled("InternetAccess", "searchPrices",
                    "Price", "POST",
                    () -> client.prices().search(new PriceSearchRequest().equals("/account/accountNumber", number)));
            assertNotNull(prices);
            if (!prices.isEmpty()) {
                Price first = prices.get(0);
                assertNotNull(first.getType());
                assertDoesNotThrow(first::getCurrency);
                assertDoesNotThrow(first::getCharges);
            }
        }
    }

    /**
     * Read-only coverage of the seven v1 product / attribute configuration lookups:
     * {@code getRoutingConfigurations}, {@code getDedicatedBandwidthConfigurations},
     * {@code getVirtualBandwidthConfigurations}, {@code getVirtualConnectionDefaultConfigurations},
     * {@code getCustomerRouteConfigurations}, {@code getDedicatedPortDefaultConfigurationsPage}
     * and {@code getPortConfigurations}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Product Configuration Read-Only Tests")
    class ProductConfigurationTests {

        @Test
        @DisplayName("productConfigurations_routing - Get routing protocol configurations (getRoutingConfigurations)")
        void productConfigurations_routing() {
            PaginatedList<RoutingProtocolConfiguration> configurations = requireEntitled("InternetAccess",
                    "getRoutingConfigurations", "RoutingProtocolConfiguration", "GET",
                    () -> client.productConfigurations().routingConfigurations(UseCase.MAIN));
            assertNotNull(configurations);
            if (!configurations.isEmpty()) {
                RoutingProtocolConfiguration first = configurations.get(0);
                assertNotNull(first.getUseCase());
                assertDoesNotThrow(first::getRoutingProtocol);
            }
        }

        @Test
        @DisplayName("productConfigurations_dedicatedBandwidth - Get dedicated bandwidth configurations (getDedicatedBandwidthConfigurations)")
        void productConfigurations_dedicatedBandwidth() {
            PaginatedList<DedicatedBandwidthConfiguration> configurations = requireEntitled("InternetAccess",
                    "getDedicatedBandwidthConfigurations", "DedicatedBandwidthConfiguration", "GET",
                    () -> client.productConfigurations().dedicatedBandwidthConfigurations(UseCase.MAIN));
            assertNotNull(configurations);
            if (!configurations.isEmpty()) {
                DedicatedBandwidthConfiguration first = configurations.get(0);
                assertNotNull(first.getUseCase());
                assertDoesNotThrow(first::getBandwidth);
                assertDoesNotThrow(first::getBilling);
            }
        }

        @Test
        @DisplayName("productConfigurations_virtualBandwidth - Get virtual bandwidth configurations (getVirtualBandwidthConfigurations)")
        void productConfigurations_virtualBandwidth() {
            PaginatedList<VirtualBandwidthConfiguration> configurations = requireEntitled("InternetAccess",
                    "getVirtualBandwidthConfigurations", "VirtualBandwidthConfiguration", "GET",
                    () -> client.productConfigurations().virtualBandwidthConfigurations(UseCase.MAIN));
            assertNotNull(configurations);
            if (!configurations.isEmpty()) {
                VirtualBandwidthConfiguration first = configurations.get(0);
                assertNotNull(first.getUseCase());
                assertDoesNotThrow(first::getBandwidth);
            }
        }

        @Test
        @DisplayName("productConfigurations_virtualConnectionDefaults - Get virtual connection default configurations (getVirtualConnectionDefaultConfigurations)")
        void productConfigurations_virtualConnectionDefaults() {
            String ibx = discoverIbx();
            PaginatedList<VirtualConnectionDefaultConfiguration> configurations = requireEntitled("InternetAccess",
                    "getVirtualConnectionDefaultConfigurations", "VirtualConnectionDefaultConfiguration", "GET",
                    () -> client.productConfigurations().virtualConnectionDefaultConfigurations(ibx));
            assertNotNull(configurations);
            if (!configurations.isEmpty()) {
                VirtualConnectionDefaultConfiguration first = configurations.get(0);
                assertDoesNotThrow(first::getUseCase);
                assertDoesNotThrow(first::getConnection);
            }
        }

        @Test
        @DisplayName("productConfigurations_customerRoute - Get customer route configurations (getCustomerRouteConfigurations)")
        void productConfigurations_customerRoute() {
            PaginatedList<CustomerRouteConfiguration> configurations = requireEntitled("InternetAccess",
                    "getCustomerRouteConfigurations", "CustomerRouteConfiguration", "GET",
                    () -> client.productConfigurations().customerRouteConfigurations(UseCase.MAIN));
            assertNotNull(configurations);
            if (!configurations.isEmpty()) {
                CustomerRouteConfiguration first = configurations.get(0);
                assertNotNull(first.getUseCase());
                assertDoesNotThrow(first::getRoutingProtocol);
            }
        }

        @Test
        @DisplayName("productConfigurations_dedicatedPortDefaults - Get dedicated port default configurations (getDedicatedPortDefaultConfigurationsPage)")
        void productConfigurations_dedicatedPortDefaults() {
            String ibx = discoverIbx();
            PaginatedList<DedicatedPortDefaultConfiguration> configurations = requireEntitled("InternetAccess",
                    "getDedicatedPortDefaultConfigurationsPage", "DedicatedPortDefaultConfiguration", "GET",
                    () -> client.productConfigurations().dedicatedPortDefaultConfigurations(ibx));
            assertNotNull(configurations);
            if (!configurations.isEmpty()) {
                DedicatedPortDefaultConfiguration first = configurations.get(0);
                assertDoesNotThrow(first::getUseCase);
                assertDoesNotThrow(first::getConnection);
            }
        }

        @Test
        @DisplayName("productConfigurations_port - Get port configurations (getPortConfigurations)")
        void productConfigurations_port() {
            String ibx = discoverIbx();
            PaginatedList<PortConfiguration> configurations = requireEntitled("InternetAccess",
                    "getPortConfigurations", "PortConfiguration", "GET",
                    () -> client.productConfigurations().portConfigurations(ibx, UseCase.MAIN));
            assertNotNull(configurations);
            if (!configurations.isEmpty()) {
                PortConfiguration first = configurations.get(0);
                assertNotNull(first.getUseCase());
                assertDoesNotThrow(first::getConnection);
            }
        }
    }

    /**
     * Read-only coverage of the v1 purchase-order lookups: {@code getPurchaseOrders} and
     * {@code getPurchaseOrder}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Purchase Order Read-Only Tests")
    class PurchaseOrderTests {

        @Test
        @DisplayName("purchaseOrders_list - List purchase orders (getPurchaseOrders)")
        void purchaseOrders_list() {
            String number = discoverAccountNumber();
            PaginatedList<PurchaseOrder> purchaseOrders = requireEntitled("InternetAccess", "getPurchaseOrders",
                    "PurchaseOrder", "GET", () -> client.purchaseOrders().list(number));
            assertNotNull(purchaseOrders);
            if (!purchaseOrders.isEmpty()) {
                PurchaseOrder first = purchaseOrders.get(0);
                assertNotNull(first.getNumber());
                assertDoesNotThrow(first::getType);
                assertDoesNotThrow(first::getStatus);
            }
        }

        @Test
        @DisplayName("purchaseOrders_get - Get single purchase order (getPurchaseOrder)")
        void purchaseOrders_get() {
            String account = discoverAccountNumber();
            PaginatedList<PurchaseOrder> purchaseOrders = requireEntitled("InternetAccess", "getPurchaseOrders",
                    "PurchaseOrder", "GET", () -> client.purchaseOrders().list(account));
            Assumptions.assumeTrue(purchaseOrders != null && !purchaseOrders.isEmpty(),
                    "No purchase orders on account " + account + "; skipping get test");
            String number = purchaseOrders.get(0).getNumber();
            Assumptions.assumeTrue(number != null, "Purchase order without a number; skipping get test");

            PurchaseOrder purchaseOrder = requireEntitled("InternetAccess", "getPurchaseOrder",
                    "PurchaseOrder", "GET", () -> client.purchaseOrders().get(account, number));
            assertNotNull(purchaseOrder);
            assertEquals(number, purchaseOrder.getNumber());
        }
    }

    /**
     * Read-only coverage of the v1 product-availability inventory: {@code getCages},
     * {@code getCabinets}, {@code getPatchPanels} and {@code getConnectionServices}.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Inventory Read-Only Tests")
    class InventoryTests {

        @Test
        @DisplayName("cages_list - List cages available for EIA (getCages)")
        void cages_list() {
            String ibx = discoverIbx();
            String number = discoverAccountNumber();
            PaginatedList<Cage> cages = requireEntitled("InternetAccess", "getCages", "Cage", "GET",
                    () -> client.cages().list(ibx, number));
            assertNotNull(cages);
            if (!cages.isEmpty()) {
                Cage first = cages.get(0);
                assertNotNull(first.getSpaceId());
                assertDoesNotThrow(first::getCabinetsCount);
            }
        }

        @Test
        @DisplayName("cabinets_list - List cabinets available for EIA (getCabinets)")
        void cabinets_list() {
            PaginatedList<Cabinet> cabinets = requireEntitled("InternetAccess", "getCabinets", "Cabinet", "GET",
                    () -> client.cabinets().list());
            assertNotNull(cabinets);
            if (!cabinets.isEmpty()) {
                Cabinet first = cabinets.get(0);
                assertNotNull(first.getSpaceId());
                assertDoesNotThrow(first::getCage);
                assertDoesNotThrow(first::getLocation);
            }
        }

        @Test
        @DisplayName("patchPanels_list - List patch panels available for EIA (getPatchPanels)")
        void patchPanels_list() {
            PaginatedList<Cabinet> cabinets = requireEntitled("InternetAccess", "getCabinets", "Cabinet", "GET",
                    () -> client.cabinets().list());
            Assumptions.assumeTrue(cabinets != null && !cabinets.isEmpty(),
                    "No cabinets available for EIA; skipping patch panel test");

            Cabinet target = null;
            for (Cabinet candidate : cabinets) {
                if (candidate.getSpaceId() != null
                        && candidate.getCage() != null && candidate.getCage().getSpaceId() != null
                        && candidate.getLocation() != null && candidate.getLocation().getIbx() != null
                        && candidate.getAccount() != null && candidate.getAccount().getAccountNumber() != null) {
                    target = candidate;
                    break;
                }
            }
            Assumptions.assumeTrue(target != null,
                    "No cabinet with cage/location/account context; skipping patch panel test");
            final Cabinet cabinet = target;

            PaginatedList<PatchPanel> patchPanels = requireEntitled("InternetAccess", "getPatchPanels",
                    "PatchPanel", "GET",
                    () -> client.patchPanels().list(cabinet.getLocation().getIbx(),
                            cabinet.getAccount().getAccountNumber(),
                            cabinet.getCage().getSpaceId(), cabinet.getSpaceId()));
            assertNotNull(patchPanels);
            if (!patchPanels.isEmpty()) {
                PatchPanel first = patchPanels.get(0);
                assertNotNull(first.getNumber());
                assertDoesNotThrow(first::getType);
                assertDoesNotThrow(first::getMediaTypes);
            }
        }

        @Test
        @DisplayName("connectionServices_list - List connection services available for EIA (getConnectionServices)")
        void connectionServices_list() {
            String ibx = discoverIbx();
            PaginatedList<ConnectionService> services = requireEntitled("InternetAccess", "getConnectionServices",
                    "ConnectionService", "GET", () -> client.connectionServices().list(ibx));
            assertNotNull(services);
            if (!services.isEmpty()) {
                ConnectionService first = services.get(0);
                assertNotNull(first.getName());
                assertDoesNotThrow(first::getMediaTypes);
            }
        }
    }

    /**
     * Read-only coverage of the v2 service surface ({@code getEquinixInternetAccessServices},
     * {@code getEquinixInternetAccessServiceDetails}) and the v1 order single get
     * ({@code getOrder}), whose order UUID is discovered from a live service.
     */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Service and Order Read-Only Tests")
    class ServiceAndOrderTests {

        @Test
        @DisplayName("services_search - Search EIA v2 services (getEquinixInternetAccessServices)")
        void services_search() {
            PaginatedFilteredList<InternetAccessService> services = searchServices();
            assertNotNull(services);
            if (!services.isEmpty()) {
                InternetAccessService first = services.get(0);
                assertNotNull(first.getUuid());
                assertDoesNotThrow(first::getState);
                assertDoesNotThrow(first::getType);
            }
        }

        @Test
        @DisplayName("services_getByUuid - Get EIA v2 service details (getEquinixInternetAccessServiceDetails)")
        void services_getByUuid() {
            PaginatedFilteredList<InternetAccessService> services = searchServices();
            Assumptions.assumeTrue(services != null && !services.isEmpty(),
                    "No EIA services found; skipping service get test");

            String uuid = services.get(0).getUuid();
            InternetAccessService service = requireEntitled("InternetAccess",
                    "getEquinixInternetAccessServiceDetails", "InternetAccessService", "GET",
                    () -> client.services().getByUuid(uuid));
            assertNotNull(service);
            assertEquals(uuid, service.getUuid());
            assertDoesNotThrow(service::getConnections);
            assertDoesNotThrow(service::getRoutingProtocol);
        }

        @Test
        @DisplayName("orders_get - Get order details (getOrder)")
        void orders_get() {
            PaginatedFilteredList<InternetAccessService> services = searchServices();
            Assumptions.assumeTrue(services != null && !services.isEmpty(),
                    "No EIA services found; skipping order get test");

            String orderUuid = null;
            for (InternetAccessService service : services) {
                if (service.getOrder() != null && service.getOrder().getUuid() != null) {
                    orderUuid = service.getOrder().getUuid();
                    break;
                }
            }
            Assumptions.assumeTrue(orderUuid != null,
                    "No EIA service carries an order UUID; skipping order get test");
            final String uuid = orderUuid;

            OrderDetails order = requireEntitled("InternetAccess", "getOrder", "OrderDetails", "GET",
                    () -> client.orders().get(uuid));
            assertNotNull(order);
            assertEquals(uuid, order.getUuid());
            assertDoesNotThrow(order::getType);
            assertDoesNotThrow(order::getStatus);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  DRYRUN TESTS - v2 dryRun=true validation calls (zero real mutations)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Dry-run coverage of the EIA v2 {@code dryRun} query parameter. Per the
     * {@code updateEquinixInternetAccess} / {@code deleteEquinixInternetAccess} specs it is an
     * optional boolean with no default: "Setting this parameter to true will perform only
     * request validation without actually updating/deleting the service" (the SDK omits the
     * parameter entirely when {@code false}).
     */
    @Nested
    @Tag("integration-dryrun")
    @DisplayName("EIA v2 Dry-Run Tests")
    class DryRunTests {

        // PARAM-DROP SAFETY DOCTRINE: a live dry-run must be harmless even if a future
        // regression silently dropped the dryRun parameter and the call executed FOR REAL.
        //
        // DELETE /internetAccess/v2/services/{serviceId}?dryRun=true is therefore deliberately
        // NOT live-tested: its payload is nothing but the serviceId of a live customer
        // service, so a dropped parameter would REALLY delete that service — catastrophic and
        // not reliably recoverable. The InternetAccessServiceLifecycleWireMockTest wire-proof
        // (asserts dryRun=true actually goes on the wire, and covers the body-less v2 202) is
        // the lock for that surface; no live delete dry-run is ever made here.

        @Test
        @DisplayName("services_dryRunUpdate - Validate-only NO-OP bandwidth update (updateEquinixInternetAccess?dryRun=true)")
        void services_dryRunUpdate() {
            Assumptions.assumeTrue(isDryRunEnabled(), "Dry-run tests disabled in READONLY mode");

            PaginatedFilteredList<InternetAccessService> services = searchServices();
            Assumptions.assumeTrue(services != null && !services.isEmpty(),
                    "No EIA services found; skipping dry-run update");

            InternetAccessService target = null;
            for (InternetAccessService candidate : services) {
                if (candidate.getUuid() != null && candidate.getBandwidth() != null
                        && candidate.getState() == ServiceState.ACTIVE) {
                    target = candidate;
                    break;
                }
            }
            Assumptions.assumeTrue(target != null,
                    "No ACTIVE EIA service with a bandwidth found; skipping dry-run update");
            final InternetAccessService service = target;

            // PARAM-DROP SAFETY: the single change operation replaces /bandwidth with the
            // service's CURRENT bandwidth, so even if a regression dropped dryRun=true and
            // the PATCH executed for real, the service would be "updated" to exactly what it
            // already is.
            InternetAccessService dryRunResult = requireEntitled("InternetAccess",
                    "updateEquinixInternetAccess_dryRun", "InternetAccessService", "PATCH",
                    () -> client.services().update(service.getUuid(),
                            List.of(ChangeOperationUpdate.replace("/bandwidth",
                                    String.valueOf(service.getBandwidth()))),
                            true));

            assertNotNull(dryRunResult, "Dry-run update should return the validated service");
            if (dryRunResult.getUuid() != null) {
                assertEquals(service.getUuid(), dryRunResult.getUuid(),
                        "Validated service should be the one the no-op update targeted");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  FULL CRUD TESTS - Create, read, delete lifecycle with cleanup
    //  (folded in from the legacy InternetAccessTest live suite)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Full CRUD coverage of the v2 service lifecycle, folded in from the legacy
     * {@code InternetAccessTest} live suite. Requires an existing EIA-eligible connection UUID
     * supplied via {@code -DinternetAccessConnectionUuid}.
     */
    @Nested
    @Tag("integration-full")
    @DisplayName("Full CRUD Tests")
    class FullCrudTests {

        @Test
        @DisplayName("services_createBgp_lifecycle - Create BGP service, get, cleanup delete")
        void services_createBgp_lifecycle() {
            Assumptions.assumeTrue(isFullCrudEnabled(),
                    "Full CRUD tests disabled; requires -DtestMode=full -DconfirmDestructive=true");
            String connectionUuid = System.getProperty("internetAccessConnectionUuid");
            Assumptions.assumeTrue(connectionUuid != null && !connectionUuid.isBlank(),
                    "No -DinternetAccessConnectionUuid provided; skipping service create test");

            // ── CREATE ──────────────────────────────────────────────
            InternetAccessService created = timedCall("InternetAccess", "create", "InternetAccessService", "POST",
                    () -> client.services().define()
                            .name(testResourceName("eia"))
                            .type(ServiceTypeV2.SINGLE)
                            .connection(connectionUuid)
                            .routingProtocol(BgpRoutingProtocolRequest.builder()
                                    .customerAsn(16220L)
                                    .exportPolicy(ExportPolicy.FULL)
                                    .build())
                            .create());
            assertNotNull(created, "Created service should not be null");
            assertNotNull(created.getUuid(), "Created service UUID should not be null");
            String createdUuid = created.getUuid();

            // Register cleanup immediately so the service is deleted even if later assertions fail
            registerCleanup("InternetAccessService", createdUuid, id -> client.services().delete(id));

            // ── GET ─────────────────────────────────────────────────
            InternetAccessService fetched = timedCall("InternetAccess", "getByUuid", "InternetAccessService",
                    "GET", createdUuid, () -> client.services().getByUuid(createdUuid));
            assertNotNull(fetched);
            assertEquals(createdUuid, fetched.getUuid());
        }
    }
}
