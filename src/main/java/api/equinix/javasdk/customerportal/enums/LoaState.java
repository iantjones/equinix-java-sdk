package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

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
