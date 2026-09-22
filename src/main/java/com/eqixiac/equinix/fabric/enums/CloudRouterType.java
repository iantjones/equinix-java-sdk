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
 * Cloud Router {@code type} values in the Fabric v4 catalog (fetched 2026-09-21).
 *
 * <table>
 *   <caption>Where each value appears in the catalog</caption>
 *   <tr><th>Constant</th><th>Catalog schemas</th></tr>
 *   <tr><td>{@link #XF_ROUTER}</td><td>{@code CloudRouterPostRequestBase.type} (the only value a
 *       create request accepts), {@code CloudRouterReadResponse.type}, and the router reference
 *       schemas.</td></tr>
 *   <tr><td>{@link #IC_ROUTER}</td><td>{@code CloudRouterReadResponse.type} only.</td></tr>
 * </table>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release. Never send
 * it.</p>
 */
public enum CloudRouterType {
    XF_ROUTER,
    /**
     * Read-only router type. It is absent from {@code CloudRouterPostRequestBase.type}, so a
     * create request with this value is outside the published contract. The catalog's
     * {@code InterconnectResponse} example shows a router of this type attached to an
     * {@code XF_IC} resource.
     *
     * <p><b>Beta</b>: the {@code XF_IC} resource has examples but no published path or schema.</p>
     */
    IC_ROUTER,
    UNKNOWN;

    @JsonCreator
    public static CloudRouterType fromString(String value) {
        try { return CloudRouterType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
