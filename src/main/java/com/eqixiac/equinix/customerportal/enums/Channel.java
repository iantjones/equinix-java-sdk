package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Channel used to place an order (Orders v2 / Billing v2 APIs). {@link #UNKNOWN} is a read-side
 * fallback for values added after this SDK release — never send it.
 */
public enum Channel implements APIParam {
    API,
    PORTAL,
    MOBILE,
    OFFLINE,
    OTHER,
    UNKNOWN;

    @JsonCreator
    public static Channel fromString(String value) {
        try { return Channel.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
