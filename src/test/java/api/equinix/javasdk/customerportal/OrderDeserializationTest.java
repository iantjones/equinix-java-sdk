package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.OrderStatus;
import api.equinix.javasdk.customerportal.enums.OrderType;
import api.equinix.javasdk.customerportal.model.json.OrderJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

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
    void uuid_isDeserialized() {
        assertEquals("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e", order.getUuid());
    }

    @Test
    void href_isDeserialized() {
        assertEquals("/v1/orders/b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e", order.getHref());
    }

    @Test
    void orderNumber_isDeserialized() {
        assertEquals("ORD-2024-0078542", order.getOrderNumber());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(OrderType.NEW_CROSS_CONNECT, order.getType());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
    }

    @Test
    void description_isDeserialized() {
        assertEquals("New cabinet installation in SV5 with 10kW power allocation", order.getDescription());
    }

    @Test
    void accountNumber_isDeserialized() {
        assertEquals("128745", order.getAccountNumber());
    }

    @Test
    void ibxCode_isDeserialized() {
        assertEquals("SV5", order.getIbxCode());
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-10-15T08:30:00.000Z", order.getCreatedDate());
    }
}
