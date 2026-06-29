package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Format in which an account's invoices are produced (Billing v1 finance accounts API).
 */
public enum InvoiceFormat implements APIParam {
    PDF,
    EXCEL
}
