package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.Channel;
import api.equinix.javasdk.customerportal.enums.ContactAvailability;
import api.equinix.javasdk.customerportal.enums.OrderContactType;
import api.equinix.javasdk.customerportal.enums.OrderLineRequestType;
import api.equinix.javasdk.customerportal.enums.OrderNoteType;
import api.equinix.javasdk.customerportal.enums.OrderProductType;
import api.equinix.javasdk.customerportal.enums.PricingChargeType;
import api.equinix.javasdk.customerportal.enums.PurchaseOrderType;
import api.equinix.javasdk.customerportal.enums.QuoteRequestType;
import api.equinix.javasdk.customerportal.enums.SubChannel;
import api.equinix.javasdk.customerportal.enums.OrderStatus;
import api.equinix.javasdk.customerportal.model.implementation.OrderContactInfo;
import api.equinix.javasdk.customerportal.model.implementation.OrderLine;
import api.equinix.javasdk.customerportal.model.implementation.OrderNote;
import api.equinix.javasdk.customerportal.model.json.OrderJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderDeserializationTest {

    private static ObjectMapper objectMapper;
    private static OrderJson order;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = OrderDeserializationTest.class.getResourceAsStream("/json/customerportal/order_response.json");
        assertNotNull(is, "order_response.json fixture not found on classpath");
        order = objectMapper.readValue(is, OrderJson.class);
    }

    @Test
    void orderId_isDeserialized() {
        assertEquals("1-23232322", order.getOrderId());
    }

    @Test
    void accountFields_areDeserialized() {
        assertEquals("AAA Corporation Ltd", order.getAccountName());
        assertEquals("128745", order.getAccountNumber());
    }

    @Test
    void quoteRequestType_isDeserialized() {
        assertEquals(QuoteRequestType.NEW, order.getQuoteRequestType());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
    }

    @Test
    void dateTimes_areDeserialized() {
        assertEquals("2020-08-25T08:30:00.000Z", order.getCreatedDateTime());
        assertEquals("2020-09-15T00:00:00.000Z", order.getEstimatedCompletionDateTime());
    }

    @Test
    void currencyAndChannel_areDeserialized() {
        assertEquals("USD", order.getCurrencyCode());
        assertEquals(Channel.PORTAL, order.getChannel());
        assertEquals(SubChannel.ECP, order.getSubChannel());
    }

    @Test
    void flags_areDeserialized() {
        assertTrue(order.getCancellable());
        assertFalse(order.getModifiable());
    }

    @Test
    void customerReferenceId_isDeserialized() {
        assertEquals("CREF-ORD-20200825", order.getCustomerReferenceId());
    }

    @Test
    void contacts_areTyped() {
        assertNotNull(order.getContacts());
        OrderContactInfo contact = order.getContacts().get(0);
        assertEquals("john_doe", contact.getRegisteredUser());
        assertEquals(OrderContactType.ORDERING, contact.getType());
        assertEquals(ContactAvailability.ANYTIME, contact.getAvailability());
        assertEquals("EMAIL", contact.getDetails().get(0).getType());
    }

    @Test
    void notes_areTyped() {
        assertNotNull(order.getNotes());
        OrderNote note = order.getNotes().get(0);
        assertEquals("1-NOTE-001", note.getId());
        assertEquals(OrderNoteType.CUSTOMER_NOTES, note.getType());
        assertEquals("johndoe@acme.com", note.getAuthor());
        assertEquals("error-log", note.getAttachments().get(0).getName());
    }

    @Test
    void additionalInfo_isTyped() {
        assertNotNull(order.getAdditionalInfo());
        assertEquals("ASSET_ID", order.getAdditionalInfo().get(0).getKey());
    }

    @Test
    void details_areTyped() {
        assertNotNull(order.getDetails());
        assertEquals(1, order.getDetails().size());
        OrderLine line = order.getDetails().get(0);
        assertEquals("1-NEYSNX123", line.getLineId());
        assertEquals(OrderProductType.CROSS_CONNECT, line.getProductType());
        assertEquals("CC00001", line.getProductCode());
        assertEquals("SV5", line.getIbx());
        assertEquals("SV5:01:0101", line.getCage());
        assertEquals("SV5:01:0101:01", line.getCabinets().get(0));
        assertEquals(OrderLineRequestType.ADD, line.getRequestType());
        assertEquals(0, new BigDecimal("500").compareTo(line.getUnitPricing().get(0).getValue()));
        assertEquals(PricingChargeType.MONTHLY_CHARGE, line.getTotalPricing().get(0).getType());
        assertEquals(PurchaseOrderType.NEW, line.getPurchaseOrder().getType());
        assertEquals("PO-2020-0042", line.getPurchaseOrder().getNumber());
        assertEquals("floor-plan", line.getAttachments().get(0).getName());
        assertEquals("SERIAL_NUMBER", line.getAdditionalInfo().get(0).getKey());
    }
}
