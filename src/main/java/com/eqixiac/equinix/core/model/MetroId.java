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

package com.eqixiac.equinix.core.model;

import com.eqixiac.equinix.core.enums.MetroCode;

import java.util.Objects;
import java.util.Optional;

/**
 * A forward-compatible identifier for an Equinix metro: a normalized metro code string that can name
 * <em>any</em> metro, including ones added after this SDK was built and therefore absent from the
 * {@link MetroCode} enum. Use it where a metro must be referenced by code without being constrained
 * to the well-known set — e.g. keys in {@code MetroRegistry}, or provisioning against a brand-new
 * metro.
 *
 * <p>The code is normalized to upper case with surrounding whitespace trimmed. Two {@code MetroId}s
 * are equal when their normalized codes match. {@link #asMetroCode()} bridges back to the enum when
 * the code is one of the well-known metros.</p>
 *
 * @author ianjones
 */
public final class MetroId {

    private final String code;

    private MetroId(String code) {
        this.code = code;
    }

    /**
     * Creates a {@code MetroId} from a metro code string (e.g. {@code "SV"}, {@code "da"}).
     *
     * @param code the metro code; normalized to upper case and trimmed
     * @return the metro id
     * @throws NullPointerException if {@code code} is null
     * @throws IllegalArgumentException if {@code code} is blank
     */
    public static MetroId of(String code) {
        Objects.requireNonNull(code, "code");
        String normalized = code.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("metro code must not be blank");
        }
        return new MetroId(normalized);
    }

    /**
     * Creates a {@code MetroId} from a well-known {@link MetroCode}.
     *
     * @param metroCode the metro code enum constant
     * @return the metro id
     * @throws NullPointerException if {@code metroCode} is null
     */
    public static MetroId of(MetroCode metroCode) {
        Objects.requireNonNull(metroCode, "metroCode");
        return new MetroId(metroCode.name());
    }

    /**
     * @return the normalized metro code string (e.g. {@code "SV"})
     */
    public String code() {
        return code;
    }

    /**
     * @return the matching {@link MetroCode} enum constant, or empty if this code is not one of the
     *         well-known metros listed by {@link MetroCode}
     */
    public Optional<MetroCode> asMetroCode() {
        return MetroCode.lookup(code);
    }

    /**
     * @return {@code true} if this code names a metro present in the {@link MetroCode} enum
     */
    public boolean isKnown() {
        return asMetroCode().isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MetroId)) {
            return false;
        }
        return code.equals(((MetroId) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }
}
