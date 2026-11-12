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

package com.eqixiac.equinix.fabric.model.implementation.cloud;

/** Argument checks shared by the built-in cloud adapters. */
final class AdapterArguments {

    private AdapterArguments() {
    }

    /**
     * Requires the provider metro. Fabric rejects a service-provider z-side without
     * {@code location.metroCode} (HTTP 400 EQ-3142501 "Invalid seller metroCode location",
     * live-confirmed 2026-11-11).
     *
     * @param metroCode the metro code
     * @return the metro code
     */
    static String requireMetro(String metroCode) {
        if (metroCode == null || metroCode.isBlank()) {
            throw new IllegalArgumentException("metroCode is required: Fabric rejects a service-provider "
                    + "z-side without location.metroCode (HTTP 400 EQ-3142501)");
        }
        return metroCode;
    }
}
