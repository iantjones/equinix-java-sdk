package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Format in which an account's invoices are produced (Billing v1 finance accounts API).
 */
public enum InvoiceFormat implements APIParam {
    PDF,
    EXCEL
}
