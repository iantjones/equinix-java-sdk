package com.eqixiac.equinix.operations.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Cage or cabinet type in a Lookup v2 location ({@code cage_details.type}: {@code SHARED},
 * {@code PRIVATE}; {@code cage_details.cabinetType}: {@code PRIVATE}, {@code DEMARCATION}).
 */
public enum CageCabType {
    PRIVATE,
    SHARED,
    DEMARCATION,
    /** Read-side fallback for a value this SDK version does not know; never sent. */
    UNKNOWN;

    /**
     * Maps a wire value to a constant, falling back to {@link #UNKNOWN}.
     *
     * @param value the wire value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static CageCabType fromString(String value) {
        if (value != null) {
            for (CageCabType v : values()) {
                if (v.name().equalsIgnoreCase(value)) {
                    return v;
                }
            }
        }
        return UNKNOWN;
    }
}
