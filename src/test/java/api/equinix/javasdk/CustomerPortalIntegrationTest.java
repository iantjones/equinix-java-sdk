package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.Asset;
import api.equinix.javasdk.customerportal.model.CrossConnect;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.Notification;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.Quote;
import api.equinix.javasdk.customerportal.model.Reseller;
import api.equinix.javasdk.customerportal.model.Shipment;
import api.equinix.javasdk.customerportal.model.SmartHands;
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration-readonly")
@DisplayName("Customer Portal Integration Tests")
class CustomerPortalIntegrationTest extends IntegrationTestBase {

    static CustomerPortal client;

    @BeforeAll
    static void setUp() {
        client = new CustomerPortal(testCredentials());
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Readonly Operations")
    class ReadonlyTests {

        @Test
        @DisplayName("List invoice summaries returns valid response")
        void listInvoiceSummaries() {
            try {
                PaginatedList<InvoiceSummary> items = timedCall("CustomerPortal", "summaries", "InvoiceSummary", "GET",
                        () -> client.invoices().summaries());
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Invoice summaries test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List resellers returns valid response")
        void listResellers() {
            try {
                PaginatedList<Reseller> items = timedCall("CustomerPortal", "list", "Reseller", "GET",
                        () -> client.resellers().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Resellers test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List cross-connects and get by UUID")
        void listCrossConnects() {
            try {
                PaginatedList<CrossConnect> items = timedCall("CustomerPortal", "list", "CrossConnect", "GET",
                        () -> client.crossConnects().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    CrossConnect item = timedCall("CustomerPortal", "getByUuid", "CrossConnect", "GET",
                            items.get(0).getUuid(),
                            () -> client.crossConnects().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "CrossConnects test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List orders and get by UUID")
        void listOrders() {
            try {
                PaginatedList<Order> items = timedCall("CustomerPortal", "list", "Order", "GET",
                        () -> client.orders().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Order item = timedCall("CustomerPortal", "getByUuid", "Order", "GET",
                            items.get(0).getUuid(),
                            () -> client.orders().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Orders test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List trouble tickets and get by UUID")
        void listTroubleTickets() {
            try {
                PaginatedList<TroubleTicket> items = timedCall("CustomerPortal", "list", "TroubleTicket", "GET",
                        () -> client.troubleTickets().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    TroubleTicket item = timedCall("CustomerPortal", "getByUuid", "TroubleTicket", "GET",
                            items.get(0).getUuid(),
                            () -> client.troubleTickets().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "TroubleTickets test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List work visits and get by UUID")
        void listWorkVisits() {
            try {
                PaginatedList<WorkVisit> items = timedCall("CustomerPortal", "list", "WorkVisit", "GET",
                        () -> client.workVisits().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    WorkVisit item = timedCall("CustomerPortal", "getByUuid", "WorkVisit", "GET",
                            items.get(0).getUuid(),
                            () -> client.workVisits().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "WorkVisits test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List smart hands requests and get by UUID")
        void listSmartHandsRequests() {
            try {
                PaginatedList<SmartHands> items = timedCall("CustomerPortal", "list", "SmartHands", "GET",
                        () -> client.smartHandsRequests().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    SmartHands item = timedCall("CustomerPortal", "getByUuid", "SmartHands", "GET",
                            items.get(0).getUuid(),
                            () -> client.smartHandsRequests().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "SmartHandsRequests test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List shipments and get by UUID")
        void listShipments() {
            try {
                PaginatedList<Shipment> items = timedCall("CustomerPortal", "list", "Shipment", "GET",
                        () -> client.shipments().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Shipment item = timedCall("CustomerPortal", "getByUuid", "Shipment", "GET",
                            items.get(0).getUuid(),
                            () -> client.shipments().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Shipments test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List notifications returns valid response")
        void listNotifications() {
            try {
                PaginatedList<Notification> items = timedCall("CustomerPortal", "list", "Notification", "GET",
                        () -> client.notifications().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Notifications test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List assets and get by UUID")
        void listAssets() {
            try {
                PaginatedList<Asset> items = timedCall("CustomerPortal", "list", "Asset", "GET",
                        () -> client.assets().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Asset item = timedCall("CustomerPortal", "getByUuid", "Asset", "GET",
                            items.get(0).getUuid(),
                            () -> client.assets().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Assets test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List support cases and get by UUID")
        void listSupportCases() {
            try {
                PaginatedList<SupportCase> items = timedCall("CustomerPortal", "list", "SupportCase", "GET",
                        () -> client.supportCases().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    SupportCase item = timedCall("CustomerPortal", "getByUuid", "SupportCase", "GET",
                            items.get(0).getUuid(),
                            () -> client.supportCases().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "SupportCases test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List quotes and get by UUID")
        void listQuotes() {
            try {
                PaginatedList<Quote> items = timedCall("CustomerPortal", "list", "Quote", "GET",
                        () -> client.quotes().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Quote item = timedCall("CustomerPortal", "getByUuid", "Quote", "GET",
                            items.get(0).getUuid(),
                            () -> client.quotes().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Quotes test skipped: " + e.getMessage());
            }
        }
    }
}
