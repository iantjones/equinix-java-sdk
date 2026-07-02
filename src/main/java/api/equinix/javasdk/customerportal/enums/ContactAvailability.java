package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Availability of a contact person (diLOA v1 + Orders v2 APIs). {@link #UNKNOWN} is a read-side
 * fallback for values added after this SDK release — never send it.
 */
public enum ContactAvailability implements APIParam {
    WORK_HOURS,
    ANYTIME,
    UNKNOWN;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static ContactAvailability fromString(String value) {
        try { return ContactAvailability.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
