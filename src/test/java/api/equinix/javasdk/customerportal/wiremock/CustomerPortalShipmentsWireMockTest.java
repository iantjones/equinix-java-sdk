package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.ShipmentCarrier;
import api.equinix.javasdk.customerportal.enums.ShipmentTransportType;
import api.equinix.javasdk.customerportal.enums.SmartHandsContactType;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.PendingStorageOrderResponse;
import api.equinix.javasdk.customerportal.model.ShipmentLocation;
import api.equinix.javasdk.customerportal.model.ShipmentOrderResponse;
import api.equinix.javasdk.customerportal.model.json.creators.ContactInfo;
import api.equinix.javasdk.customerportal.model.json.creators.IbxLocation;
import api.equinix.javasdk.customerportal.model.json.creators.InboundShipmentDetails;
import api.equinix.javasdk.customerportal.model.json.creators.InboundShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.InboundShipmentServiceDetails;
import api.equinix.javasdk.customerportal.model.json.creators.OutboundShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.OutboundShipmentServiceDetails;
import api.equinix.javasdk.customerportal.model.json.creators.PendingStorageOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentUpdateRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Shipment orders.
 *
 * <p>Exercises the v2 order surface — {@code order(...)} (POST
 * {@code /colocations/v2/orders/shipments}) and {@code update(...)} (PATCH
 * {@code .../{orderId}}), both returning the {@code Location}-header order id — and the v1
 * surface: the typed inbound/outbound/pending-storage submissions (POST
 * {@code /v1/orders/shipment/inbound|outbound|pendingStorage}, order number in the response
 * body) and the permitted-locations GET ({@code /v1/orders/shipment/locations}). Cancellation is
 * via {@code orders().cancel(orderId, reason)}.</p>
 */
class CustomerPortalShipmentsWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    @BeforeAll
    static void setUp() {
        customerPortal = new CustomerPortal(testCredentials());
        redirectToWireMock(customerPortal);
        customerPortal.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (customerPortal != null) customerPortal.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("order()")
    class Order {

        @Test
        @DisplayName("POSTs the shipment and returns the Location-header order id")
        void placesOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/colocations/v2/orders/shipments"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Location", "/orders/1-55667788")));

            OrderResponse response = customerPortal.shipments().order(
                    ShipmentOrderRequest.builder("INBOUND", "2025-02-01T10:00:00Z", "SV5:01:000ABC",
                                    InboundShipmentDetails.builder(ShipmentCarrier.FEDEX, List.of("123ABC"), 4).build())
                            .accountNumber("128745")
                            .description("Server delivery")
                            .build());

            assertNotNull(response);
            assertEquals("1-55667788", response.getOrderId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/colocations/v2/orders/shipments"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("INBOUND")))
                    .withRequestBody(matchingJsonPath("$.cageId", equalTo("SV5:01:000ABC")))
                    .withRequestBody(matchingJsonPath("$.details.carrier", equalTo("FEDEX")))
                    .withRequestBody(matchingJsonPath("$.details.numberOfBoxes", equalTo("4"))));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("PATCHes the shipment by id and returns the order id")
        void updatesOrder() {
            wireMock.stubFor(patch(urlPathEqualTo("/colocations/v2/orders/shipments/1-55667788"))
                    .willReturn(aResponse().withStatus(202)
                            .withHeader("Location", "/orders/1-55667788")));

            OrderResponse response = customerPortal.shipments().update("1-55667788",
                    ShipmentUpdateRequest.builder().requestedDateTime("2025-02-03T10:00:00Z").build());

            assertNotNull(response);
            assertEquals("1-55667788", response.getOrderId());

            wireMock.verify(patchRequestedFor(urlPathEqualTo("/colocations/v2/orders/shipments/1-55667788")));
        }
    }

    @Nested
    @DisplayName("orderInbound()")
    class OrderInbound {

        @Test
        @DisplayName("POSTs /v1/orders/shipment/inbound and returns the body order number")
        void placesInboundOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/orders/shipment/inbound"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"orderNumber\":\"1-128726682522\",\"OrderReferenceId\":\"REF-001\","
                                    + "\"Id\":\"ORD-001\",\"SRNumber\":\"SR-001\"}")));

            InboundShipmentOrderRequest request = InboundShipmentOrderRequest.builder(
                            new IbxLocation("LD8", List.of(new IbxLocation.Cage("LD8:02:02MC2T", "111"))),
                            List.of(ContactInfo.registered(SmartHandsContactType.ORDERING, "jondoe@test.com"),
                                    ContactInfo.registered(SmartHandsContactType.NOTIFICATION, "jondoe@test.com")),
                            InboundShipmentServiceDetails.builder("2025-02-15T05:00:00.420-08:00",
                                            new InboundShipmentServiceDetails.ShipmentDetails(10, ShipmentTransportType.CARRIER)
                                                    .carrierName(ShipmentCarrier.FEDEX)
                                                    .trackingNumber(List.of("1235467869"))
                                                    .isOverSized(true))
                                    .deliverToCage(true)
                                    .inboundRequestDescription("Server delivery")
                                    .build())
                    .customerReferenceNumber("RQT0036422")
                    .build();

            ShipmentOrderResponse response = customerPortal.shipments().orderInbound(request);

            assertNotNull(response);
            assertEquals("1-128726682522", response.getOrderNumber());
            assertEquals("REF-001", response.getOrderReferenceId());
            assertEquals("ORD-001", response.getId());
            assertEquals("SR-001", response.getSrNumber());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/orders/shipment/inbound"))
                    .withRequestBody(matchingJsonPath("$.ibxLocation.ibx", equalTo("LD8")))
                    .withRequestBody(matchingJsonPath("$.ibxLocation.cages[0].cage", equalTo("LD8:02:02MC2T")))
                    .withRequestBody(matchingJsonPath("$.ibxLocation.cages[0].accountNumber", equalTo("111")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].contactType", equalTo("ORDERING")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.estimatedDateTime", equalTo("2025-02-15T05:00:00.420-08:00")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.deliverToCage", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.noOfBoxes", equalTo("10")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.inboundType", equalTo("CARRIER")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.carrierName", equalTo("FEDEX")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.trackingNumber[0]", equalTo("1235467869")))
                    .withRequestBody(matchingJsonPath("$.customerReferenceNumber", equalTo("RQT0036422"))));
        }
    }

    @Nested
    @DisplayName("orderOutbound()")
    class OrderOutbound {

        @Test
        @DisplayName("POSTs /v1/orders/shipment/outbound and returns the body order number")
        void placesOutboundOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/orders/shipment/outbound"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"orderNumber\":\"1-128726682521\"}")));

            OutboundShipmentOrderRequest request = OutboundShipmentOrderRequest.builder(
                            new IbxLocation("AM1", List.of(new IbxLocation.Cage("AM1:0G:00EQ11-1", "108812"))),
                            List.of(ContactInfo.registered(SmartHandsContactType.ORDERING, "jondoe@test.com")),
                            OutboundShipmentServiceDetails.builder("2025-03-28T19:09:30.211Z",
                                            new OutboundShipmentServiceDetails.ShipmentDetails(ShipmentTransportType.CARRIER)
                                                    .carrierName(ShipmentCarrier.DHL)
                                                    .noOfBoxes(3)
                                                    .trackingNumber("33,333")
                                                    .declaredValue("3")
                                                    .labelExists(false)
                                                    .shipToAddress(new OutboundShipmentServiceDetails.ShipToAddress(
                                                            "test", "1188 test address", "Sunnyvale", "CALIFORNIA",
                                                            "US", "94085", "+1 1331313", "111")))
                                    .build())
                    .build();

            ShipmentOrderResponse response = customerPortal.shipments().orderOutbound(request);

            assertNotNull(response);
            assertEquals("1-128726682521", response.getOrderNumber());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/orders/shipment/outbound"))
                    .withRequestBody(matchingJsonPath("$.ibxLocation.ibx", equalTo("AM1")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.estimatedDateTime", equalTo("2025-03-28T19:09:30.211Z")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.outboundType", equalTo("CARRIER")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.carrierName", equalTo("DHL")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.noOfBoxes", equalTo("3")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.labelExists", equalTo("false")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.shipToAddress.city", equalTo("Sunnyvale")))
                    .withRequestBody(matchingJsonPath("$.serviceDetails.shipmentDetails.shipToAddress.carrierAccountNumber", equalTo("111"))));
        }
    }

    @Nested
    @DisplayName("orderPendingStorage()")
    class OrderPendingStorage {

        @Test
        @DisplayName("POSTs /v1/orders/shipment/pendingStorage and returns one result per stored shipment")
        void placesPendingStorageOrder() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/orders/shipment/pendingStorage"))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"orderNumber\":\"1-108124419634\",\"storageID\":\"1-275PQCAS\","
                                    + "\"Ibx\":\"CH1\",\"cage\":\"CH1:05:313\",\"accountNumber\":\"133333\","
                                    + "\"trackingNumber\":\"TRK-9001\"}]")));

            PendingStorageOrderRequest request = new PendingStorageOrderRequest(
                    List.of(new PendingStorageOrderRequest.Detail("1-275PQCAS", "CH1:05:313", "133333", true)
                            .additionalDetails("Deliver to cage rear")),
                    List.of(ContactInfo.registered(SmartHandsContactType.ORDERING, "jondoe@test.com"),
                            ContactInfo.registered(SmartHandsContactType.NOTIFICATION, "jondoe@test.com")));

            List<? extends PendingStorageOrderResponse> responses =
                    customerPortal.shipments().orderPendingStorage(request);

            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals("1-108124419634", responses.get(0).getOrderNumber());
            assertEquals("1-275PQCAS", responses.get(0).getStorageId());
            assertEquals("CH1", responses.get(0).getIbx());
            assertEquals("CH1:05:313", responses.get(0).getCage());
            assertEquals("133333", responses.get(0).getAccountNumber());
            assertEquals("TRK-9001", responses.get(0).getTrackingNumber());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/orders/shipment/pendingStorage"))
                    .withRequestBody(matchingJsonPath("$.shipmentDetails[0].storageId", equalTo("1-275PQCAS")))
                    .withRequestBody(matchingJsonPath("$.shipmentDetails[0].cage", equalTo("CH1:05:313")))
                    .withRequestBody(matchingJsonPath("$.shipmentDetails[0].accountNumber", equalTo("133333")))
                    .withRequestBody(matchingJsonPath("$.shipmentDetails[0].deliverToCage", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.contacts[0].contactType", equalTo("ORDERING"))));
        }
    }

    @Nested
    @DisplayName("listLocations()")
    class ListLocations {

        @Test
        @DisplayName("GETs /v1/orders/shipment/locations and returns the permitted locations")
        void listsLocations() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/shipment/locations"))
                    .willReturn(okJson("{\"locations\":[{\"ibx\":\"AM1\",\"cages\":[{\"cage\":\"AM1:02:002MC1\","
                            + "\"cageTypes\":[\"Shared\"],\"accounts\":[{\"number\":\"1111\","
                            + "\"name\":\"ABC Network Services\",\"isCreditHold\":false,\"isPOBearing\":true,"
                            + "\"cabinets\":[{\"cabinet\":\"AM1:02:002MC1:0601\",\"cabinetType\":\"Shared\"}]}]}]}]}")));

            List<? extends ShipmentLocation> locations = customerPortal.shipments().listLocations();

            assertNotNull(locations);
            assertEquals(1, locations.size());
            assertEquals("AM1", locations.get(0).getIbx());
            assertEquals("AM1:02:002MC1", locations.get(0).getCages().get(0).getCage());
            assertEquals(List.of("Shared"), locations.get(0).getCages().get(0).getCageTypes());
            assertEquals("1111", locations.get(0).getCages().get(0).getAccounts().get(0).getNumber());
            assertEquals("ABC Network Services", locations.get(0).getCages().get(0).getAccounts().get(0).getName());
            assertEquals(true, locations.get(0).getCages().get(0).getAccounts().get(0).getIsPOBearing());
            assertEquals("AM1:02:002MC1:0601",
                    locations.get(0).getCages().get(0).getAccounts().get(0).getCabinets().get(0).getCabinet());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/shipment/locations"))
                    .withQueryParam("detail", absent())
                    .withQueryParam("ibxs", absent())
                    .withQueryParam("cages", absent()));
        }

        @Test
        @DisplayName("forwards detail/ibxs/cages query params")
        void forwardsQueryParams() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/orders/shipment/locations"))
                    .willReturn(okJson("{\"locations\":[{\"ibx\":\"AM1\"}]}")));

            List<? extends ShipmentLocation> locations = customerPortal.shipments()
                    .listLocations(true, "AM1,AM2", "AM1:02:002MC1");

            assertNotNull(locations);
            assertEquals("AM1", locations.get(0).getIbx());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/orders/shipment/locations"))
                    .withQueryParam("detail", equalTo("true"))
                    .withQueryParam("ibxs", equalTo("AM1,AM2"))
                    .withQueryParam("cages", equalTo("AM1:02:002MC1")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/colocations/v2/orders/shipments",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.shipments().order(
                            ShipmentOrderRequest.builder("INBOUND", "2025-02-01T10:00:00Z", "SV5:01:000ABC",
                                    Map.of("numberOfBoxes", 1)).build()));
        }
    }
}
