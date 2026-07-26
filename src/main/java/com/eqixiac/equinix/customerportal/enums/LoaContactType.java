package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Notification category relevant to a Digital LOA contact (diLOA v1 API).
 */
public enum LoaContactType implements APIParam {
    NOTIFICATION,
    TECHNICAL,
    ORDERING,
    RESELLING,
    BILLING
}
