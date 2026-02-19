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

package api.equinix.javasdk.internetaccess.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.RoutingConfig;
import api.equinix.javasdk.internetaccess.model.json.creators.RoutingConfigOperator;

/**
 * Client interface for managing Equinix Internet Access routing configurations. Provides
 * methods to list, retrieve, and create routing configurations that define BGP sessions
 * and route policies for internet access services.
 */
public interface RoutingConfigs {

    /**
     * Lists all routing configurations for the current account.
     *
     * @return a paginated list of routing configurations
     */
    PaginatedList<RoutingConfig> list();

    /**
     * Retrieves a specific routing configuration by its unique identifier.
     *
     * @param uuid the unique identifier of the routing configuration
     * @return the routing configuration
     */
    RoutingConfig getByUuid(String uuid);

    /**
     * Returns a builder for defining and creating a new routing configuration.
     *
     * @return a routing configuration builder
     */
    RoutingConfigOperator.RoutingConfigBuilder define();
}
