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
import api.equinix.javasdk.fabric.model.StreamSubscription;
import api.equinix.javasdk.fabric.model.json.creators.StreamSubscriptionOperator;

/**
 * Client interface for managing subscriptions to Equinix Fabric event streams. Subscriptions
 * define the sink destination where stream events are delivered.
 */
public interface StreamSubscriptions {

    /**
     * Lists all subscriptions for the specified stream.
     *
     * @param streamId the unique identifier of the parent stream
     * @return a paginated list of stream subscriptions
     */
    PaginatedList<StreamSubscription> list(String streamId);

    /**
     * Retrieves a single stream subscription by its unique identifier.
     *
     * @param streamId the unique identifier of the parent stream
     * @param uuid the unique identifier of the stream subscription
     * @return the stream subscription matching the given UUID
     */
    StreamSubscription getByUuid(String streamId, String uuid);

    /**
     * Begins the fluent builder for creating a new stream subscription.
     * Call methods on the returned builder to configure the subscription, then call {@code create()}.
     *
     * @param streamId the unique identifier of the parent stream
     * @return a builder for configuring the new stream subscription
     */
    StreamSubscriptionOperator.StreamSubscriptionBuilder define(String streamId);
}
