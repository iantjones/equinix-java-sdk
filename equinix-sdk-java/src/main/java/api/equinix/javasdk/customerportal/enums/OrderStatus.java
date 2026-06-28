package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

public enum OrderStatus implements APIParam {
    RECEIVED,
    IN_PROGRESS,
    ON_HOLD,
    CLOSED,
    CANCELLED
}
