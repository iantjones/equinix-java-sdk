package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Type of an invoice document (Billing v1 finance accounts API).
 */
public enum InvoiceType implements APIParam {
    INVOICE,
    MANUAL_INVOICE,
    CREDIT_MEMO,
    STATEMENT
}
