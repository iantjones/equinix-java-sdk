package com.eqixiac.equinix.operations.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Type of a product covered by a Digital LOA (diLOA v1 API). Values the SDK does not know read as
 * {@link #UNKNOWN}.
 */
public enum LoaProductType implements APIParam {
    CROSS_CONNECT,
    /** Read-side fallback for a value this SDK version does not know; never sent. */
    UNKNOWN;

    /**
     * Maps a wire value to a constant, falling back to {@link #UNKNOWN}.
     *
     * @param value the wire value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static LoaProductType fromString(String value) {
        if (value != null) {
            for (LoaProductType v : values()) {
                if (v.name().equalsIgnoreCase(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
