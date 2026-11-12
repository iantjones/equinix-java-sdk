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
 * Network Edge device vendor ({@code VirtualDeviceType.vendor}, display form, and
 * {@code VirtualDeviceDetailsResponse.deviceTypeVendor}, code form).
 *
 * <p>Values are the spec's {@code deviceTypeVendor} enum plus the vendors the live
 * {@code GET /ne/v1/deviceTypes} returns (observed 2026-11-11): 6WIND, Infoblox, Netskope, Thales
 * and VyOS, and F5 as {@code "F5 Networks"}. Several products are device types of an existing
 * vendor, not vendors: Meraki vMX ({@code MERAKI-VMX-GA}) is Cisco, NGINX ({@code NGINX}) is F5
 * Networks, VeloCloud SD-WAN ({@code VMware-SDWAN}) is Arista, Prisma SD-WAN and Prisma AIRS
 * ({@code PRISMA-SDWAN}, {@code PA-VM-AIRS}) are Palo Alto Networks, and CloudGuard
 * ({@code CGUARD}) is Check Point. Unrecognised values read as {@link #UNKNOWN}.</p>
 *
 * <p>The device type code ({@code deviceTypeCode}) stays a {@code String}: new codes appear without
 * notice and some are not mnemonic.</p>
 */
public enum Vendor {
    CISCO("Cisco"),
    JUNIPER_NETWORKS("Juniper Networks"),
    PALO_ALTO_NETWORKS("Palo Alto Networks"),
    CLOUDGENIX("CloudGenix"),
    FORTINET("Fortinet"),
    VERSA_NETWORKS("VERSA Networks"),
    VMWARE("VMWare"),
    SILVER_PEAK("Silver Peak"),
    CHECK_POINT("Check Point"),
    ARUBA("Aruba"),
    ARISTA("Arista"),
    /** Live display form {@code "F5 Networks"}; spec code {@code F5}. */
    F5("F5 Networks"),
    BLUECAT("BlueCat"),
    ZSCALER("Zscaler"),
    AVIATRIX("Aviatrix"),
    /** Live only (2026-11-11). */
    SIX_WIND("6WIND"),
    /** Live only (2026-11-11). */
    INFOBLOX("Infoblox"),
    /** Live only (2026-11-11). */
    NETSKOPE("Netskope"),
    /** Live only (2026-11-11). */
    THALES("Thales"),
    /** Live only (2026-11-11). */
    VYOS("VyOS"),
    /** Read-side fallback for a vendor this SDK version does not know. */
    UNKNOWN("Unknown");

    private final String formatted;

    Vendor(String formatted) {
        this.formatted = formatted;
    }

    /**
     * Returns the display form sent on the wire.
     *
     * @return the display form, for example {@code "Palo Alto Networks"}
     */
    @JsonValue
    public String getJsonValue() {
        return formatted;
    }

    /**
     * Maps a wire value to a vendor.
     *
     * <p>The API returns two different wire forms for a vendor: the display form used by
     * {@code VirtualDeviceType.vendor} (e.g. "Palo Alto Networks") and the uppercase code form used
     * by {@code VirtualDeviceDetailsResponse.deviceTypeVendor} (e.g. "PALO_ALTO_NETWORKS"). Both are
     * accepted here; unrecognised values map to {@link #UNKNOWN}.</p>
     *
     * @param value the wire value
     * @return the vendor, {@link #UNKNOWN} when unrecognised, or {@code null} for {@code null}
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
            case "6WIND": return SIX_WIND;
            default: return UNKNOWN;
        }
    }
}
