package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Period type of a terms-of-use entry on an invoice line item (Billing v2 API; the Quotes v2
 * {@code termsOfUse_Details.period} adds {@code DAYS}).
 */
public enum TermsOfUsePeriod implements APIParam {
    MONTHS,
    DAYS
}
