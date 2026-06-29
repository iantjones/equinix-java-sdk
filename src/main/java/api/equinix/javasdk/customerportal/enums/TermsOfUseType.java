package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Type of a terms-of-use period on an invoice line item (Billing v2 API).
 */
public enum TermsOfUseType implements APIParam {
    INITIAL_TERM,
    RENEWAL_TERM
}
