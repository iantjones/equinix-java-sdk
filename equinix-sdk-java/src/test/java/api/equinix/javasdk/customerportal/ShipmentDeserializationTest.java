package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.CarrierType;
import api.equinix.javasdk.customerportal.enums.ShipmentStatus;
import api.equinix.javasdk.customerportal.enums.ShipmentType;
import api.equinix.javasdk.customerportal.model.json.ShipmentJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentDeserializationTest {

    private static ObjectMapper objectMapper;
    private static ShipmentJson shipment;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = ShipmentDeserializationTest.class.getResourceAsStream("/json/customerportal/shipment_response.json");
        assertNotNull(is, "shipment_response.json fixture not found on classpath");
        shipment = objectMapper.readValue(is, ShipmentJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f809102", shipment.getUuid());
    }

    @Test
    void href_isDeserialized() {
        assertEquals("/v1/shipments/f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f809102", shipment.getHref());
    }

    @Test
    void shipmentId_isDeserialized() {
        assertEquals("SHIP-2024-0005647", shipment.getShipmentId());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(ShipmentType.INBOUND, shipment.getType());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
    }

    @Test
    void carrierType_isDeserialized() {
        assertEquals(CarrierType.FEDEX, shipment.getCarrierType());
    }

    @Test
    void trackingNumber_isDeserialized() {
        assertEquals("794644790132", shipment.getTrackingNumber());
    }

    @Test
    void ibxCode_isDeserialized() {
        assertEquals("SV5", shipment.getIbxCode());
    }

    @Test
    void accountNumber_isDeserialized() {
        assertEquals("128745", shipment.getAccountNumber());
    }

    @Test
    void numberOfBoxes_isDeserialized() {
        assertEquals(4, shipment.getNumberOfBoxes());
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-11-13T15:20:00.000Z", shipment.getCreatedDate());
    }
}
