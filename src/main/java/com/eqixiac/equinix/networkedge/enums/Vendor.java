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

package com.eqixiac.equinix.networkedge.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author ianjones
 */
public enum Vendor {
    CISCO("Cisco"),
    JUNIPER_NETWORKS("Juniper Networks"),
    PALO_ALTO_NETWORKS("Palo Alto Networks"),
    CLOUDGENIX("CloudGenix"),
    FORTINET("Fortinet"),
    VERSA_NETWORKS("VERSA Networks"),
    VMWWARE("VMWare"),
    SILVER_PEAK("Silver Peak"),
    CHECK_POINT("Check Point"),
    ARUBA("Aruba"),
    ARISTA("Arista"),
    F5("F5"),
    BLUECAT("BlueCat"),
    ZSCALER("Zscaler"),
    AVIATRIX("Aviatrix"),
    UNKNOWN("Unknown");

    private final String formatted;

    Vendor(String formatted) {
        this.formatted = formatted;
    }

    @JsonValue
    public String getJsonValue() {
        return formatted;
    }

    /**
     * <p>The API returns two different wire forms for a vendor: the display form used by
     * {@code VirtualDeviceType.vendor} (e.g. "Palo Alto Networks") and the uppercase code form used
     * by {@code VirtualDeviceDetailsResponse.deviceTypeVendor} (e.g. "PALO_ALTO_NETWORKS"). Both are
     * accepted here; unrecognised values map to {@link #UNKNOWN}.</p>
     */
    @JsonCreator
    public static Vendor fromString(String value) {
        if (value == null) {
            return null;
        }
        for (Vendor vendor : values()) {
            if (vendor.name().equalsIgnoreCase(value) || vendor.formatted.equalsIgnoreCase(value)) {
                return vendor;
            }
        }
        // Spec code forms whose constant name differs.
        switch (value) {
            case "JUNIPER": return JUNIPER_NETWORKS;
            case "VMWARE": return VMWWARE;
            default: return UNKNOWN;
        }
    }
}
