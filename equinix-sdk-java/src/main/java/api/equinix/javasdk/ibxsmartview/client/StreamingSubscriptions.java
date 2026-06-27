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

package api.equinix.javasdk.ibxsmartview.client;

import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.SubscriptionCertificate;
import api.equinix.javasdk.ibxsmartview.model.SubscriptionData;
import api.equinix.javasdk.ibxsmartview.model.json.creators.StreamingSubscriptionOperator;

import java.util.List;

/**
 * Client interface for managing IBX SmartView streaming subscriptions. Provides methods
 * to list, retrieve, and create streaming subscriptions that enable real-time delivery
 * of environmental and power data from monitored IBX assets.
 */
public interface StreamingSubscriptions {

    /**
     * Lists all streaming subscriptions for the current account.
     *
     * @return a list of streaming subscriptions
     */
    List<StreamingSubscription> list();

    /**
     * Retrieves a specific streaming subscription by its unique identifier.
     *
     * @param uuid the unique identifier of the streaming subscription
     * @return the streaming subscription
     */
    StreamingSubscription getByUuid(String uuid);

    /**
     * Returns a builder for defining and creating a new streaming subscription.
     *
     * @return a streaming subscription builder
     */
    StreamingSubscriptionOperator.StreamingSubscriptionBuilder define();

    /**
     * Retrieves the data payload associated with a specific subscription.
     *
     * @param subscriptionId the unique identifier of the subscription
     * @return the subscription data
     */
    SubscriptionData getSubscriptionData(String subscriptionId);

    /**
     * Retrieves the certificate used for authenticating a streaming channel.
     *
     * @param channelType the type of streaming channel
     * @return the subscription certificate
     */
    SubscriptionCertificate getCertificate(String channelType);
}
