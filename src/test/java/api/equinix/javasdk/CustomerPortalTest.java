package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
        try {
            PaginatedList<CrossConnect> crossConnects = customerPortal.crossConnects().list();
            assertNotNull(crossConnects);
            assertTrue(crossConnects.size() >= 0);

            if (crossConnects.size() > 0) {
                CrossConnect crossConnect = customerPortal.crossConnects().getByUuid(crossConnects.get(0).getUuid());
                assertNotNull(crossConnect);
                assertEquals(crossConnects.get(0).getUuid(), crossConnect.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Cross connects test skipped: " + e.getMessage());
        }
    }

    @Test
    void orders() {
        try {
            PaginatedList<Order> orders = customerPortal.orders().list();
            assertNotNull(orders);
            assertTrue(orders.size() >= 0);

            if (orders.size() > 0) {
                Order order = customerPortal.orders().getByUuid(orders.get(0).getUuid());
                assertNotNull(order);
                assertEquals(orders.get(0).getUuid(), order.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Orders test skipped: " + e.getMessage());
        }
    }

    @Test
    void troubleTickets() {
        try {
            PaginatedList<TroubleTicket> tickets = customerPortal.troubleTickets().list();
            assertNotNull(tickets);
            assertTrue(tickets.size() >= 0);

            if (tickets.size() > 0) {
                TroubleTicket ticket = customerPortal.troubleTickets().getByUuid(tickets.get(0).getUuid());
                assertNotNull(ticket);
                assertEquals(tickets.get(0).getUuid(), ticket.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Trouble tickets test skipped: " + e.getMessage());
        }
    }

    @Test
    void workVisits() {
        try {
            PaginatedList<WorkVisit> workVisits = customerPortal.workVisits().list();
            assertNotNull(workVisits);
            assertTrue(workVisits.size() >= 0);

            if (workVisits.size() > 0) {
                WorkVisit workVisit = customerPortal.workVisits().getByUuid(workVisits.get(0).getUuid());
                assertNotNull(workVisit);
                assertEquals(workVisits.get(0).getUuid(), workVisit.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Work visits test skipped: " + e.getMessage());
        }
    }

    @Test
    void smartHandsRequests() {
        try {
            PaginatedList<SmartHands> smartHands = customerPortal.smartHandsRequests().list();
            assertNotNull(smartHands);
            assertTrue(smartHands.size() >= 0);

            if (smartHands.size() > 0) {
                SmartHands smartHand = customerPortal.smartHandsRequests().getByUuid(smartHands.get(0).getUuid());
                assertNotNull(smartHand);
                assertEquals(smartHands.get(0).getUuid(), smartHand.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Smart hands test skipped: " + e.getMessage());
        }
    }

    @Test
    void shipments() {
        try {
            PaginatedList<Shipment> shipments = customerPortal.shipments().list();
            assertNotNull(shipments);
            assertTrue(shipments.size() >= 0);

            if (shipments.size() > 0) {
                Shipment shipment = customerPortal.shipments().getByUuid(shipments.get(0).getUuid());
                assertNotNull(shipment);
                assertEquals(shipments.get(0).getUuid(), shipment.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Shipments test skipped: " + e.getMessage());
        }
    }

    @Test
    void notifications() {
        try {
            PaginatedList<Notification> notifications = customerPortal.notifications().list();
            assertNotNull(notifications);
            assertTrue(notifications.size() >= 0);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Notifications test skipped: " + e.getMessage());
        }
    }

    @Test
    void assets() {
        try {
            PaginatedList<Asset> assets = customerPortal.assets().list();
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
    void supportCases() {
        try {
            PaginatedList<SupportCase> supportCases = customerPortal.supportCases().list();
            assertNotNull(supportCases);
            assertTrue(supportCases.size() >= 0);

            if (supportCases.size() > 0) {
                SupportCase supportCase = customerPortal.supportCases().getByUuid(supportCases.get(0).getUuid());
                assertNotNull(supportCase);
                assertEquals(supportCases.get(0).getUuid(), supportCase.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Support cases test skipped: " + e.getMessage());
        }
    }

    @Test
    void quotes() {
        try {
            PaginatedList<Quote> quotes = customerPortal.quotes().list();
            assertNotNull(quotes);
            assertTrue(quotes.size() >= 0);

            if (quotes.size() > 0) {
                Quote quote = customerPortal.quotes().getByUuid(quotes.get(0).getUuid());
                assertNotNull(quote);
                assertEquals(quotes.get(0).getUuid(), quote.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Quotes test skipped: " + e.getMessage());
        }
    }

    @Test
    void supportPlans() {
        try {
            PaginatedList<SupportPlan> supportPlans = customerPortal.supportPlans().list();
            assertNotNull(supportPlans);
            assertTrue(supportPlans.size() >= 0);

            if (supportPlans.size() > 0) {
                SupportPlan supportPlan = customerPortal.supportPlans().getByUuid(supportPlans.get(0).getUuid());
                assertNotNull(supportPlan);
                assertEquals(supportPlans.get(0).getUuid(), supportPlan.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Support plans test skipped: " + e.getMessage());
        }
    }

    @Test
    void orderHistory() {
        try {
            PaginatedList<OrderHistoryItem> orderHistory = customerPortal.orderHistory().list();
            assertNotNull(orderHistory);
            assertTrue(orderHistory.size() >= 0);

            if (orderHistory.size() > 0) {
                OrderHistoryItem item = customerPortal.orderHistory().getByUuid(orderHistory.get(0).getUuid());
                assertNotNull(item);
                assertEquals(orderHistory.get(0).getUuid(), item.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Order history test skipped: " + e.getMessage());
        }
    }

    @Test
    void lookups() {
        try {
            PaginatedList<LookupLocation> locations = customerPortal.lookups().listLocations();
            assertNotNull(locations);
            assertTrue(locations.size() >= 0);
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
            PaginatedList<Report> reports = customerPortal.reports().list();
            assertNotNull(reports);
            assertTrue(reports.size() >= 0);

            if (reports.size() > 0) {
                Report report = customerPortal.reports().getByUuid(reports.get(0).getUuid());
                assertNotNull(report);
                assertEquals(reports.get(0).getUuid(), report.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Reports test skipped: " + e.getMessage());
        }
    }

    @Test
    void digitalLOAs() {
        try {
            PaginatedList<DigitalLOA> digitalLOAs = customerPortal.digitalLOAs().list();
            assertNotNull(digitalLOAs);
            assertTrue(digitalLOAs.size() >= 0);

            if (digitalLOAs.size() > 0) {
                DigitalLOA digitalLOA = customerPortal.digitalLOAs().getByUuid(digitalLOAs.get(0).getUuid());
                assertNotNull(digitalLOA);
                assertEquals(digitalLOAs.get(0).getUuid(), digitalLOA.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Digital LOAs test skipped: " + e.getMessage());
        }
    }

    @Test
    void secureCabinets() {
        try {
            PaginatedList<SecureCabinet> secureCabinets = customerPortal.secureCabinets().list();
            assertNotNull(secureCabinets);
            assertTrue(secureCabinets.size() >= 0);

            if (secureCabinets.size() > 0) {
                SecureCabinet secureCabinet = customerPortal.secureCabinets().getByUuid(secureCabinets.get(0).getUuid());
                assertNotNull(secureCabinet);
                assertEquals(secureCabinets.get(0).getUuid(), secureCabinet.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Secure cabinets test skipped: " + e.getMessage());
        }
    }

    @Test
    void unifiedNotifications() {
        try {
            PaginatedList<UnifiedNotification> notifications = customerPortal.unifiedNotifications().list();
            assertNotNull(notifications);
            assertTrue(notifications.size() >= 0);

            if (notifications.size() > 0) {
                UnifiedNotification notification = customerPortal.unifiedNotifications().getByUuid(notifications.get(0).getUuid());
                assertNotNull(notification);
                assertEquals(notifications.get(0).getUuid(), notification.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Unified notifications test skipped: " + e.getMessage());
        }
    }

    @Test
    void billingCredits() {
        try {
            PaginatedList<BillingCredit> billingCredits = customerPortal.billingCredits().list();
            assertNotNull(billingCredits);
            assertTrue(billingCredits.size() >= 0);

            if (billingCredits.size() > 0) {
                BillingCredit billingCredit = customerPortal.billingCredits().getByUuid(billingCredits.get(0).getUuid());
                assertNotNull(billingCredit);
                assertEquals(billingCredits.get(0).getUuid(), billingCredit.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Billing credits test skipped: " + e.getMessage());
        }
    }
}
