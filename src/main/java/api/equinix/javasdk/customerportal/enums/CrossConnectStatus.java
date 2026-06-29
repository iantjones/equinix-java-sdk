package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

public enum CrossConnectStatus implements APIParam {
    ACTIVE,
    PENDING,
    PROVISIONING,
    DEPROVISIONED,
    CANCELLED
}
