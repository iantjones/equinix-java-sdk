package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.QuoteStatus;
import api.equinix.javasdk.customerportal.model.json.QuoteJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class QuoteDeserializationTest {

    private static ObjectMapper objectMapper;
    private static QuoteJson quote;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = QuoteDeserializationTest.class.getResourceAsStream("/json/customerportal/quote_response.json");
        assertNotNull(is, "quote_response.json fixture not found on classpath");
        quote = objectMapper.readValue(is, QuoteJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("d0e1f2a3-b4c5-4d6e-7f80-910213243546", quote.getUuid());
    }

    @Test
    void quoteNumber_isDeserialized() {
        assertEquals("QT-2024-0019873", quote.getQuoteNumber());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(QuoteStatus.PENDING, quote.getStatus());
    }

    @Test
    void totalAmount_isDeserialized() {
        assertEquals("24750.00", quote.getTotalAmount());
    }

    @Test
    void currency_isDeserialized() {
        assertEquals("USD", quote.getCurrency());
    }

    @Test
    void expirationDate_isDeserialized() {
        assertEquals("2025-01-15T23:59:59.000Z", quote.getExpirationDate());
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-11-10T16:30:00.000Z", quote.getCreatedDate());
    }
}
