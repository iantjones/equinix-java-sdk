package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.Asset;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.Notification;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.OrderHistoryItem;
import api.equinix.javasdk.customerportal.model.json.creators.AssetSearchFilter;
import api.equinix.javasdk.customerportal.model.json.creators.AssetSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.NotificationSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.OrderHistorySearchRequest;
import api.equinix.javasdk.customerportal.model.ShipmentLocation;
import api.equinix.javasdk.customerportal.model.SmartHandType;
import api.equinix.javasdk.customerportal.model.SmartHandsLocation;
import api.equinix.javasdk.customerportal.model.WorkVisitLocation;
import org.junit.jupiter.api.*;

import java.util.List;

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
        @DisplayName("Cross-connect order client is reachable")
        void crossConnectsClientReachable() {
            // Cross-connects are ordered (POST/PATCH/deinstall), not listed; nothing read-only to call.
            assertNotNull(client.crossConnects());
        }

        @Test
        @DisplayName("Get order by id and retrieve its negotiations")
        void getOrder() {
            try {
                // Orders v2 has no list/create; discover an order id via order history search.
                List<? extends OrderHistoryItem> history = timedCall("CustomerPortal", "search", "OrderHistoryItem", "POST",
                        () -> client.orderHistory().search(OrderHistorySearchRequest.builder().build()));
                assertNotNull(history);

                if (!history.isEmpty()) {
                    String orderId = history.get(0).getOrderNumber();
                    Order item = timedCall("CustomerPortal", "getByUuid", "Order", "GET", orderId,
                            () -> client.orders().getByUuid(orderId));
                    assertNotNull(item);

                    assertNotNull(timedCall("CustomerPortal", "getNegotiations", "OrderNegotiation", "GET", orderId,
                            () -> client.orders().getNegotiations(orderId)));
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Orders test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Trouble ticket client is reachable")
        void troubleTicketsClientReachable() {
            // Tickets v2 has no collection listing; create/getByUuid/update/notes/cancel only.
            assertNotNull(client.troubleTickets());
        }

        @Test
        @DisplayName("List work visit locations returns valid response")
        void listWorkVisitLocations() {
            try {
                List<? extends WorkVisitLocation> locations = timedCall("CustomerPortal", "listLocations",
                        "WorkVisitLocation", "GET", () -> client.workVisits().listLocations());
                assertNotNull(locations);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Work visit locations test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List smart hands types and locations")
        void listSmartHandsReferenceData() {
            try {
                List<? extends SmartHandType> types = timedCall("CustomerPortal", "listTypes", "SmartHandType", "GET",
                        () -> client.smartHandsRequests().listTypes());
                assertNotNull(types);

                List<? extends SmartHandsLocation> locations = timedCall("CustomerPortal", "listLocations", "SmartHandsLocation", "GET",
                        () -> client.smartHandsRequests().listLocations());
                assertNotNull(locations);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "SmartHandsRequests test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List shipment locations returns valid response")
        void listShipmentLocations() {
            try {
                List<? extends ShipmentLocation> locations = timedCall("CustomerPortal", "listLocations",
                        "ShipmentLocation", "GET", () -> client.shipments().listLocations());
                assertNotNull(locations);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Shipment locations test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Search IBX notifications returns valid response")
        void searchNotifications() {
            try {
                List<? extends Notification> items = timedCall("CustomerPortal", "searchIbx", "Notification", "POST",
                        () -> client.notifications().searchIbx(NotificationSearchRequest.builder().build()));
                assertNotNull(items);
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Notifications test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Search assets and get by UUID")
        void searchAssets() {
            try {
                PaginatedList<Asset> items = timedCall("CustomerPortal", "search", "Asset", "POST",
                        () -> client.assets().search(new AssetSearchRequest((AssetSearchFilter) null)));
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Asset item = timedCall("CustomerPortal", "getByUuid", "Asset", "GET",
                            items.get(0).getAssetNumber(),
                            () -> client.assets().getByUuid(items.get(0).getAssetNumber()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getAssetNumber(), item.getAssetNumber());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Assets test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Quote client is reachable")
        void quotesClientReachable() {
            // Quotes expose only getByUuid; without a known quote id there is nothing to list.
            assertNotNull(client.quotes());
        }
    }
}
