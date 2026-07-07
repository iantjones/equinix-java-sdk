package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.customerportal.enums.OrderLineRequestType;
import api.equinix.javasdk.customerportal.enums.UnitOfMeasure;
import api.equinix.javasdk.customerportal.enums.OrderProductType;
import api.equinix.javasdk.customerportal.enums.PricingChargeType;
import api.equinix.javasdk.customerportal.enums.PricingValueType;
import api.equinix.javasdk.customerportal.enums.TermsOfUseType;
import api.equinix.javasdk.customerportal.enums.TermsOfUsePeriod;
import api.equinix.javasdk.customerportal.enums.QuoteContactType;
import api.equinix.javasdk.customerportal.enums.QuoteRequestType;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.QuoteStatus;
import api.equinix.javasdk.customerportal.model.implementation.QuoteContact;
import api.equinix.javasdk.customerportal.model.implementation.QuoteDetail;
import api.equinix.javasdk.customerportal.model.implementation.QuotePricing;
import api.equinix.javasdk.customerportal.model.json.QuoteJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class QuoteDeserializationTest {

    private static ObjectMapper objectMapper;
    private static QuoteJson quote;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.mapper();
        InputStream is = QuoteDeserializationTest.class.getResourceAsStream("/json/customerportal/quote_response.json");
        assertNotNull(is, "quote_response.json fixture not found on classpath");
        quote = objectMapper.readValue(is, QuoteJson.class);
    }

    @Test
    void quoteId_isDeserialized() {
        assertEquals("1-1234567891011", quote.getQuoteId());
    }

    @Test
    void accountFields_areDeserialized() {
        assertEquals("AAA Corporation Ltd", quote.getAccountName());
        assertEquals("123456", quote.getAccountNumber());
    }

    @Test
    void quoteRequestType_isDeserialized() {
        assertEquals(QuoteRequestType.NEW, quote.getQuoteRequestType());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(QuoteStatus.SUBMITTED, quote.getStatus());
    }

    @Test
    void dateTimes_areDeserialized() {
        assertEquals("2020-11-19T06:51:10.000Z", quote.getCreatedDateTime());
        assertEquals("2020-12-31T06:51:10.000Z", quote.getExpirationDateTime());
    }

    @Test
    void currencyCode_isString() {
        assertEquals("USD", quote.getCurrencyCode());
    }

    @Test
    void contacts_areTyped() {
        assertNotNull(quote.getContacts());
        assertEquals(1, quote.getContacts().size());
        QuoteContact contact = quote.getContacts().get(0);
        assertEquals("john_doe", contact.getRegisteredUser());
        assertEquals(QuoteContactType.QUOTATION, contact.getType());
        assertEquals(2, contact.getDetails().size());
        assertEquals("EMAIL", contact.getDetails().get(0).getType());
    }

    @Test
    void termsOfUse_areTyped() {
        assertNotNull(quote.getTermsOfUse());
        assertEquals(1, quote.getTermsOfUse().size());
        assertEquals(0, new BigDecimal("12").compareTo(quote.getTermsOfUse().get(0).getValue()));
        assertEquals(TermsOfUsePeriod.MONTHS, quote.getTermsOfUse().get(0).getPeriod());
        assertEquals(TermsOfUseType.INITIAL_TERM, quote.getTermsOfUse().get(0).getType());
    }

    @Test
    void totalPricing_isTyped() {
        assertNotNull(quote.getTotalPricing());
        QuotePricing pricing = quote.getTotalPricing().get(0);
        assertEquals(0, new BigDecimal("100").compareTo(pricing.getValue()));
        assertEquals(PricingValueType.ABSOLUTE, pricing.getValueType());
        assertEquals(PricingChargeType.MONTHLY_CHARGE, pricing.getType());
    }

    @Test
    void details_areTyped() {
        assertNotNull(quote.getDetails());
        assertEquals(1, quote.getDetails().size());
        QuoteDetail detail = quote.getDetails().get(0);
        assertEquals("1-NEYSNX123", detail.getLineId());
        assertEquals(OrderProductType.SMART_HANDS, detail.getProductType());
        assertEquals("PS00002.PROD", detail.getProductCode());
        assertEquals(UnitOfMeasure.HRS, detail.getUnitOfMeasure());
        assertEquals(OrderLineRequestType.ADD, detail.getRequestType());
        assertEquals(1, detail.getUnitPricing().size());
        assertEquals(PricingChargeType.ONE_TIME_CHARGE, detail.getTotalPricing().get(0).getType());
        assertEquals("ASSET_ID", detail.getAdditionalInfo().get(0).getKey());
    }
}
