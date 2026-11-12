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

import com.eqixiac.equinix.core.enums.SortOrder;
import com.eqixiac.equinix.operations.enums.OrderHistorySortField;
import com.eqixiac.equinix.operations.model.InvoiceSummary;
import com.eqixiac.equinix.operations.model.OrderHistoryItem;
import com.eqixiac.equinix.operations.enums.OrderHistoryProductType;
import com.eqixiac.equinix.operations.enums.OrderHistoryStatus;
import com.eqixiac.equinix.operations.model.json.creators.OrderHistorySearchRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.mcp.server.Schemas.array;
import static com.eqixiac.equinix.mcp.server.Schemas.bool;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.looseObject;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;

/**
 * The {@code ops_*} tools: cross-domain reach into the data center operations APIs - open trouble
 * tickets and a billing summary - summarized for an agent rather than mirroring the
 * underlying endpoints.
 */
final class OperationsToolFactory {

    private static final int DEFAULT_TICKETS = 20;
    private static final int MAX_TICKETS = 50;
    private static final int MAX_INVOICES = 25;

    /** Order-history statuses treated as "open". */
    private static final List<OrderHistoryStatus> OPEN_STATUSES = List.of(OrderHistoryStatus.ENTERED,
            OrderHistoryStatus.SUBMITTED, OrderHistoryStatus.IN_PROGRESS, OrderHistoryStatus.PENDING_QA);

    private OperationsToolFactory() {
    }

    static List<ToolRegistration> tools() {
        return List.of(listOpenTickets(), getBillingSummary());
    }

    // ops_list_open_tickets

    private static ToolRegistration listOpenTickets() {
        return ToolRegistration.builder()
                .name("ops_list_open_tickets")
                .title("List open trouble tickets")
                .description("Lists the account's open trouble tickets from the order "
                        + "history (statuses: entered, submitted, in progress, pending QA), newest first, "
                        + "summarized to number, status, type, IBXs, and dates. Optionally filter by IBX. "
                        + "Set include_closed=true to include cancelled/closed tickets too.")
                .inputSchema(object(props(
                        "ibxs", array("Restrict to tickets raised for these IBX codes.",
                                string("IBX code, e.g. 'DC11'.")),
                        "include_closed", bool("Also include cancelled and closed tickets. Default false."),
                        "limit", integer("Maximum tickets to return (default " + DEFAULT_TICKETS
                                + ", max " + MAX_TICKETS + ")."))))
                .outputSchema(looseObject("The matching trouble tickets, summarized, with a count and a "
                        + "truncation note when capped."))
                .toolset(Toolset.OPS)
                .handler(OperationsToolFactory::handleListOpenTickets)
                .build();
    }

