package com.eqixiac.equinix.operations.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Billing v2 {@code GET /v2/invoices}: all four filters are optional in the spec, so every
 * combination of set/unset filters must build without error and send only what was set.
 */
class InvoiceRequestBuilderTest {

    /** Bit 0 start, bit 1 end, bit 2 account, bit 3 transaction: all 16 combinations. */
    @ParameterizedTest(name = "filter mask {0}")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    void everyCombinationBuilds(int mask) {
        RequestBuilder.Invoice b = RequestBuilder.Invoice.builder();
        if ((mask & 1) != 0) b.withStart(LocalDate.of(2024, 1, 1));
        if ((mask & 2) != 0) b.withEnd(LocalDate.of(2024, 12, 31));
        if ((mask & 4) != 0) b.withAccountNumber("128745");
        if ((mask & 8) != 0) b.withTransactionId("INV-1");

        Map<String, List<String>> q = b.build().getQueryParameters();

        assertTrue(b.wasBuilt());
        assertEquals((mask & 1) != 0 ? List.of("2024-01-01") : null, q.get("startDate"));
        assertEquals((mask & 2) != 0 ? List.of("2024-12-31") : null, q.get("endDate"));
        assertEquals((mask & 4) != 0 ? List.of("128745") : null, q.get("accountNumbers"));
        assertEquals((mask & 8) != 0 ? List.of("INV-1") : null, q.get("transactionIds"));
        assertEquals(Integer.bitCount(mask), q.size());
    }

    @Test
    @DisplayName("several account numbers / transaction ids are comma-joined (spec: form, explode=false)")
    void listsAreCommaJoined() {
        Map<String, List<String>> q = RequestBuilder.Invoice.builder()
                .withAccountNumber("1").withAccountNumber("2")
                .withTransactionId("A").withTransactionId("B").withTransactionId(null)
                .build().getQueryParameters();
        assertEquals(List.of("1,2"), q.get("accountNumbers"));
        assertEquals(List.of("A,B"), q.get("transactionIds"));
    }
}
