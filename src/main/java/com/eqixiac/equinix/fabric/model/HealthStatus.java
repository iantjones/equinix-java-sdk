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

package com.eqixiac.equinix.fabric.model;

import com.eqixiac.equinix.fabric.model.implementation.ApiServices;

/**
 * Health of the Fabric API (the Fabric v4 {@code HealthResponse} schema).
 */
public interface HealthStatus {

    String getHref();

    String getVersion();

    String getRelease();

    String getState();

    /**
     * The first per-route service status. The live service returns a list (see
     * {@link #getAllApiServices()}); this accessor keeps the original single-object signature.
     *
     * @return the first service status, or {@code null} when none is reported
     */
    ApiServices getApiServices();

    /**
     * Every per-route service status the health endpoint reports (the live response carries one per
     * Fabric API route).
     *
     * @return the service statuses, never {@code null}
     */
    java.util.List<ApiServices> getAllApiServices();
}
