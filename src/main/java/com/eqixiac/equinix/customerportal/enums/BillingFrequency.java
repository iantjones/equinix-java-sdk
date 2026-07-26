package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Frequency at which a billing account is billed (Billing v1 finance accounts API).
 */
public enum BillingFrequency implements APIParam {
    MONTHLY,
    QUARTERLY,
    ANNUALLY
}
