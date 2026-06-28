package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Status of a unified notification event (Customer Portal Unified Notifications v2 API).
 */
public enum NotificationStatus implements APIParam {
    CANCELLED,
    COMPLETED,
    EXPEDITE,
    EXPIRED_CANCELLED,
    EXTENDED,
    IN_PROGRESS,
    MODIFIED,
    NEW,
    OTHER,
    RECEIVED,
    REQUIRE_APPROVAL,
    RESCHEDULED,
    RESOLVED,
    SCHEDULED,
    SHIPMENT_NOTIFICATION,
    UPDATED,
    FAILED,
    PROVISIONED,
    DEPROVISIONED
}
