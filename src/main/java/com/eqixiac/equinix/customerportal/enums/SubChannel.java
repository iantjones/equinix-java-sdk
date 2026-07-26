package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Sub-channel used to place an order (Orders v2 / Billing v2 APIs — the union of both specs'
 * values). {@link #UNKNOWN} is a read-side fallback for values added after this SDK release —
 * never send it.
 */
public enum SubChannel implements APIParam {
    QUOTE,
    IX,
    ECX,
    ECP,
    CSC,
    NE,
    EMG,
    NRC_CHARGE_UPLOAD,
    UNKNOWN;

    @JsonCreator
    public static SubChannel fromString(String value) {
        try { return SubChannel.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
