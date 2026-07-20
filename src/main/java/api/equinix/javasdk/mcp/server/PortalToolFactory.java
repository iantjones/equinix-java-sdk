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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.OrderHistoryItem;
import api.equinix.javasdk.customerportal.model.json.creators.OrderHistorySearchRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.mcp.server.Schemas.array;
import static api.equinix.javasdk.mcp.server.Schemas.bool;
import static api.equinix.javasdk.mcp.server.Schemas.integer;
import static api.equinix.javasdk.mcp.server.Schemas.looseObject;
import static api.equinix.javasdk.mcp.server.Schemas.object;
import static api.equinix.javasdk.mcp.server.Schemas.props;
import static api.equinix.javasdk.mcp.server.Schemas.string;

/**
 * The {@code portal_*} tools: cross-domain reach into the Customer Portal — open trouble
 * tickets and a billing summary — summarized for an agent rather than mirroring the
 * underlying endpoints.
 */
final class PortalToolFactory {

    private static final int DEFAULT_TICKETS = 20;
    private static final int MAX_TICKETS = 50;
    private static final int MAX_INVOICES = 25;

    /** Order-history statuses treated as "open". */
    private static final List<String> OPEN_STATUSES =
            List.of("ENTERED", "SUBMITTED", "IN_PROGRESS", "PENDING_QA");

    private PortalToolFactory() {
    }

    static List<ToolRegistration> tools() {
        return List.of(listOpenTickets(), getBillingSummary());
    }

    // ── portal_list_open_tickets ────────────────────────────────────────────

    private static ToolRegistration listOpenTickets() {
        return ToolRegistration.builder()
                .name("portal_list_open_tickets")
                .title("List open trouble tickets")
                .description("Lists the account's open trouble tickets from the Customer Portal order "
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
                .toolset(Toolset.PORTAL)
                .handler(PortalToolFactory::handleListOpenTickets)
                .build();
    }

    private static ObjectNode handleListOpenTickets(JsonNode args, ServerContext ctx) {
        int limit = Args.limit(args, "limit", DEFAULT_TICKETS, MAX_TICKETS);
        boolean includeClosed = Args.optBool(args, "include_closed", false);
        List<String> ibxs = Args.stringList(args, "ibxs");

        OrderHistorySearchRequest.Filters.Builder filters = OrderHistorySearchRequest.Filters.builder()
                .productTypes(List.of("TROUBLE_TICKET"));
        if (!includeClosed) {
            filters.orderStatus(new ArrayList<>(OPEN_STATUSES));
        }
        if (!ibxs.isEmpty()) {
            filters.ibxs(ibxs);
        }
        OrderHistorySearchRequest request = OrderHistorySearchRequest.builder()
                .filters(filters.build())
                .page(new OrderHistorySearchRequest.PageRequest(0, limit))
                .build();

        List<? extends OrderHistoryItem> tickets = ctx.customerPortal().orderHistory().search(request);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        ArrayNode items = payload.putArray("tickets");
        int shown = 0;
        for (OrderHistoryItem ticket : tickets) {
            if (shown >= limit) {
                break;
            }
            ObjectNode t = items.addObject();
            t.put("order_number", ticket.getOrderNumber());
            t.put("status", String.valueOf(ticket.getOrderStatus()));
            ArrayNode types = t.putArray("types");
            if (ticket.getType() != null) {
                ticket.getType().forEach(types::add);
            }
            ArrayNode ticketIbxs = t.putArray("ibxs");
            if (ticket.getIbx() != null) {
                ticket.getIbx().forEach(ticketIbxs::add);
            }
            t.put("created_at", ticket.getCreatedAt());
            t.put("submitted_date", ticket.getSubmittedDate());
            if (ticket.getCustomerReferenceNumbers() != null && !ticket.getCustomerReferenceNumbers().isEmpty()) {
                ArrayNode refs = t.putArray("customer_references");
                ticket.getCustomerReferenceNumbers().forEach(refs::add);
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

    // ── portal_get_billing_summary ──────────────────────────────────────────

    private static ToolRegistration getBillingSummary() {
        return ToolRegistration.builder()
                .name("portal_get_billing_summary")
                .title("Get a billing summary")
                .description("Summarizes the account's recent invoices from the Customer Portal: per invoice "
                        + "the transaction id, type, dates, and amounts, plus totals grouped by currency. "
                        + "Reads the first page of invoice summaries (up to " + MAX_INVOICES + ").")
                .inputSchema(object(props()))
                .outputSchema(looseObject("Recent invoices with amounts and due dates, and totals by currency."))
                .toolset(Toolset.PORTAL)
                .handler(PortalToolFactory::handleGetBillingSummary)
                .build();
    }

    private static ObjectNode handleGetBillingSummary(JsonNode args, ServerContext ctx) {
        Iterable<InvoiceSummary> summaries = ctx.customerPortal().invoices().summaries();

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
            inv.put("transaction_id", summary.getTransactionId());
            inv.put("transaction_type", String.valueOf(summary.getTransactionType()));
            inv.put("transaction_date", String.valueOf(summary.getTransactionDate()));
            inv.put("payment_due_date", String.valueOf(summary.getPaymentDueDate()));
            String currency = summary.getCurrencyCode() == null
                    ? "unknown" : String.valueOf(summary.getCurrencyCode());
            inv.put("currency", currency);
            inv.put("total_amount", summary.getTotalAmount());
            inv.put("recurring_amount", summary.getTotalRecurringAmount());
            inv.put("non_recurring_amount", summary.getTotalNonRecurringAmount());
            inv.put("billing_cycle", summary.getBillingCycle());
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
