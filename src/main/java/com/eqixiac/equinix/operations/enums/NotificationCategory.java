package com.eqixiac.equinix.operations.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Category of a unified notification event (Unified Notifications v2 API).
 */
public enum NotificationCategory implements APIParam {
    GENERAL,
    INCIDENT,
    MAINTENANCE,
    ADVISORY
}
