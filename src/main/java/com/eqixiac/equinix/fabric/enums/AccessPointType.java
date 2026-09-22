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
 * Values of the Fabric v4 {@code AccessPointType} schema, plus two constants the schema does not
 * list (see the table). Checked against the catalog fetched 2026-09-21.
 *
 * <table>
 *   <caption>Constants that differ from the {@code AccessPointType} schema</caption>
 *   <tr><th>Constant</th><th>Catalog status</th></tr>
 *   <tr><td>{@link #XF_IC}</td><td>In {@code AccessPointType}. Request and response shape
 *       unpublished; see the constant.</td></tr>
 *   <tr><td>{@link #CHAINGROUP}</td><td>Absent from {@code AccessPointType}; present in
 *       {@code VirtualConnectionPriceAccessPointType} (price search only).</td></tr>
 *   <tr><td>{@link #CX_PORT}</td><td>Absent from the catalog.</td></tr>
 * </table>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release. Never send
 * it.</p>
 *
 * @author ianjones
 */
public enum AccessPointType {
    VD,
    VG,
    SP,
    IGW,
    COLO,
    SUBNET,
    CLOUD_ROUTER,
    CHAINGROUP,
    NETWORK,
    METAL_NETWORK,
    VPIC_INTERFACE,
    APP_LINK,
    /**
     * Access point of type {@code XF_IC}. The catalog uses the same literal as the {@code type}
     * of the resource in its {@code InterconnectCreate}, {@code InterconnectResponse} and
     * {@code InterconnectSearchResponse} examples ({@code href}
     * {@code /fabric/v4/interconnects/{uuid}}, with a {@code router} of type
     * {@link CloudRouterType#IC_ROUTER}).
     *
     * <p><b>Beta</b>: the catalog publishes those examples but no path or schema for the
     * resource, and no connection-create example with an {@code XF_IC} access point. The fields a
     * create request needs for this type are unverified. Probe with
     * {@code ConnectionBuilder.dryRun()} before sending a live create.</p>
     */
    XF_IC,
    // Non-spec value retained for backward compatibility.
    CX_PORT,
    UNKNOWN;

    @JsonCreator
    public static AccessPointType fromString(String value) {
        try { return AccessPointType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}