package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Category of a unified notification event (Customer Portal Unified Notifications v2 API).
 */
public enum NotificationCategory implements APIParam {
    GENERAL,
    INCIDENT,
    MAINTENANCE,
    ADVISORY
}
