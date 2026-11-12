package com.eqixiac.equinix.operations.model;

import com.eqixiac.equinix.core.enums.SortOrder;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.operations.enums.OrderHistoryDateRange;
import com.eqixiac.equinix.operations.enums.OrderHistoryProductType;
import com.eqixiac.equinix.operations.enums.OrderHistorySortField;
import com.eqixiac.equinix.operations.enums.OrderHistoryStatus;
import com.eqixiac.equinix.operations.model.json.OrderHistorySearchResponseJson;
import com.eqixiac.equinix.operations.model.json.creators.OrderHistorySearchRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Typed order-history search request and the {links, content, page} envelope. */
class OrderHistorySearchRequestTest {

    @Test
    void serializesTypedEnumsAsWireValues() {
        OrderHistorySearchRequest request = OrderHistorySearchRequest.builder()
                .filters(OrderHistorySearchRequest.Filters.builder()
                        .productTypes(OrderHistoryProductType.TROUBLE, OrderHistoryProductType.OUTBOUND_SHIPMENT)
                        .orderStatus(OrderHistoryStatus.PENDING_CUSTOMER_INPUT)
                        .dateRange(OrderHistoryDateRange.PAST_7_DAYS)
                        .toDate(LocalDate.of(2026, 11, 11))
                        .build())
                .sorts(java.util.List.of(new OrderHistorySearchRequest.Sort(OrderHistorySortField.ACCOUNT_NAME, SortOrder.ASC)))
                .build();

        JsonNode json = Constants.mapper().valueToTree(request);

        assertEquals("TROUBLE", json.at("/filters/productTypes/0").asText());
        assertEquals("OUTBOUND_SHIPMENT", json.at("/filters/productTypes/1").asText());
        assertEquals("PENDING_CUSTOMER_INPUT", json.at("/filters/orderStatus/0").asText());
        assertEquals("PAST_7_DAYS", json.at("/filters/dateRange").asText());
        assertEquals("2026-11-11", json.at("/filters/toDate").asText());
        assertEquals("ACCOUNT_NAME", json.at("/sorts/0/name").asText());
        assertEquals("ASC", json.at("/sorts/0/direction").asText());
    }

    @Test
    void rejectsUnknownFallbackValues() {
        var filters = OrderHistorySearchRequest.Filters.builder();
        assertThrows(IllegalArgumentException.class, () -> filters.productTypes(OrderHistoryProductType.UNKNOWN));
        assertThrows(IllegalArgumentException.class, () -> filters.orderStatus(OrderHistoryStatus.UNKNOWN));
        assertThrows(IllegalArgumentException.class, () -> filters.dateRange(OrderHistoryDateRange.UNKNOWN));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderHistorySearchRequest.Sort(OrderHistorySortField.UNKNOWN, SortOrder.ASC));
    }

    @Test
    void specOnlyShipmentValueReadsAsUnknown() {
        // The spec's SHIPMENT is not in the live enum (2026-11-11).
        assertEquals(OrderHistoryProductType.UNKNOWN, OrderHistoryProductType.fromString("SHIPMENT"));
        assertEquals(OrderHistoryStatus.PII_HOLD, OrderHistoryStatus.fromString("PII_HOLD"));
    }

    @Test
    void pageEnvelopeAndHasNext() throws Exception {
        OrderHistoryPage page = Constants.mapper().readValue(
                "{\"links\":[],\"content\":[{\"orderNumber\":\"1-1\"}],"
                        + "\"page\":{\"size\":1,\"totalElements\":3,\"totalPages\":3,\"number\":1}}",
                OrderHistorySearchResponseJson.class);
        assertEquals(1, page.getContent().size());
        assertEquals(3L, page.getTotalElements());
        assertEquals(1, page.getNumber());
        assertTrue(page.hasNext());

        OrderHistoryPage last = Constants.mapper().readValue(
                "{\"content\":[],\"page\":{\"size\":1,\"totalElements\":3,\"totalPages\":3,\"number\":2}}",
                OrderHistorySearchResponseJson.class);
        assertFalse(last.hasNext());
        assertTrue(last.getLinks().isEmpty());
    }
}
