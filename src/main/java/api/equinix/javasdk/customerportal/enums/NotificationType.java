package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Type of a unified notification event (Customer Portal Unified Notifications v2 API).
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
    MANAGED_SERVICES_INCIDENT,
    MANAGED_SERVICES_IBX_MAINTENANCE,
    MANAGED_SERVICES_NETWORK_MAINTENANCE
}
