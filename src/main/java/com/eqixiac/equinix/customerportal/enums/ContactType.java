package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

public enum ContactType implements APIParam {
    PHONE,
    EMAIL,
    MOBILE,
    /** Read-side fallback for values added after this SDK release — never send it. */
    UNKNOWN;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static ContactType fromString(String value) {
        try { return ContactType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
