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

package com.eqixiac.equinix.internetaccess.enums;

/**
 * Sort property for {@code POST /internetAccess/v2/services/search} ({@code sort} query parameter).
 *
 * <p>The spec types {@code sort} as a free pattern ({@code /name,-/type}). These are the
 * properties the live service sorts by (observed 2026-11-11); others such as {@code /bandwidth}
 * and {@code /uuid} fail with HTTP 500 {@code EQ-7108000}.</p>
 */
public enum ServiceSearchSortProperty {
    /** {@code /name}. */
    NAME("/name"),
    /** {@code /state}. */
    STATE("/state"),
    /** {@code /type}. */
    TYPE("/type");

    private final String pointer;

    ServiceSearchSortProperty(String pointer) {
        this.pointer = pointer;
    }

    /**
     * Returns the JSON pointer sent on the wire.
     *
     * @return the property pointer, for example {@code /name}
     */
    public String getPointer() {
        return pointer;
    }
}
