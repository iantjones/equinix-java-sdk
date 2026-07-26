package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Status of a Digital LOA document (diLOA v1 API).
 */
public enum LoaState implements APIParam {
    READY_FOR_USE,
    LOCKED,
    USED,
    CANCELLED,
    EXPIRED,
    PENDING_REQUESTOR_ACCEPTANCE,
    PENDING_PROVIDER_AUTHORIZATION
}
