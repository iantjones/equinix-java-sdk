package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Status of a unified notification event (Customer Portal Unified Notifications v2 API; also covers the
 * Notifications v1 {@code notification-status} values). {@link #UNKNOWN} is a read-side fallback — never send it.
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
    DEPROVISIONED,
    UNKNOWN;

    @JsonCreator
    public static NotificationStatus fromString(String value) {
        try { return NotificationStatus.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
