package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * File type of an invoice document (Billing v1 finance accounts API).
 */
public enum InvoiceDocumentFileType implements APIParam {
    PDF_DETAILS,
    EXCEL_DETAILS,
    PDF_SUMMARY,
    EXCEL_SUMMARY,
    INVOICE_PDF,
    BOLETO_PDF,
    NF_URL
}
