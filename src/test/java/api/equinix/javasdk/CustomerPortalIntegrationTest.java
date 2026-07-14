package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.exception.EquinixNotFoundException;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.enums.LoaState;
import api.equinix.javasdk.customerportal.enums.ReportStatus;
import api.equinix.javasdk.customerportal.model.*;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.implementation.Account;
import api.equinix.javasdk.customerportal.model.implementation.BillingInvoice;
import api.equinix.javasdk.customerportal.model.implementation.BillingInvoiceDocument;
import api.equinix.javasdk.customerportal.model.implementation.CageDetails;
import api.equinix.javasdk.customerportal.model.implementation.SupportCaseAttachmentInfo;
import api.equinix.javasdk.customerportal.model.implementation.SupportCaseEmail;
import api.equinix.javasdk.customerportal.model.json.ReportDefinitionJson;
import api.equinix.javasdk.customerportal.model.json.creators.AssetSearchFilter;
import api.equinix.javasdk.customerportal.model.json.creators.AssetSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.BillingAccountSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.NotificationSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.OrderHistorySearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.UnifiedNotificationSearchRequest;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration tests for the Customer Portal domain of the Equinix Java SDK.
 *
 * <p>These tests exercise the real Customer Portal APIs to prove the SDK's spec-derived
 * models match reality. Coverage is catalog-complete over the verified safe-op inventory:
 * every read (GET / list / search-POST) across ordersv2, orderhistoryv1, quotesv2, basv2,
 * securecabinetv1, smarthandsv1, supportv2, ticketsv2, troubleticketv1, workvisitv1,
 * shipmentsv1, billingv1, billingv2, assetsv1, attachmentsv1, reportsv1, notificationsv1,
 * unifiednotificationv2, diloav1, lookupv2 and supportplansv2.</p>
 *
 * <p>Every live call goes through {@code requireEntitled}, which skips ONLY when the
 * credential lacks the product (401/403) and fails on everything else (deserialization
 * crash, unmapped enum, 5xx, unexpected 404 on a collection URL). Item-GETs discover an
 * identifier from the corresponding list/search first and skip via {@link Assumptions}
 * when the account has none.</p>
 *
 * <p>The Customer Portal safe-op inventory contains no documented dry-run/validate
 * operations, so this suite has no {@code integration-dryrun} nest. Cross-connects expose
 * only irreversible order mutations (no safe reads), so they do not appear here; their
 * reference data is covered by the Lookups nest ({@code lookupv2}).</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -DaccessKey=ID -DsecretKey=SECRET
 * </pre>
 *
 * <h3>Optional identifier properties</h3>
 * <p>Three resources have item-GETs but no listing operation to discover an id from.
 * Supply a known identifier to exercise them; the tests skip otherwise:</p>
 * <ul>
 *     <li>{@code -DquoteId=...} — quotesv2 {@code Retrieve a quote}</li>
 *     <li>{@code -DtroubleTicketId=...} — ticketsv2 {@code Retrieve a ticket}</li>
 *     <li>{@code -DsupportCaseId=...} — supportv2 {@code getTicketDetails} (case or order
 *         number; also unlocks the email-details and attachment-download chains)</li>
 * </ul>
 */
@Tag("integration-readonly")
@DisplayName("Customer Portal Integration Tests")
class CustomerPortalIntegrationTest extends IntegrationTestBase {

    static CustomerPortal client;

    // ── Memoized discovery state (shared across nests; fetched at most once) ──
    private static List<? extends OrderHistoryItem> orderHistoryItems;
    private static PaginatedList<BillingAccount> billingAccountSummaries;
    private static PaginatedList<BillingAccountV2> billingAccountsV2;
    private static List<? extends LookupLocation> lookupLocations;
    private static PaginatedList<Attachment> attachmentList;
    private static PaginatedList<Report> reportList;
    private static List<? extends ScheduledReport> scheduledReportList;
    private static List<ReportDefinitionJson> reportDefinitionList;
    private static List<? extends Notification> ibxNotifications;
    private static List<? extends Notification> networkNotifications;
    private static List<? extends DigitalLoa> digitalLoaList;
    private static PaginatedList<Asset> assetList;
    private static SupportCase supportCase;

    @BeforeAll
    static void setUp() {
        client = new CustomerPortal(testCredentials());
    }

    // ── Discovery helpers ──────────────────────────────────────────────
    // Each helper runs its op through requireEntitled exactly once and caches the
    // result, so a search used by several item-GET tests is called (and reported)
    // a single time. An entitlement gap (401/403) aborts the calling test.

    static List<? extends OrderHistoryItem> orderHistoryItems() {
        if (orderHistoryItems == null) {
            orderHistoryItems = requireEntitled("CustomerPortal", "search", "OrderHistoryItem", "POST",
                    () -> client.orderHistory().search(OrderHistorySearchRequest.builder().build()));
        }
        return orderHistoryItems;
    }

    static PaginatedList<BillingAccount> billingAccountSummaries() {
        if (billingAccountSummaries == null) {
            billingAccountSummaries = requireEntitled("CustomerPortal", "summaries", "BillingAccount", "GET",
                    () -> client.billingAccounts().summaries());
        }
        return billingAccountSummaries;
    }

    static PaginatedList<BillingAccountV2> billingAccountsV2() {
        if (billingAccountsV2 == null) {
            billingAccountsV2 = requireEntitled("CustomerPortal", "search", "BillingAccountV2", "POST",
                    () -> client.billingAccountsSearch().search(BillingAccountSearchRequest.builder().build()));
        }
        return billingAccountsV2;
    }

    static List<? extends LookupLocation> lookupLocations() {
        if (lookupLocations == null) {
            lookupLocations = requireEntitled("CustomerPortal", "listLocations", "LookupLocation", "GET",
                    () -> client.lookups().listLocations("CROSS_CONNECT"));
        }
        return lookupLocations;
    }

    static PaginatedList<Attachment> attachmentList() {
        if (attachmentList == null) {
            attachmentList = requireEntitled("CustomerPortal", "list", "Attachment", "GET",
                    () -> client.attachments().list());
        }
        return attachmentList;
    }

