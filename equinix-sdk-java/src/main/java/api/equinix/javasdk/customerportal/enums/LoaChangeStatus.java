package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Status of a Digital LOA change record (diLOA v1 API).
 */
public enum LoaChangeStatus implements APIParam {
    REQUESTED,
    COMPLETED,
    FAILED
}
