package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Type of a Digital LOA change record (diLOA v1 API).
 */
public enum LoaChangeType implements APIParam {
    LOA_CREATION,
    LOA_UPDATE,
    LOA_AUTHORIZATION,
    LOA_CANCELLATION,
    LOA_PATCHING,
    LOA_LOCK_ACQUISITION,
    LOA_LOCK_RELEASE,
    LOA_USAGE,
    LOA_ACCEPTANCE,
    LOA_UNAUTHORIZATION,
    LOA_EXPIRATION,
    TYPE_UNKNOWN
}