    static PaginatedList<Report> reportList() {
        if (reportList == null) {
            reportList = requireEntitled("CustomerPortal", "getReports", "Report", "GET",
                    () -> client.reports().getReports());
        }
        return reportList;
    }

    static List<? extends ScheduledReport> scheduledReportList() {
        if (scheduledReportList == null) {
            scheduledReportList = requireEntitled("CustomerPortal", "getScheduledReports", "ScheduledReport", "GET",
                    () -> client.reports().getScheduledReports());
        }
        return scheduledReportList;
    }

    static List<ReportDefinitionJson> reportDefinitionList() {
        if (reportDefinitionList == null) {
            reportDefinitionList = requireEntitled("CustomerPortal", "getReportDefinitions", "ReportDefinition", "GET",
                    () -> client.reports().getReportDefinitions());
        }
        return reportDefinitionList;
    }

    static List<? extends Notification> ibxNotifications() {
        if (ibxNotifications == null) {
            ibxNotifications = requireEntitled("CustomerPortal", "searchIbx", "Notification", "POST",
                    () -> client.notifications().searchIbx(NotificationSearchRequest.builder().build()));
        }
        return ibxNotifications;
    }

    static List<? extends Notification> networkNotifications() {
        if (networkNotifications == null) {
            networkNotifications = requireEntitled("CustomerPortal", "searchNetwork", "Notification", "POST",
                    () -> client.notifications().searchNetwork(NotificationSearchRequest.builder().build()));
        }
        return networkNotifications;
    }

    static List<? extends DigitalLoa> digitalLoaList() {
        if (digitalLoaList == null) {
            digitalLoaList = requireEntitled("CustomerPortal", "search", "DigitalLoa", "POST",
                    () -> client.digitalLoas().search(loaSearchRequest()));
        }
        return digitalLoaList;
    }

    static PaginatedList<Asset> assetList() {
        if (assetList == null) {
            assetList = requireEntitled("CustomerPortal", "search", "Asset", "POST",
                    () -> client.assets().search(new AssetSearchRequest((AssetSearchFilter) null)));
        }
        return assetList;
    }

    /**
     * Fetches the support case named by {@code -DsupportCaseId} (case or order number).
     * The supportv2 spec has no case-listing operation, so without the property the
     * dependent tests skip.
     */
    static SupportCase supportCase() {
        String id = System.getProperty("supportCaseId");
        Assumptions.assumeTrue(id != null && !id.isBlank(),
                "SupportCase skipped: no -DsupportCaseId supplied (supportv2 has no case listing op to discover one)");
        if (supportCase == null) {
            supportCase = requireEntitled("CustomerPortal", "getByCaseOrOrderNumber", "SupportCase", "GET",
                    () -> client.supportCases().getByCaseOrOrderNumber(id));
        }
        return supportCase;
    }

