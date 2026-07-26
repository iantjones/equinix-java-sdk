package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Means of contact for a Digital LOA contact detail (diLOA v1 API).
 */
public enum ContactDetailType implements APIParam {
    EMAIL,
    PHONE,
    MOBILE,
    SECONDARY_EMAIL
}
