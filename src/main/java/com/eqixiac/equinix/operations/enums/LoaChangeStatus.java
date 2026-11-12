package com.eqixiac.equinix.operations.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Status of a Digital LOA change record (diLOA v1 API).
 */
public enum LoaChangeStatus implements APIParam {
    REQUESTED,
    COMPLETED,
    FAILED
}
