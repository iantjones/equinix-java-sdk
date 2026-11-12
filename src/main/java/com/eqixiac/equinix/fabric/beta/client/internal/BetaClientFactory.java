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
package com.eqixiac.equinix.fabric.beta.client.internal;

import com.eqixiac.equinix.core.client.Config;
import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.fabric.beta.Beta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and caches one {@link BetaResourceClient} per (resource group, model type) pair.
 */
@Beta
public class BetaClientFactory {

    private final Config config;

    private final Map<String, BetaResourceClient<?>> clients = new ConcurrentHashMap<>();

    /**
     * Creates the factory.
     *
     * @param equinixClient the core client
     */
    public BetaClientFactory(EquinixClient equinixClient) {
        this.config = new Config(equinixClient);
    }

    /**
     * Returns the cached client for a resource group and model type, creating it on first use.
     *
     * @param requestParent the apiParams resource group
     * @param type the JSON model class
     * @param <J> the JSON model type
     * @return the client
     */
    @SuppressWarnings("unchecked")
    public <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return (BetaResourceClient<J>) clients.computeIfAbsent(requestParent + "#" + type.getName(),
                k -> new BetaResourceClient<>(config, requestParent, type));
    }
}
