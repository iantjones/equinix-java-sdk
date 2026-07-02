package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Type of a unified notification event (Customer Portal Unified Notifications v2 API; also covers the
 * Notifications v1 {@code ibx-notification-type} / {@code network-notification-type} values, which add
 * {@code IBX_SMARTVIEW}). {@link #UNKNOWN} is a read-side fallback — never send it.
 */
public enum NotificationType implements APIParam {
    ORDERS,
    SMART_HANDS_SUPPORT_PLAN,
    NETWORK_SPECIFICATION,
    NETWORK_INCIDENT,
    NETWORK_MAINTENANCE,
    VIRTUAL_ASSET_UPDATES,
    IBX_MAINTENANCE,
    IBX_INCIDENT,
    IBX_ADVISORY,
    IBX_SECURITY_INCIDENT,
    IBX_SMARTVIEW,
    MANAGED_SERVICES_INCIDENT,
    MANAGED_SERVICES_IBX_MAINTENANCE,
    MANAGED_SERVICES_NETWORK_MAINTENANCE,
    UNKNOWN;

    @JsonCreator
    public static NotificationType fromString(String value) {
        try { return NotificationType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
