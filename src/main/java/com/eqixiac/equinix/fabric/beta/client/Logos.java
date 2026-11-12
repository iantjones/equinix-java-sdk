/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.client;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.model.LogoResponse;

/**
 * Company-profile logos (3 operations).
 *
 * <p>Obtain it from {@code fabric.beta().logos()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface Logos {

    /**
     * Create Logo: {@code POST /fabric/v4/logos} (operation {@code createLogo}).
     * <p>Create Logo for Equinix Fabric™ Company Profile</p>
     * <p>Sent as {@code multipart/form-data} with the {@code logo} file part and the
     * {@code name}/{@code type}/{@code description} text parts.</p>
     *
     * @param logo the image bytes (spec: required, binary)
     * @param fileName the file name advertised for the {@code logo} part
     * @param name the logo name (spec: required)
     * @param type the logo type (spec: required)
     * @param description the logo description (spec: required)
     * @return the {@code LogoResponse} the API returns
     */
    LogoResponse create(byte[] logo, String fileName, String name, String type, String description);

    /**
     * Get Logo: {@code GET /fabric/v4/logos/{uuid}} (operation {@code getLogoByUuid}).
     * <p>Get Logo by UUID</p>
     * <p>The spec types the response as {@code multipart/mixed}; the raw body bytes are returned.</p>
     *
     * @param uuid the {@code uuid} path parameter
     * @return the raw response body
     */
    byte[] download(String uuid);

    /**
     * Delete Logo: {@code DELETE /fabric/v4/logos/{uuid}} (operation {@code deleteLogoByUuid}).
     * <p>Delete Logo by UUID</p>
     *
     * @param uuid the {@code uuid} path parameter
     * @return the {@code LogoResponse} the API returns
     */
    LogoResponse delete(String uuid);
}
