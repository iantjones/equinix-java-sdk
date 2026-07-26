package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Status of a billing account as returned by the Platform Billing Account v2 (BAS) API.
 */
public enum BillingAccountStatus implements APIParam {
    ACTIVE,
    PENDING_SIGNATURE,
    CREDIT_HOLD,
    IN_PROGRESS,
    DECLINED
}
