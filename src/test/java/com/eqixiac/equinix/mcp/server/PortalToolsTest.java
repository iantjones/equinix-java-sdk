/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.client.Invoices;
import com.eqixiac.equinix.customerportal.client.OrderHistory;
import com.eqixiac.equinix.customerportal.enums.OrderHistoryStatus;
import com.eqixiac.equinix.customerportal.model.InvoiceSummary;
import com.eqixiac.equinix.customerportal.model.OrderHistoryItem;
import com.eqixiac.equinix.customerportal.model.json.creators.OrderHistorySearchRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("portal_* tool handlers (mocked CustomerPortal)")
class PortalToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CustomerPortal portal;
    private OrderHistory orderHistory;
    private Invoices invoices;
    private ServerContext context;

    @BeforeEach
    void stubPortal() {
        portal = mock(CustomerPortal.class);
        orderHistory = mock(OrderHistory.class);
        invoices = mock(Invoices.class);
        when(portal.orderHistory()).thenReturn(orderHistory);
        when(portal.invoices()).thenReturn(invoices);
        context = ServerContext.builder().customerPortal(portal).environment(Map.of()).build();
    }

    private static ToolRegistration tool(String name) {
        return EquinixMcpServer.catalog(EnumSet.of(Toolset.PORTAL)).stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private ObjectNode call(String name, String argsJson) throws Exception {
        return tool(name).getHandler().handle(MAPPER.readTree(argsJson), context);
    }

    private static OrderHistoryItem ticket(String orderNumber, OrderHistoryStatus status, String ibx) {
        OrderHistoryItem item = mock(OrderHistoryItem.class);
        when(item.getOrderNumber()).thenReturn(orderNumber);
        when(item.getOrderStatus()).thenReturn(status);
        when(item.getType()).thenReturn(List.of("Trouble Ticket"));
        when(item.getIbx()).thenReturn(List.of(ibx));
        when(item.getCreatedAt()).thenReturn("2026-07-19T09:00:00Z");
        when(item.getSubmittedDate()).thenReturn("2026-07-19");
        when(item.getCustomerReferenceNumbers()).thenReturn(List.of());
        return item;
    }

    @Test
    @DisplayName("portal_list_open_tickets filters to open trouble-ticket orders and summarizes")
    void listOpenTickets() throws Exception {
        doReturn(List.of(ticket("1-100", OrderHistoryStatus.IN_PROGRESS, "DC11"),
                ticket("1-101", OrderHistoryStatus.SUBMITTED, "LD8")))
                .when(orderHistory).search(any());

        ObjectNode payload = call("portal_list_open_tickets", "{}");

        assertEquals(2, payload.get("count").asInt());
        assertEquals("1-100", payload.get("tickets").get(0).get("order_number").asText());
        assertEquals("IN_PROGRESS", payload.get("tickets").get(0).get("status").asText());
        assertEquals("DC11", payload.get("tickets").get(0).get("ibxs").get(0).asText());
        assertTrue(payload.get("open_only").asBoolean());

        // The search request itself carries the trouble-ticket product type and the open statuses.
        ArgumentCaptor<OrderHistorySearchRequest> captor =
                ArgumentCaptor.forClass(OrderHistorySearchRequest.class);
        verify(orderHistory).search(captor.capture());
        JsonNode request = MAPPER.valueToTree(captor.getValue());
        assertEquals("TROUBLE_TICKET", request.get("filters").get("productTypes").get(0).asText());
        List<String> statuses = new ArrayList<>();
        request.get("filters").get("orderStatus").forEach(s -> statuses.add(s.asText()));
        assertTrue(statuses.containsAll(List.of("ENTERED", "SUBMITTED", "IN_PROGRESS", "PENDING_QA")),
                "open statuses requested: " + statuses);
        assertFalse(statuses.contains("CLOSED"), "closed tickets are excluded by default");
    }

    @Test
    @DisplayName("portal_list_open_tickets include_closed drops the status filter; limit caps the payload")
    void listTicketsIncludeClosedAndLimit() throws Exception {
        List<OrderHistoryItem> many = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            many.add(ticket("1-" + i, OrderHistoryStatus.CLOSED, "DC11"));
        }
        doReturn(many).when(orderHistory).search(any());

        ObjectNode payload = call("portal_list_open_tickets", "{\"include_closed\": true, \"limit\": 3}");

        assertEquals(3, payload.get("count").asInt(), "the limit caps the summarized list");
        assertTrue(payload.get("truncated").asBoolean());
        assertTrue(payload.get("truncation_note").asText().contains("3 of 10"));

        ArgumentCaptor<OrderHistorySearchRequest> captor =
                ArgumentCaptor.forClass(OrderHistorySearchRequest.class);
        verify(orderHistory).search(captor.capture());
        JsonNode request = MAPPER.valueToTree(captor.getValue());
        assertNull(request.get("filters").get("orderStatus"),
                "include_closed drops the status filter: " + request);
    }

    @Test
    @DisplayName("portal_get_billing_summary summarizes invoices and totals them by currency")
    void billingSummary() throws Exception {
        InvoiceSummary usd = invoice("INV-1", "150.00", "USD", LocalDate.of(2026, 7, 1));
        InvoiceSummary usd2 = invoice("INV-2", "50.00", "USD", LocalDate.of(2026, 6, 1));
        InvoiceSummary sgd = invoice("INV-3", "70.00", "SGD", LocalDate.of(2026, 7, 5));
        when(invoices.summaries()).thenReturn(
                new PaginatedList<>(List.of(usd, usd2, sgd), null, null, null, null));

        ObjectNode payload = call("portal_get_billing_summary", "{}");

        assertEquals(3, payload.get("invoice_count").asInt());
        assertEquals("INV-1", payload.get("invoices").get(0).get("transaction_id").asText());
        assertEquals(new BigDecimal("200.00"),
                payload.get("totals_by_currency").get("USD").decimalValue());
        assertEquals(new BigDecimal("70.00"),
                payload.get("totals_by_currency").get("SGD").decimalValue());
    }

    @Test
    @DisplayName("an SDK failure surfaces as an MCP tool error result, not an exception")
    void sdkFailureBecomesToolError() {
        when(orderHistory.search(any())).thenThrow(new RuntimeException("403 from the portal API"));

        McpSchema.CallToolResult result = McpToolAdapter
                .toSpecification(tool("portal_list_open_tickets"), context)
                .callHandler()
                .apply(null, McpSchema.CallToolRequest.builder("portal_list_open_tickets")
                        .arguments(Map.of()).build());

        assertTrue(Boolean.TRUE.equals(result.isError()));
        McpSchema.TextContent text = (McpSchema.TextContent) result.content().get(0);
        assertTrue(text.text().contains("portal_list_open_tickets failed:"), text.text());
        assertTrue(text.text().contains("403"), text.text());
    }

    private static InvoiceSummary invoice(String id, String total, String currency, LocalDate date) {
        InvoiceSummary summary = mock(InvoiceSummary.class);
        when(summary.getTransactionId()).thenReturn(id);
        when(summary.getTotalAmount()).thenReturn(new BigDecimal(total));
        when(summary.getTotalRecurringAmount()).thenReturn(new BigDecimal(total));
        when(summary.getTotalNonRecurringAmount()).thenReturn(BigDecimal.ZERO);
        when(summary.getCurrencyCode()).thenReturn(Currency.getInstance(currency));
        when(summary.getTransactionDate()).thenReturn(date);
        when(summary.getPaymentDueDate()).thenReturn(date.plusDays(30));
        when(summary.getBillingCycle()).thenReturn(date.getMonth().toString());
        return summary;
    }
}
