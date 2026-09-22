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

package com.eqixiac.equinix.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Values of the Fabric v4 {@code ConnectionType} schema, plus two constants the schema does not
 * list (see the table). Checked against the catalog fetched 2026-09-21.
 *
 * <table>
 *   <caption>Constants that differ from the {@code ConnectionType} schema</caption>
 *   <tr><th>Constant</th><th>Catalog status</th></tr>
 *   <tr><td>{@link #GW_VC}, {@link #IPX_VC}</td><td>In {@code ConnectionType}. The catalog gives
 *       no description and no create example for either value.</td></tr>
 *   <tr><td>{@link #VD_CHAIN_VC}</td><td>Absent from {@code ConnectionType}; present in
 *       {@code VirtualConnectionPriceConnectionType} (price search only).</td></tr>
 *   <tr><td>{@link #IC_VC}</td><td>Absent from the catalog.</td></tr>
 * </table>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release. Never send
 * it.</p>
 *
 * @author ianjones
 */
public enum ConnectionType {
    EVPL_VC,
    EPL_VC,
    EC_VC,
    IP_VC,
    ACCESS_EPL_VC,
    EIA_VC,
    EVPLAN_VC,
    EPLAN_VC,
    EVPTREE_VC,
    EPTREE_VC,
    IPWAN_VC,
    IA_VC,
    MC_VC,
    IX_VC,
    GW_VC,
    IPX_VC,
    // Not in the catalog's ConnectionType schema; retained for backward compatibility.
    IC_VC,
    VD_CHAIN_VC,
    UNKNOWN;

    @JsonCreator
    public static ConnectionType fromString(String value) {
        try { return ConnectionType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}