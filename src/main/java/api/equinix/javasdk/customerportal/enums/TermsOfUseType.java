package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Type of a terms-of-use period on an invoice line item (Billing v2 API; the Quotes v2
 * {@code termsOfUse_Details.type} adds {@code NON_RENEWAL_NOTICE}).
 */
public enum TermsOfUseType implements APIParam {
    INITIAL_TERM,
    RENEWAL_TERM,
    NON_RENEWAL_NOTICE
}