    /**
     * Minimal valid diloav1 search body: the spec's {@code LoaSearch} requires a filter of
     * {@code LoaCriterion} (property / operator / values). Matching every {@link LoaState}
     * maximizes discovery for the item-GET chains.
     */
    static DigitalLoaSearchRequest loaSearchRequest() {
        Map<String, Object> criterion = new LinkedHashMap<>();
        criterion.put("property", "/state");
        criterion.put("operator", "=");
        criterion.put("values", Arrays.stream(LoaState.values()).map(Enum::name).toList());
        return new DigitalLoaSearchRequest(criterion);
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS - one nest per spec / resource area
    // ════════════════════════════════════════════════════════════════════

    /** orderhistoryv1: {@code POST /v1/retrieve-orders} + {@code GET /v1/retrieve-orders/locations}. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Order History (orderhistoryv1)")
    class OrderHistoryTests {

        @Test
        @DisplayName("orderHistory_search - Search order history (POST /v1/retrieve-orders)")
        void orderHistory_search() {
            List<? extends OrderHistoryItem> items = orderHistoryItems();
            assertNotNull(items);
            if (!items.isEmpty()) {
                OrderHistoryItem first = items.get(0);
                assertNotNull(first.getOrderNumber());
                first.getOrderStatus();
                first.getIbx();
                first.getSubmittedDate();
            }
        }

        @Test
        @DisplayName("orderHistory_listLocations - Order locations (GET /v1/retrieve-orders/locations)")
        void orderHistory_listLocations() {
            List<? extends PermissibleLocation> locations = requireEntitled("CustomerPortal",
                    "listLocations", "PermissibleLocation", "GET", () -> client.orderHistory().listLocations());
            assertNotNull(locations);
            if (!locations.isEmpty()) {
                locations.get(0).getIbx();
                locations.get(0).getCages();
            }
        }
    }

    /** ordersv2: {@code GET /colocations/v2/orders/{orderId}} (+ negotiations). */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Orders (ordersv2)")
    class OrdersTests {

        /**
         * Orders v2 has no list op; the order number is discovered via order-history search.
         * History spans every product family while ordersv2 serves colocation orders, so a
         * 404 on the discovered number is genuine data-dependence (non-colo order), not a
         * spec defect, and skips.
         */
        @Test
        @DisplayName("orders_getByUuid - Get order details (GET /colocations/v2/orders/{orderId})")
        void orders_getByUuid() {
            List<? extends OrderHistoryItem> history = orderHistoryItems();
            Assumptions.assumeTrue(!history.isEmpty(), "No order history found; skipping order get test");

            String orderId = history.get(0).getOrderNumber();
            try {
                Order order = requireEntitled("CustomerPortal", "getByUuid", "Order", "GET",
                        () -> client.orders().getByUuid(orderId));
                assertNotNull(order);
                order.getOrderId();
                order.getStatus();
                order.getAccountNumber();
            } catch (EquinixNotFoundException e) {
                Assumptions.abort("Order " + orderId + " from history is not a colocations v2 order: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("orders_getNegotiations - Order negotiations (GET /colocations/v2/orders/{orderId}/negotiations)")
        void orders_getNegotiations() {
            List<? extends OrderHistoryItem> history = orderHistoryItems();
            Assumptions.assumeTrue(!history.isEmpty(), "No order history found; skipping negotiations test");

            String orderId = history.get(0).getOrderNumber();
            try {
                List<? extends OrderNegotiation> negotiations = requireEntitled("CustomerPortal",
                        "getNegotiations", "OrderNegotiation", "GET",
                        () -> client.orders().getNegotiations(orderId));
                assertNotNull(negotiations);
                if (!negotiations.isEmpty()) {
                    negotiations.get(0).getMessage();
                    negotiations.get(0).getProposedDateTime();
                }
            } catch (EquinixNotFoundException e) {
                Assumptions.abort("Order " + orderId + " from history is not a colocations v2 order: " + e.getMessage());
            }
        }
    }

    /** quotesv2: {@code GET /v2/quotes/{quoteId}}. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Quotes (quotesv2)")
    class QuotesTests {

        /** Quotes v2 has no list op; supply {@code -DquoteId} to exercise the item-GET. */
        @Test
        @DisplayName("quotes_getByUuid - Retrieve a quote (GET /v2/quotes/{quoteId})")
        void quotes_getByUuid() {
            String quoteId = System.getProperty("quoteId");
            Assumptions.assumeTrue(quoteId != null && !quoteId.isBlank(),
                    "Quote skipped: no -DquoteId supplied (quotesv2 has no list op to discover one)");

            Quote quote = requireEntitled("CustomerPortal", "getByUuid", "Quote", "GET",
                    () -> client.quotes().getByUuid(quoteId));
            assertNotNull(quote);
            assertEquals(quoteId, quote.getQuoteId());
            quote.getStatus();
            quote.getCurrencyCode();
        }
    }

    /** billingv2: {@code GET /v2/invoices} + {@code GET /v2/invoices/details}. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Invoices (billingv2)")
    class InvoicesTests {

        @Test
        @DisplayName("invoices_summaries - Retrieve billing summary (GET /v2/invoices)")
        void invoices_summaries() {
            PaginatedList<InvoiceSummary> summaries = requireEntitled("CustomerPortal", "summaries",
                    "InvoiceSummary", "GET", () -> client.invoices().summaries());
            assertNotNull(summaries);
            if (!summaries.isEmpty()) {
                InvoiceSummary first = summaries.get(0);
                first.getTransactionId();
                first.getTransactionType();
                first.getCurrencyCode();
            }
        }

        @Test
        @DisplayName("invoices_details - Retrieve billing details (GET /v2/invoices/details)")
        void invoices_details() {
            PaginatedList<InvoiceDetail> details = requireEntitled("CustomerPortal", "details",
                    "InvoiceDetail", "GET", () -> client.invoices().details());
            assertNotNull(details);
            if (!details.isEmpty()) {
                InvoiceDetail first = details.get(0);
                first.getTransactionId();
                first.getProductName();
                first.getTotalAmount();
            }
        }
    }

    /** billingv1: finance account summaries, account detail and invoice-document download. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Billing Accounts (billingv1)")
    class BillingAccountsTests {

        @Test
        @DisplayName("billingAccounts_summaries - List billing accounts (GET /v1/finance/accounts)")
        void billingAccounts_summaries() {
            PaginatedList<BillingAccount> accounts = billingAccountSummaries();
            assertNotNull(accounts);
            if (!accounts.isEmpty()) {
                BillingAccount first = accounts.get(0);
                assertNotNull(first.getAccountNumber());
                first.getAccountName();
                first.getCurrencyCode();
            }
        }

        @Test
        @DisplayName("billingAccounts_getByAccountNumber - Account detail (GET /v1/finance/accounts/{accountNumber})")
        void billingAccounts_getByAccountNumber() {
            PaginatedList<BillingAccount> accounts = billingAccountSummaries();
            Assumptions.assumeTrue(!accounts.isEmpty(), "No billing accounts found; skipping account detail test");

            String accountNumber = accounts.get(0).getAccountNumber();
            BillingAccount account = requireEntitled("CustomerPortal", "getByAccountNumber", "BillingAccount", "GET",
                    () -> client.billingAccounts().getByAccountNumber(accountNumber));
            assertNotNull(account);
            assertEquals(accountNumber, account.getAccountNumber());
            account.getBillingFrequency();
            account.getInvoiceFormat();
        }

        @Test
        @DisplayName("billingAccounts_downloadInvoiceDocument - Document download (GET /v1/finance/accounts/{accountNumber}/{invoiceId})")
        void billingAccounts_downloadInvoiceDocument() {
            PaginatedList<BillingAccount> accounts = billingAccountSummaries();
            Assumptions.assumeTrue(!accounts.isEmpty(), "No billing accounts found; skipping document download test");

            String accountNumber = accounts.get(0).getAccountNumber();
            BillingAccount account = requireEntitled("CustomerPortal", "getByAccountNumber", "BillingAccount", "GET",
                    () -> client.billingAccounts().getByAccountNumber(accountNumber));

            String invoiceId = null;
            String documentId = null;
            if (account.getInvoices() != null) {
                for (BillingInvoice invoice : account.getInvoices()) {
                    if (invoice.getDocuments() != null && !invoice.getDocuments().isEmpty()) {
                        BillingInvoiceDocument document = invoice.getDocuments().get(0);
                        invoiceId = invoice.getInvoiceId();
                        documentId = document.getDocumentId();
                        break;
                    }
                }
            }
            Assumptions.assumeTrue(invoiceId != null && documentId != null,
                    "No invoice documents on account " + accountNumber + "; skipping document download test");

            String finalInvoiceId = invoiceId;
            String finalDocumentId = documentId;
            byte[] bytes = requireEntitled("CustomerPortal", "downloadInvoiceDocument", "BillingDocument", "GET",
                    () -> client.billingAccounts().downloadInvoiceDocument(accountNumber, finalInvoiceId, finalDocumentId));
            assertNotNull(bytes);
            assertTrue(bytes.length > 0, "Downloaded billing document should not be empty");
        }
    }

    /** basv2: billing-account search plus lookups by account number and account id. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Billing Accounts Search (basv2)")
    class BillingAccountsSearchTests {

        @Test
        @DisplayName("billingAccountsSearch_search - Search billing accounts (POST /billing/v2/billingAccounts/search)")
        void billingAccountsSearch_search() {
            PaginatedList<BillingAccountV2> accounts = billingAccountsV2();
            assertNotNull(accounts);
            if (!accounts.isEmpty()) {
                BillingAccountV2 first = accounts.get(0);
                assertNotNull(first.getAccountNumber());
                first.getAccountStatus();
                first.getCurrency();
            }
        }

        @Test
        @DisplayName("billingAccountsSearch_getByAccountNumber - GET /billing/v2/billingAccounts/accountNumber/{accountNumber}")
        void billingAccountsSearch_getByAccountNumber() {
            PaginatedList<BillingAccountV2> accounts = billingAccountsV2();
            Assumptions.assumeTrue(!accounts.isEmpty(), "No billing accounts found; skipping get-by-number test");

            String accountNumber = accounts.get(0).getAccountNumber();
            BillingAccountV2 account = requireEntitled("CustomerPortal", "getByAccountNumber", "BillingAccountV2", "GET",
                    () -> client.billingAccountsSearch().getByAccountNumber(accountNumber));
            assertNotNull(account);
            assertEquals(accountNumber, account.getAccountNumber());
            account.getAccountName();
            account.getBillingCountry();
        }

        @Test
        @DisplayName("billingAccountsSearch_getByAccountId - GET /billing/v2/billingAccounts/{accountId}")
        void billingAccountsSearch_getByAccountId() {
            String accountId = billingAccountsV2().stream()
                    .map(BillingAccountV2::getAccountId)
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst().orElse(null);
            Assumptions.assumeTrue(accountId != null,
                    "No billing account exposes an accountId; skipping get-by-id test");

            BillingAccountV2 account = requireEntitled("CustomerPortal", "getByAccountId", "BillingAccountV2", "GET",
                    () -> client.billingAccountsSearch().getByAccountId(accountId));
            assertNotNull(account);
            assertEquals(accountId, account.getAccountId());
        }
    }

    /** securecabinetv1: product availability catalog per billing account. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Secure Cabinets (securecabinetv1)")
    class SecureCabinetsTests {

        @Test
        @DisplayName("secureCabinets_getProductsAvailability - GET /securecabinet/v1/availability/{accountNumber}")
        void secureCabinets_getProductsAvailability() {
            PaginatedList<BillingAccountV2> accounts = billingAccountsV2();
            Assumptions.assumeTrue(!accounts.isEmpty(), "No billing accounts found; skipping availability test");

            String accountNumber = accounts.get(0).getAccountNumber();
            List<? extends ProductAvailability> availability = requireEntitled("CustomerPortal",
                    "getProductsAvailability", "ProductAvailability", "GET",
                    () -> client.secureCabinets().getProductsAvailability(accountNumber));
            assertNotNull(availability);
            if (!availability.isEmpty()) {
                availability.get(0).getIbx();
                availability.get(0).getCabinetDimensions();
            }
        }
    }

    /** smarthandsv1: static type catalog and permission-scoped locations. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Smart Hands (smarthandsv1)")
    class SmartHandsTests {

        @Test
        @DisplayName("smartHands_listTypes - Supported types (GET /v1/orders/smarthands/types)")
        void smartHands_listTypes() {
            List<? extends SmartHandType> types = requireEntitled("CustomerPortal", "listTypes",
                    "SmartHandType", "GET", () -> client.smartHandsRequests().listTypes());
            assertNotNull(types);
            assertTrue(types.size() > 0, "Expected at least one Smart Hands type in the static catalog");
            types.get(0).getType();
            types.get(0).getTypeDescription();
        }

        @Test
        @DisplayName("smartHands_listLocations - Permitted locations (GET /v1/orders/smarthands/locations)")
        void smartHands_listLocations() {
            List<? extends SmartHandsLocation> locations = requireEntitled("CustomerPortal", "listLocations",
                    "SmartHandsLocation", "GET", () -> client.smartHandsRequests().listLocations());
            assertNotNull(locations);
            if (!locations.isEmpty()) {
                locations.get(0).getIbx();
                locations.get(0).getCages();
            }
        }
    }

    /** workvisitv1: permission-scoped locations (op is marked deprecated in the spec but still live). */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Work Visits (workvisitv1)")
    class WorkVisitsTests {

        @Test
        @DisplayName("workVisits_listLocations - Permitted locations (GET /v1/orders/workvisit/locations, deprecated op)")
        void workVisits_listLocations() {
            List<? extends WorkVisitLocation> locations = requireEntitled("CustomerPortal", "listLocations",
                    "WorkVisitLocation", "GET", () -> client.workVisits().listLocations());
            assertNotNull(locations);
            if (!locations.isEmpty()) {
                locations.get(0).getIbx();
                locations.get(0).getCages();
            }
        }
    }

    /** shipmentsv1: permission-scoped locations. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Shipments (shipmentsv1)")
    class ShipmentsTests {

        @Test
        @DisplayName("shipments_listLocations - Permitted locations (GET /v1/orders/shipment/locations)")
        void shipments_listLocations() {
            List<? extends ShipmentLocation> locations = requireEntitled("CustomerPortal", "listLocations",
                    "ShipmentLocation", "GET", () -> client.shipments().listLocations());
            assertNotNull(locations);
            if (!locations.isEmpty()) {
                locations.get(0).getIbx();
                locations.get(0).getCages();
            }
        }
    }

    /** troubleticketv1: reference data for placing trouble-ticket orders. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Trouble Ticket Orders (troubleticketv1)")
    class TroubleTicketOrdersTests {

        @Test
        @DisplayName("troubleTicketOrders_getTypes - Problem categories (GET /v1/orders/troubleticket/types)")
        void troubleTicketOrders_getTypes() {
            List<? extends TroubleTicketType> types = requireEntitled("CustomerPortal", "getTypes",
                    "TroubleTicketType", "GET", () -> client.troubleTicketOrders().getTypes());
            assertNotNull(types);
            assertTrue(types.size() > 0, "Expected at least one trouble ticket problem category");
            types.get(0).getCategory();
            types.get(0).getCode();
        }

        @Test
        @DisplayName("troubleTicketOrders_getLocations - Permitted locations (GET /v1/orders/troubleticket/locations)")
        void troubleTicketOrders_getLocations() {
            List<? extends TroubleTicketOrderLocation> locations = requireEntitled("CustomerPortal", "getLocations",
                    "TroubleTicketOrderLocation", "GET", () -> client.troubleTicketOrders().getLocations());
            assertNotNull(locations);
            if (!locations.isEmpty()) {
                locations.get(0).getIbx();
                locations.get(0).getCages();
            }
        }
    }

    /** ticketsv2: {@code GET /v2/tickets/{id}}. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Trouble Tickets (ticketsv2)")
    class TroubleTicketsTests {

        /** Tickets v2 has no list op; supply {@code -DtroubleTicketId} to exercise the item-GET. */
        @Test
        @DisplayName("troubleTickets_getByUuid - Retrieve a ticket (GET /v2/tickets/{id})")
        void troubleTickets_getByUuid() {
            String ticketId = System.getProperty("troubleTicketId");
            Assumptions.assumeTrue(ticketId != null && !ticketId.isBlank(),
                    "TroubleTicket skipped: no -DtroubleTicketId supplied (ticketsv2 has no list op to discover one)");

            TroubleTicket ticket = requireEntitled("CustomerPortal", "getByUuid", "TroubleTicket", "GET",
                    () -> client.troubleTickets().getByUuid(ticketId));
            assertNotNull(ticket);
            ticket.getId();
            ticket.getStatus();
            ticket.getCategory();
        }
    }

    /** supportv2: ticket details, email details and attachment download, chained from one known case. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Support Cases (supportv2)")
    class SupportCasesTests {

        @Test
        @DisplayName("supportCases_getByCaseOrOrderNumber - Ticket details (GET /support/v2/tickets/{id})")
        void supportCases_getByCaseOrOrderNumber() {
            SupportCase supportCase = supportCase();
            assertNotNull(supportCase);
            supportCase.getId();
            supportCase.getStatus();
            supportCase.getAccountNumber();
        }

        @Test
        @DisplayName("supportCases_getEmailDetails - Email details (GET /support/v1/tickets/emailDetails/{emailId}/caseNumber/{caseNumber})")
        void supportCases_getEmailDetails() {
            SupportCase supportCase = supportCase();
            List<SupportCaseEmail> emails = supportCase.getEmail();
            Assumptions.assumeTrue(emails != null && !emails.isEmpty(),
                    "Support case has no emails; skipping email details test");

            String emailId = emails.get(0).getId();
            String caseNumber = supportCase.getId() != null
                    ? supportCase.getId() : System.getProperty("supportCaseId");
            EmailDetails details = requireEntitled("CustomerPortal", "getEmailDetails", "EmailDetails", "GET",
                    () -> client.supportCases().getEmailDetails(emailId, caseNumber));
            assertNotNull(details);
            details.getSubject();
            details.getFromAddress();
        }

        @Test
        @DisplayName("supportCases_downloadAttachment - Attachment download (GET /support/v2/tickets/attachment/download/{caseId}/{attachmentId})")
        void supportCases_downloadAttachment() {
            SupportCase supportCase = supportCase();
            List<SupportCaseAttachmentInfo> attachments = supportCase.getAttachments();
            Assumptions.assumeTrue(attachments != null && !attachments.isEmpty(),
                    "Support case has no attachments; skipping attachment download test");

            String caseId = supportCase.getId() != null
                    ? supportCase.getId() : System.getProperty("supportCaseId");
            String attachmentId = attachments.get(0).getId();
            byte[] bytes = requireEntitled("CustomerPortal", "downloadAttachment", "SupportCaseAttachment", "GET",
                    () -> client.supportCases().downloadAttachment(caseId, attachmentId));
            assertNotNull(bytes);
            assertTrue(bytes.length > 0, "Downloaded attachment should not be empty");
        }
    }

    /** notificationsv1: IBX and network notification searches plus their item-GETs. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Notifications (notificationsv1)")
    class NotificationsTests {

        @Test
        @DisplayName("notifications_searchIbx - Search IBX notifications (POST /v1/notifications/ibx/search)")
        void notifications_searchIbx() {
            List<? extends Notification> items = ibxNotifications();
            assertNotNull(items);
            if (!items.isEmpty()) {
                Notification first = items.get(0);
                assertNotNull(first.getId());
                first.getType();
                first.getStatus();
            }
        }

        @Test
        @DisplayName("notifications_getIbxById - IBX notification details (GET /v1/notifications/ibx/{id})")
        void notifications_getIbxById() {
            List<? extends Notification> items = ibxNotifications();
            Assumptions.assumeTrue(!items.isEmpty(), "No IBX notifications found; skipping get test");

            String id = items.get(0).getId();
            Notification item = requireEntitled("CustomerPortal", "getIbxById", "Notification", "GET",
                    () -> client.notifications().getIbxById(id));
            assertNotNull(item);
            item.getSummary();
            item.getIbxs();
        }

        @Test
        @DisplayName("notifications_searchNetwork - Search network notifications (POST /v1/notifications/network/search)")
        void notifications_searchNetwork() {
            List<? extends Notification> items = networkNotifications();
            assertNotNull(items);
            if (!items.isEmpty()) {
                Notification first = items.get(0);
                assertNotNull(first.getId());
                first.getType();
                first.getStatus();
            }
        }

        @Test
        @DisplayName("notifications_getNetworkById - Network notification details (GET /v1/notifications/network/{id})")
        void notifications_getNetworkById() {
            List<? extends Notification> items = networkNotifications();
            Assumptions.assumeTrue(!items.isEmpty(), "No network notifications found; skipping get test");

            String id = items.get(0).getId();
            Notification item = requireEntitled("CustomerPortal", "getNetworkById", "Notification", "GET",
                    () -> client.notifications().getNetworkById(id));
            assertNotNull(item);
            item.getSummary();
            item.getIbxs();
        }
    }

    /** unifiednotificationv2: the events findAll search. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Unified Notifications (unifiednotificationv2)")
    class UnifiedNotificationsTests {

        @Test
        @DisplayName("unifiedNotifications_search - Search events (POST /notifications/v2/events/findAll)")
        void unifiedNotifications_search() {
            UnifiedNotificationSearchRequest request = UnifiedNotificationSearchRequest.builder()
                    .pagination(new UnifiedNotificationSearchRequest.PaginationRequest(0, 20))
                    .build();
            List<? extends UnifiedNotification> items = requireEntitled("CustomerPortal", "getNotifications",
                    "UnifiedNotification", "POST", () -> client.unifiedNotifications().getNotifications(request));
            assertNotNull(items);
            if (!items.isEmpty()) {
                UnifiedNotification first = items.get(0);
                first.getId();
                first.getCategory();
                first.getNotificationNumber();
            }
        }
    }

    /** assetsv1: asset search plus asset detail by id. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Assets (assetsv1)")
    class AssetsTests {

        @Test
        @DisplayName("assets_search - Search assets (POST /v1/assets/search)")
        void assets_search() {
            PaginatedList<Asset> items = assetList();
            assertNotNull(items);
            if (!items.isEmpty()) {
                Asset first = items.get(0);
                assertNotNull(first.getAssetNumber());
                first.getIbx();
                first.getProductName();
            }
        }

        @Test
        @DisplayName("assets_getByUuid - Asset details (GET /v1/assets/{assetId})")
        void assets_getByUuid() {
            PaginatedList<Asset> items = assetList();
            Assumptions.assumeTrue(!items.isEmpty(), "No assets found; skipping asset get test");

            String assetNumber = items.get(0).getAssetNumber();
            Asset asset = requireEntitled("CustomerPortal", "getByUuid", "Asset", "GET",
                    () -> client.assets().getByUuid(assetNumber));
            assertNotNull(asset);
            assertEquals(assetNumber, asset.getAssetNumber());
            asset.getStatus();
            asset.getSerialNumber();
        }
    }

    /** attachmentsv1: metadata list, single metadata get and binary download. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Attachments (attachmentsv1)")
    class AttachmentsTests {

        @Test
        @DisplayName("attachments_list - Attachment metadata (GET /v1/attachments)")
        void attachments_list() {
            PaginatedList<Attachment> items = attachmentList();
            assertNotNull(items);
            if (!items.isEmpty()) {
                Attachment first = items.get(0);
                assertNotNull(first.getAttachmentId());
                first.getAttachmentName();
                first.getAttachmentType();
            }
        }

        @Test
        @DisplayName("attachments_getByUuid - Single attachment metadata (GET /v1/attachments/{attachmentId})")
        void attachments_getByUuid() {
            PaginatedList<Attachment> items = attachmentList();
            Assumptions.assumeTrue(!items.isEmpty(), "No attachments found; skipping attachment get test");

            String attachmentId = items.get(0).getAttachmentId();
            Attachment attachment = requireEntitled("CustomerPortal", "getByUuid", "Attachment", "GET",
                    () -> client.attachments().getByUuid(attachmentId));
            assertNotNull(attachment);
            assertEquals(attachmentId, attachment.getAttachmentId());
            attachment.getAttachmentSize();
        }

        @Test
        @DisplayName("attachments_download - Binary download (GET /v1/attachments/{attachmentId}/file)")
        void attachments_download() {
            PaginatedList<Attachment> items = attachmentList();
            Assumptions.assumeTrue(!items.isEmpty(), "No attachments found; skipping attachment download test");

            String attachmentId = items.get(0).getAttachmentId();
            byte[] bytes = requireEntitled("CustomerPortal", "download", "AttachmentFile", "GET",
                    () -> client.attachments().download(attachmentId));
            assertNotNull(bytes);
            assertTrue(bytes.length > 0, "Downloaded attachment file should not be empty");
        }
    }

    /** reportsv1: generated reports, scheduled reports and the report-definition catalog. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Reports (reportsv1)")
    class ReportsTests {

        @Test
        @DisplayName("reports_getReports - Triggered reports (GET /v1/reportCenter/reports)")
        void reports_getReports() {
            PaginatedList<Report> reports = reportList();
            assertNotNull(reports);
            if (!reports.isEmpty()) {
                Report first = reports.get(0);
                assertNotNull(first.getReportId());
                first.getReportName();
                first.getStatus();
            }
        }

        @Test
        @DisplayName("reports_getReportById - Report details (GET /v1/reportCenter/reports/{reportId})")
        void reports_getReportById() {
            PaginatedList<Report> reports = reportList();
            Assumptions.assumeTrue(!reports.isEmpty(), "No reports found; skipping report get test");

            String reportId = reports.get(0).getReportId();
            Report report = requireEntitled("CustomerPortal", "getReportById", "Report", "GET",
                    () -> client.reports().getReportById(reportId));
            assertNotNull(report);
            assertEquals(reportId, report.getReportId());
            report.getFileName();
            report.getGeneratedDate();
        }

        /**
         * Downloads an already-generated report; per the spec the only server effect is the
         * documented {@code numberOfDownloads} bookkeeping counter. Skips unless a report in
         * {@link ReportStatus#SUCCESS} exists, since only generated reports have a file.
         */
        @Test
        @DisplayName("reports_downloadReport - Report file download (GET /v1/reportCenter/reports/{reportId}/file)")
        void reports_downloadReport() {
            Report generated = reportList().stream()
                    .filter(r -> r.getStatus() == ReportStatus.SUCCESS)
                    .findFirst().orElse(null);
            Assumptions.assumeTrue(generated != null,
                    "No successfully generated report found; skipping report download test");

            String reportId = generated.getReportId();
            byte[] bytes = requireEntitled("CustomerPortal", "downloadReport", "ReportFile", "GET",
                    () -> client.reports().downloadReport(reportId));
            assertNotNull(bytes);
            assertTrue(bytes.length > 0, "Downloaded report file should not be empty");
        }

        @Test
        @DisplayName("reports_getScheduledReports - Scheduled reports (GET /v1/reportCenter/reports/scheduler)")
        void reports_getScheduledReports() {
            List<? extends ScheduledReport> scheduled = scheduledReportList();
            assertNotNull(scheduled);
            if (!scheduled.isEmpty()) {
                ScheduledReport first = scheduled.get(0);
                assertNotNull(first.getScheduledId());
                first.getReportName();
                first.getScheduleType();
            }
        }

        @Test
        @DisplayName("reports_getScheduledReport - Scheduled report details (GET /v1/reportCenter/reports/scheduler/{scheduledId})")
        void reports_getScheduledReport() {
            List<? extends ScheduledReport> scheduled = scheduledReportList();
            Assumptions.assumeTrue(!scheduled.isEmpty(), "No scheduled reports found; skipping scheduled get test");

            String scheduledId = scheduled.get(0).getScheduledId();
            ScheduledReport report = requireEntitled("CustomerPortal", "getScheduledReport", "ScheduledReport", "GET",
                    () -> client.reports().getScheduledReport(scheduledId));
            assertNotNull(report);
            assertEquals(scheduledId, report.getScheduledId());
            report.getPeriod();
            report.getStatus();
        }

        @Test
        @DisplayName("reports_getReportDefinitions - Definition catalog (GET /v1/reportCenter/reports/definitions)")
        void reports_getReportDefinitions() {
            List<ReportDefinitionJson> definitions = reportDefinitionList();
            assertNotNull(definitions);
            assertTrue(definitions.size() > 0, "Expected at least one report definition in the static catalog");
            definitions.get(0).getName();
            definitions.get(0).getParameters();
        }

        @Test
        @DisplayName("reports_getReportDefinition - Single definition (GET /v1/reportCenter/reports/definitions/{reportName})")
        void reports_getReportDefinition() {
            List<ReportDefinitionJson> definitions = reportDefinitionList();
            Assumptions.assumeTrue(!definitions.isEmpty(), "No report definitions found; skipping definition get test");

            String reportName = definitions.get(0).getName();
            ReportDefinitionJson definition = requireEntitled("CustomerPortal", "getReportDefinition",
                    "ReportDefinition", "GET", () -> client.reports().getReportDefinition(reportName));
            assertNotNull(definition);
            assertEquals(reportName, definition.getName());
            definition.getScheduleType();
        }
    }

    /** diloav1: beta gates, LOA search and its item-GET / change chains, and organizations. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Digital LOAs (diloav1)")
    class DigitalLoasTests {

        @Test
        @DisplayName("digitalLoas_isPrivateBetaAllowed - Permission check (GET /diloa/v1/privateBetaAccess)")
        void digitalLoas_isPrivateBetaAllowed() {
            PrivateBetaPermission permission = requireEntitled("CustomerPortal", "isPrivateBetaAllowed",
                    "PrivateBetaPermission", "GET", () -> client.digitalLoas().isPrivateBetaAllowed());
            assertNotNull(permission);
            permission.getPrivateBetaTests();
        }

        @Test
        @DisplayName("digitalLoas_getBetaTermsAgreement - Terms status (GET /diloa/v1/betaTermsAgreement)")
        void digitalLoas_getBetaTermsAgreement() {
            BetaTermsAgreement agreement = requireEntitled("CustomerPortal", "getBetaTermsAgreement",
                    "BetaTermsAgreement", "GET", () -> client.digitalLoas().getBetaTermsAgreement());
            assertNotNull(agreement);
            agreement.getAgreementAccepted();
        }

        @Test
        @DisplayName("digitalLoas_search - Search LOAs (POST /diloa/v1/digitalLoas/search)")
        void digitalLoas_search() {
            List<? extends DigitalLoa> loas = digitalLoaList();
            assertNotNull(loas);
            if (!loas.isEmpty()) {
                DigitalLoa first = loas.get(0);
                assertNotNull(first.getUuid());
                first.getState();
                first.getExpiryDateTime();
            }
        }

        @Test
        @DisplayName("digitalLoas_findByUuid - LOA details (GET /diloa/v1/digitalLoas/{uuid})")
        void digitalLoas_findByUuid() {
            List<? extends DigitalLoa> loas = digitalLoaList();
            Assumptions.assumeTrue(!loas.isEmpty(), "No digital LOAs found; skipping LOA get test");

            String uuid = loas.get(0).getUuid();
            DigitalLoa loa = requireEntitled("CustomerPortal", "findByUuid", "DigitalLoa", "GET",
                    () -> client.digitalLoas().findByUuid(uuid));
            assertNotNull(loa);
            assertEquals(uuid, loa.getUuid());
            loa.getProducts();
            loa.getRequestor();
        }

        @Test
        @DisplayName("digitalLoas_findChangesByLoaUuid - LOA changes (GET /diloa/v1/digitalLoas/{uuid}/changes)")
        void digitalLoas_findChangesByLoaUuid() {
            List<? extends DigitalLoa> loas = digitalLoaList();
            Assumptions.assumeTrue(!loas.isEmpty(), "No digital LOAs found; skipping LOA changes test");

            String uuid = loas.get(0).getUuid();
            List<? extends DigitalLoaChange> changes = requireEntitled("CustomerPortal", "findChangesByLoaUuid",
                    "DigitalLoaChange", "GET", () -> client.digitalLoas().findChangesByLoaUuid(uuid));
            assertNotNull(changes);
            if (!changes.isEmpty()) {
                changes.get(0).getChangeType();
                changes.get(0).getStatus();
            }
        }

        @Test
        @DisplayName("digitalLoas_findChangeByUuid - LOA change details (GET /diloa/v1/digitalLoas/{uuid}/changes/{changeUuid})")
        void digitalLoas_findChangeByUuid() {
            List<? extends DigitalLoa> loas = digitalLoaList();
            Assumptions.assumeTrue(!loas.isEmpty(), "No digital LOAs found; skipping LOA change get test");

            String uuid = loas.get(0).getUuid();
            List<? extends DigitalLoaChange> changes = requireEntitled("CustomerPortal", "findChangesByLoaUuid",
                    "DigitalLoaChange", "GET", () -> client.digitalLoas().findChangesByLoaUuid(uuid));
            Assumptions.assumeTrue(changes != null && !changes.isEmpty(),
                    "Digital LOA " + uuid + " has no changes; skipping change get test");

            String changeUuid = changes.get(0).getUuid();
            DigitalLoaChange change = requireEntitled("CustomerPortal", "findChangeByUuid", "DigitalLoaChange", "GET",
                    () -> client.digitalLoas().findChangeByUuid(uuid, changeUuid));
            assertNotNull(change);
            assertEquals(changeUuid, change.getUuid());
            change.getDescription();
        }

        @Test
        @DisplayName("digitalLoas_listOrganizations - Organizations in an IBX (GET /diloa/v1/organizations)")
        void digitalLoas_listOrganizations() {
            String ibx = lookupLocations().stream()
                    .map(LookupLocation::getIbx)
                    .filter(code -> code != null && !code.isBlank())
                    .findFirst().orElse(null);
            Assumptions.assumeTrue(ibx != null, "No permitted IBX found via lookups; skipping organizations test");

            String finalIbx = ibx;
            List<? extends LoaCustomerOrganization> organizations = requireEntitled("CustomerPortal",
                    "listOrganizations", "LoaCustomerOrganization", "GET",
                    () -> client.digitalLoas().listOrganizations(finalIbx));
            assertNotNull(organizations);
            if (!organizations.isEmpty()) {
                organizations.get(0).getName();
                organizations.get(0).getOrgIds();
            }
        }
    }

    /** lookupv2: ordering reference data — locations, patch panels, providers, connection services. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Lookups (lookupv2)")
    class LookupsTests {

        @Test
        @DisplayName("lookups_listLocations - Locations by permission code (GET /colocations/v2/locations)")
        void lookups_listLocations() {
            List<? extends LookupLocation> locations = lookupLocations();
            assertNotNull(locations);
            if (!locations.isEmpty()) {
                LookupLocation first = locations.get(0);
                first.getIbx();
                first.getAccounts();
                first.getCages();
            }
        }

        @Test
        @DisplayName("lookups_listPatchPanels - Patch panels for a cabinet (GET /colocations/v2/patchPanels)")
        void lookups_listPatchPanels() {
            String cabinetId = firstCabinetId();
            Assumptions.assumeTrue(cabinetId != null, "No cabinet found via lookups; skipping patch panels test");

            List<? extends PatchPanel> patchPanels = requireEntitled("CustomerPortal", "listPatchPanels",
                    "PatchPanel", "GET", () -> client.lookups().listPatchPanels(cabinetId));
            assertNotNull(patchPanels);
            if (!patchPanels.isEmpty()) {
                patchPanels.get(0).getPatchPanelId();
                patchPanels.get(0).getType();
            }
        }

        @Test
        @DisplayName("lookups_getPatchPanelById - Patch panel details (GET /colocations/v2/patchPanels/{patchPanelId})")
        void lookups_getPatchPanelById() {
            String cabinetId = firstCabinetId();
            Assumptions.assumeTrue(cabinetId != null, "No cabinet found via lookups; skipping patch panel get test");

            List<? extends PatchPanel> patchPanels = requireEntitled("CustomerPortal", "listPatchPanels",
                    "PatchPanel", "GET", () -> client.lookups().listPatchPanels(cabinetId));
            Assumptions.assumeTrue(patchPanels != null && !patchPanels.isEmpty(),
                    "No patch panels on cabinet " + cabinetId + "; skipping patch panel get test");

            String patchPanelId = patchPanels.get(0).getPatchPanelId();
            PatchPanel patchPanel = requireEntitled("CustomerPortal", "getPatchPanelById", "PatchPanel", "GET",
                    () -> client.lookups().getPatchPanelById(patchPanelId));
            assertNotNull(patchPanel);
            assertEquals(patchPanelId, patchPanel.getPatchPanelId());
            patchPanel.getAvailablePortCount();
        }

        @Test
        @DisplayName("lookups_listProviders - Cross connect providers (GET /colocations/v2/providers)")
        void lookups_listProviders() {
            String cageId = null;
            String accountNumber = null;
            for (LookupLocation location : lookupLocations()) {
                if (location.getCages() != null && !location.getCages().isEmpty()
                        && location.getAccounts() != null && !location.getAccounts().isEmpty()) {
                    CageDetails cage = location.getCages().get(0);
                    Account account = location.getAccounts().get(0);
                    cageId = cage.getId();
                    accountNumber = account.getAccountNumber();
                    break;
                }
            }
            Assumptions.assumeTrue(cageId != null && accountNumber != null,
                    "No cage/account pair found via lookups; skipping providers test");

            String finalCageId = cageId;
            String finalAccountNumber = accountNumber;
            List<? extends Provider> providers = requireEntitled("CustomerPortal", "listProviders",
                    "Provider", "GET", () -> client.lookups().listProviders(finalCageId, finalAccountNumber));
            assertNotNull(providers);
            if (!providers.isEmpty()) {
                providers.get(0).getProviderAccountName();
                providers.get(0).getProviderAccountNumber();
            }
        }

        @Test
        @DisplayName("lookups_listConnectionServices - Connection services (GET /colocations/v2/connectionServices)")
        void lookups_listConnectionServices() {
            String ibx = lookupLocations().stream()
                    .map(LookupLocation::getIbx)
                    .filter(code -> code != null && !code.isBlank())
                    .findFirst().orElse(null);
            Assumptions.assumeTrue(ibx != null, "No permitted IBX found via lookups; skipping connection services test");

            String finalIbx = ibx;
            List<? extends ConnectionService> services = requireEntitled("CustomerPortal", "listConnectionServices",
                    "ConnectionService", "GET", () -> client.lookups().listConnectionServices(finalIbx));
            assertNotNull(services);
            if (!services.isEmpty()) {
                services.get(0).getName();
                services.get(0).getMediaTypes();
            }
        }

        private String firstCabinetId() {
            for (LookupLocation location : lookupLocations()) {
                if (location.getCages() != null) {
                    for (CageDetails cage : location.getCages()) {
                        if (cage.getCabinetId() != null && !cage.getCabinetId().isBlank()) {
                            return cage.getCabinetId();
                        }
                    }
                }
            }
            return null;
        }
    }

    /** supportplansv2: Smart Hands support plans. */
    @Nested
    @Tag("integration-readonly")
    @DisplayName("Support Plans (supportplansv2)")
    class SupportPlansTests {

        @Test
        @DisplayName("supportPlans_list - Smart Hands support plans (GET /colocations/v2/supportPlans)")
        void supportPlans_list() {
            PaginatedList<SupportPlan> plans = requireEntitled("CustomerPortal", "list", "SupportPlan", "GET",
                    () -> client.supportPlans().list());
            assertNotNull(plans);
            if (!plans.isEmpty()) {
                SupportPlan first = plans.get(0);
                first.getPlanName();
                first.getRemainingMinutes();
                first.getStatus();
            }
        }
    }
}
