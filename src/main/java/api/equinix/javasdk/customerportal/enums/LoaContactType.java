package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

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
