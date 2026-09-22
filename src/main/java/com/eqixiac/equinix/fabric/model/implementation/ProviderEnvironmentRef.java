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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.core.model.ResourceRef;
import lombok.NoArgsConstructor;

/**
 * Write-side reference to a provider environment: the {@code uuid} property of the Fabric v4
 * {@code ProviderEnvironment} schema and nothing else. Serializes as {@code {"uuid": "..."}} in
 * {@code AccessPoint.environment} of a connection-create request. The read model is
 * {@link ProviderEnvironment}.
 *
 * <p><b>Beta</b>: {@code ProviderEnvironment} carries the catalog's Beta marker (catalog fetched
 * 2026-09-21). The catalog publishes no connection-create example that sets
 * {@code accessPoint.environment}, so the reference-by-uuid form is unverified against the
 * service.</p>
 */
@NoArgsConstructor
public class ProviderEnvironmentRef extends ResourceRef {

    public ProviderEnvironmentRef(String uuid) {
        super(uuid);
    }
}
