package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Status of a payment on a billing account (Billing v1 finance accounts API).
 */
public enum PaymentStatus implements APIParam {
    APPLIED,
    UNAPPLIED
}
