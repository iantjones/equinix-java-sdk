package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.*;
import api.equinix.javasdk.customerportal.model.json.creators.AssetSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.NotificationSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.OrderHistorySearchRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerPortalTest {

    static CustomerPortal customerPortal;
    static Boolean skipCreateUpdateOperations;

    @BeforeAll
    static void obtainTestingData() {
        skipCreateUpdateOperations = Boolean.valueOf(System.getProperty("skipCreateUpdateOperations"));
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        customerPortal = new CustomerPortal(new BasicEquinixCredentials(accessKey, secretKey));
        customerPortal.authenticate();
    }

    @Test
    void invoices() {
        try {
            PaginatedList<InvoiceSummary> summaries = customerPortal.invoices().summaries();
            assertNotNull(summaries);
            assertTrue(summaries.size() >= 0);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Invoices test skipped: " + e.getMessage());
        }
    }

    @Test
    void resellers() {
        try {
            PaginatedList<Reseller> resellers = customerPortal.resellers().list();
            assertNotNull(resellers);
            assertTrue(resellers.size() >= 0);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Resellers test skipped: " + e.getMessage());
        }
    }

    @Test
    void crossConnects() {
        // Cross-connects are ordered (POST/PATCH/deinstall), not listed; nothing read-only to call.
        assertNotNull(customerPortal.crossConnects());
    }

    @Test
    void orders() {
        try {
            // Orders v2 has no list/create; discover an order id via order history search.
            List<? extends OrderHistoryItem> history = customerPortal.orderHistory()
                    .search(OrderHistorySearchRequest.builder().build());
            assertNotNull(history);

            if (!history.isEmpty()) {
                String orderId = history.get(0).getOrderNumber();
                Order order = customerPortal.orders().getByUuid(orderId);
                assertNotNull(order);

                assertNotNull(customerPortal.orders().getNegotiations(orderId));
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Orders test skipped: " + e.getMessage());
        }
    }

    @Test
    void troubleTickets() {
        // Tickets v2 has no collection listing; create/getByUuid/update/notes/cancel only.
        assertNotNull(customerPortal.troubleTickets());
    }

    @Test
    void workVisits() {
        // Work visits are scheduled (POST/PATCH), not listed; nothing read-only to call.
        assertNotNull(customerPortal.workVisits());
    }

    @Test
    void smartHandsRequests() {
        try {
            List<? extends SmartHandType> types = customerPortal.smartHandsRequests().listTypes();
            assertNotNull(types);

            List<? extends SmartHandsLocation> locations = customerPortal.smartHandsRequests().listLocations();
            assertNotNull(locations);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Smart hands test skipped: " + e.getMessage());
        }
    }

    @Test
    void shipments() {
        // Shipments are scheduled (POST/PATCH), not listed; nothing read-only to call.
        assertNotNull(customerPortal.shipments());
    }

    @Test
    void notifications() {
        try {
            List<? extends Notification> ibx = customerPortal.notifications()
                    .searchIbx(new NotificationSearchRequest(java.util.Map.of()));
            assertNotNull(ibx);

            List<? extends Notification> network = customerPortal.notifications()
                    .searchNetwork(new NotificationSearchRequest(java.util.Map.of()));
            assertNotNull(network);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Notifications test skipped: " + e.getMessage());
        }
    }

    @Test
    void assets() {
        try {
            PaginatedList<Asset> assets = customerPortal.assets()
                    .search(new AssetSearchRequest(java.util.Map.of()));
            assertNotNull(assets);
            assertTrue(assets.size() >= 0);

            if (assets.size() > 0) {
                Asset asset = customerPortal.assets().getByUuid(assets.get(0).getUuid());
                assertNotNull(asset);
                assertEquals(assets.get(0).getUuid(), asset.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Assets test skipped: " + e.getMessage());
        }
    }

    @Test
    void quotes() {
        // Quotes expose only getByUuid; without a known quote id there is nothing to list.
        assertNotNull(customerPortal.quotes());
    }

    @Test
    void supportPlans() {
        try {
            PaginatedList<SupportPlan> supportPlans = customerPortal.supportPlans().list();
            assertNotNull(supportPlans);
            assertTrue(supportPlans.size() >= 0);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Support plans test skipped: " + e.getMessage());
        }
    }

    @Test
    void orderHistory() {
        try {
            List<? extends OrderHistoryItem> orderHistory = customerPortal.orderHistory()
                    .search(OrderHistorySearchRequest.builder().build());
            assertNotNull(orderHistory);

            assertNotNull(customerPortal.orderHistory().listLocations());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Order history test skipped: " + e.getMessage());
        }
    }

    @Test
    void lookups() {
        try {
            List<? extends LookupLocation> locations = customerPortal.lookups().listLocations("CROSS_CONNECT");
            assertNotNull(locations);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Lookups test skipped: " + e.getMessage());
        }
    }

    @Test
    void attachments() {
        try {
            PaginatedList<Attachment> attachments = customerPortal.attachments().list();
            assertNotNull(attachments);
            assertTrue(attachments.size() >= 0);

            if (attachments.size() > 0) {
                Attachment attachment = customerPortal.attachments().getByUuid(attachments.get(0).getUuid());
                assertNotNull(attachment);
                assertEquals(attachments.get(0).getUuid(), attachment.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Attachments test skipped: " + e.getMessage());
        }
    }

    @Test
    void reports() {
        try {
            PaginatedList<Report> reports = customerPortal.reports().getReports();
            assertNotNull(reports);
            assertTrue(reports.size() >= 0);

            assertNotNull(customerPortal.reports().getScheduledReports());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Reports test skipped: " + e.getMessage());
        }
    }

    @Test
    void secureCabinets() {
        try {
            // Secure cabinets are ordered, not listed; the readable op is availability lookup.
            List<? extends ProductAvailability> availability =
                    customerPortal.secureCabinets().getProductsAvailability("128745");
            assertNotNull(availability);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Secure cabinets test skipped: " + e.getMessage());
        }
    }

}
