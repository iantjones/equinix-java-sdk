package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

public enum OrderStatus implements APIParam {
    RECEIVED,
    IN_PROGRESS,
    ON_HOLD,
    CLOSED,
    CANCELLED
}
