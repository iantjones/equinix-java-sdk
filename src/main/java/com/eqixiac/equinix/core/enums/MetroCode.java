/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.core.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import java.util.Locale;
import java.util.Optional;

/**
 * Well-known Equinix metro codes.
 *
 * <p>{@link #UNKNOWN} is a forward-compatibility sentinel: a metro the API returns that this enum
 * does not yet list deserializes to {@code UNKNOWN} rather than failing the response (see
 * {@code MetroCodeDeserializer}). The authoritative, always-current set of metros is the Metros
 * API ({@code fabric.metros()}); this enum is a convenience for the well-known ones.</p>
 *
 * @author ianjones
 */
public enum MetroCode implements APIParam {
        AM,
        AT,
        BA,
        BG,
        BL,
        BO,
        BX,
        CA,
        CH,
        CL,
        CN,
        CU,
        DA,
        DB,
        DC,
        DE,
        DX,
        ED,
        FR,
        GV,
        HE,
        HH,
        HK,
        HO,
        IL,
        JH,
        JK,
        JN,
        KA,
        KL,
        LA,
        LD,
        LM,
        LS,
        MA,
        MB,
        MD,
        ME,
        MI,
        ML,
        MN,
        MO,
        MT,
        MU,
        MX,
        NY,
        OS,
        OT,
        PA,
        PE,
        PH,
        RJ,
        SE,
        SG,
        SJ,
        SK,
        SL,
        SO,
        SP,
        ST,
        SV,
        SY,
        TR,
        TY,
        VA,
        WA,
        WI,
        ZH,
        ZW,

        @JsonEnumDefaultValue
        UNKNOWN;

    /**
     * Resolves a wire metro code (e.g. {@code "SV"}) to its enum constant, if this enum lists it.
     * The lookup is case-insensitive and trims surrounding whitespace. The {@link #UNKNOWN} sentinel
     * is never returned as a match — a code that does not name a known metro yields
     * {@link Optional#empty()}.
     *
     * @param code the metro code as returned by the API
     * @return the matching {@link MetroCode}, or empty if {@code code} is null/blank or not a known metro
     */
    public static Optional<MetroCode> lookup(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        try {
            MetroCode metroCode = MetroCode.valueOf(normalized);
            return metroCode == UNKNOWN ? Optional.empty() : Optional.of(metroCode);
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Resolves a wire metro code to its enum constant, falling back to {@link #UNKNOWN} for a code
     * this enum does not list (so callers always get a non-null value). Use {@link #lookup(String)}
     * when you need to distinguish "unknown" from a real metro.
     *
     * @param code the metro code as returned by the API
     * @return the matching {@link MetroCode}, or {@link #UNKNOWN}
     */
    public static MetroCode fromCode(String code) {
        return lookup(code).orElse(UNKNOWN);
    }
}
