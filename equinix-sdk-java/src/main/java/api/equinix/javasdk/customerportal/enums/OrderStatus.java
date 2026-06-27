package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

public enum OrderStatus implements APIParam {
    SUBMITTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    PENDING_CUSTOMER
}
