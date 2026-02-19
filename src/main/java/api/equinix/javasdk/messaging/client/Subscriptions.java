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

package api.equinix.javasdk.messaging.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.messaging.model.Subscription;
import api.equinix.javasdk.messaging.model.json.creators.SubscriptionOperator;

/**
 * Client interface for managing Equinix Messaging subscriptions. Provides methods to
 * list, retrieve, and create subscriptions that define which event types and resources
 * the account receives notifications for.
 */
public interface Subscriptions {

    /**
     * Lists all messaging subscriptions for the current account.
     *
     * @return a paginated list of subscriptions
     */
    PaginatedList<Subscription> list();

    /**
     * Retrieves a specific subscription by its unique identifier.
     *
     * @param uuid the unique identifier of the subscription
     * @return the subscription
     */
    Subscription getByUuid(String uuid);

    /**
     * Returns a builder for defining and creating a new messaging subscription.
     *
     * @return a subscription builder
     */
    SubscriptionOperator.SubscriptionBuilder define();
}
