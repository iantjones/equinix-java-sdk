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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator;

/**
 * Client interface for managing Equinix Fabric service tokens. Service tokens allow third
 * parties to create connections to your assets without requiring direct account access.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ServiceTokens {

    /**
     * Lists all service tokens accessible to the current account.
     *
     * @return a paginated list of service tokens
     */
    PaginatedList<ServiceToken> list();

    /**
     * Retrieves a single service token by its unique identifier.
     *
     * @param uuid the unique identifier of the service token
     * @return the service token matching the given UUID
     */
    ServiceToken getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new service token.
     * Call methods on the returned builder to configure the token, then call {@code create()}.
     *
     * @param issuerSide the side (A-side or Z-side) issuing the service token
     * @return a builder for configuring the new service token
     */
    ServiceTokenOperator.ServiceTokenBuilder define(Side issuerSide);
}
