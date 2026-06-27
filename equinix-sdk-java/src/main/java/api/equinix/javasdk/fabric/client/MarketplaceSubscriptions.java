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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.MarketplaceSubscription;

/**
 * Client interface for managing Equinix Fabric marketplace subscriptions. Marketplace
 * subscriptions represent active enrollments in provider-offered services.
 */
public interface MarketplaceSubscriptions {

    /**
     * Lists all marketplace subscriptions accessible to the current account.
     *
     * @return a paginated list of marketplace subscriptions
     */
    PaginatedList<MarketplaceSubscription> list();

    /**
     * Retrieves a single marketplace subscription by its unique identifier.
     *
     * @param uuid the unique identifier of the marketplace subscription
     * @return the marketplace subscription matching the given UUID
     */
    MarketplaceSubscription getByUuid(String uuid);
}