    private static ObjectNode handleListOpenTickets(JsonNode args, ServerContext ctx) {
        int limit = Args.limit(args, "limit", DEFAULT_TICKETS, MAX_TICKETS);
        boolean includeClosed = Args.optBool(args, "include_closed", false);
        List<String> ibxs = Args.stringList(args, "ibxs");

        // The order-history product type for trouble tickets is TROUBLE: the live service rejects
        // TROUBLE_TICKET with HTTP 400 (EQX-171-1212, checked 2026-11-11), matching the spec enum.
        OrderHistorySearchRequest.Filters.Builder filters = OrderHistorySearchRequest.Filters.builder()
                .productTypes(OrderHistoryProductType.TROUBLE);
        if (!includeClosed) {
            filters.orderStatus(OPEN_STATUSES);
        }
        if (!ibxs.isEmpty()) {
            filters.ibxs(ibxs);
        }
        OrderHistorySearchRequest request = OrderHistorySearchRequest.builder()
                .filters(filters.build())
                // "Newest first": the order-history spec's sort (orderhistoryv1 'sorts', name
                // CREATED_ON, direction DESC). Without it the service returns its own order.
                .sorts(List.of(new OrderHistorySearchRequest.Sort(OrderHistorySortField.CREATED_ON, SortOrder.DESC)))
                // One row more than shown, so a truncated result can be detected and reported.
                .page(new OrderHistorySearchRequest.PageRequest(0, limit + 1))
                .build();

        List<? extends OrderHistoryItem> tickets = ctx.operations().orderHistory().search(request);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        ArrayNode items = payload.putArray("tickets");
        int shown = 0;
        for (OrderHistoryItem ticket : tickets) {
            if (shown >= limit) {
                break;
            }
            ObjectNode t = items.addObject();
            // Every string below comes from the operations; free-text fields such as customer
            // references are customer-entered, so they are labelled as untrusted data.
            UntrustedText.put(t, "order_number", ticket.getOrderNumber());
            t.put("status", String.valueOf(ticket.getOrderStatus()));
            ArrayNode types = t.putArray("types");
            if (ticket.getType() != null) {
                ticket.getType().forEach(type -> UntrustedText.add(types, type));
            }
            ArrayNode ticketIbxs = t.putArray("ibxs");
            if (ticket.getIbx() != null) {
                ticket.getIbx().forEach(ibx -> UntrustedText.add(ticketIbxs, ibx));
            }
            t.put("created_at", ticket.getCreatedAt());
            t.put("submitted_date", ticket.getSubmittedDate());
            if (ticket.getCustomerReferenceNumbers() != null && !ticket.getCustomerReferenceNumbers().isEmpty()) {
                ArrayNode refs = t.putArray("customer_references");
                ticket.getCustomerReferenceNumbers().forEach(ref -> UntrustedText.add(refs, ref));
            }
            shown++;
        }
        payload.put("count", shown);
        payload.put("open_only", !includeClosed);
        if (tickets.size() > shown) {
            payload.put("truncated", true);
            payload.put("truncation_note", "Showing " + shown + " of " + tickets.size()
                    + " matching tickets; raise 'limit' (max " + MAX_TICKETS + ") or filter by 'ibxs'.");
        }
        return payload;
    }

    // ops_get_billing_summary

    private static ToolRegistration getBillingSummary() {
        return ToolRegistration.builder()
                .name("ops_get_billing_summary")
                .title("Get a billing summary")
                .description("Summarizes the account's recent invoices from the Billing API: per invoice "
                        + "the transaction id, type, dates, and amounts, plus totals grouped by currency. "
                        + "Reads the first page of invoice summaries (up to " + MAX_INVOICES + ").")
                .inputSchema(object(props()))
                .outputSchema(looseObject("Recent invoices with amounts and due dates, and totals by currency."))
                .toolset(Toolset.OPS)
                .handler(OperationsToolFactory::handleGetBillingSummary)
                .build();
    }

    private static ObjectNode handleGetBillingSummary(JsonNode args, ServerContext ctx) {
        Iterable<InvoiceSummary> summaries = ctx.operations().invoices().summaries();

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        ArrayNode invoices = payload.putArray("invoices");
        Map<String, BigDecimal> totalsByCurrency = new LinkedHashMap<>();
        int shown = 0;
        boolean truncated = false;
        for (InvoiceSummary summary : summaries) {
            if (shown >= MAX_INVOICES) {
                truncated = true;
                break;
            }
            ObjectNode inv = invoices.addObject();
            UntrustedText.put(inv, "transaction_id", summary.getTransactionId());
            inv.put("transaction_type", String.valueOf(summary.getTransactionType()));
            inv.put("transaction_date", String.valueOf(summary.getTransactionDate()));
            inv.put("payment_due_date", String.valueOf(summary.getPaymentDueDate()));
            String currency = summary.getCurrencyCode() == null
                    ? "unknown" : String.valueOf(summary.getCurrencyCode());
            inv.put("currency", currency);
            inv.put("total_amount", summary.getTotalAmount());
            inv.put("recurring_amount", summary.getTotalRecurringAmount());
            inv.put("non_recurring_amount", summary.getTotalNonRecurringAmount());
            UntrustedText.put(inv, "billing_cycle", summary.getBillingCycle());
            if (summary.getTotalAmount() != null) {
                totalsByCurrency.merge(currency, summary.getTotalAmount(), BigDecimal::add);
            }
            shown++;
        }
        payload.put("invoice_count", shown);
        ObjectNode totals = payload.putObject("totals_by_currency");
        totalsByCurrency.forEach(totals::put);
        if (truncated) {
            payload.put("truncated", true);
            payload.put("truncation_note",
                    "Showing the first " + MAX_INVOICES + " invoice summaries; more exist.");
        }
        return payload;
    }
}
