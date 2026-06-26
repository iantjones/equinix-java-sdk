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

package api.equinix.javasdk.messaging.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.messaging.client.implementation.MessagingConfigImpl;
import api.equinix.javasdk.messaging.client.internal.SubscriptionClient;
import api.equinix.javasdk.messaging.model.Subscription;
import api.equinix.javasdk.messaging.model.json.SubscriptionJson;
import api.equinix.javasdk.messaging.model.json.creators.SubscriptionCreatorJson;
import api.equinix.javasdk.messaging.model.wrappers.SubscriptionWrapper;

public class SubscriptionClientImpl extends ResourceClientBase<Subscription, SubscriptionJson> implements SubscriptionClient<Subscription> {

    public SubscriptionClientImpl(MessagingConfigImpl configClient) {
        super(configClient, "Messaging", "Subscriptions", SubscriptionJson.class);
    }

    @Override
    protected Subscription wrap(SubscriptionJson json) {
        return new SubscriptionWrapper(json, this);
    }

    public Page<Subscription, SubscriptionJson> list() {
        return listPage("ListSubscriptions");
    }

    public SubscriptionJson getByUuid(String uuid) {
        return getOne("GetSubscription", uuid);
    }

    public SubscriptionJson create(SubscriptionCreatorJson subscriptionCreatorJson) {
        return postOne("CreateSubscription", subscriptionCreatorJson);
    }

    public SubscriptionJson update(String uuid, SubscriptionCreatorJson subscriptionCreatorJson) {
        return updateOne("UpdateSubscription", uuid, subscriptionCreatorJson);
    }

    public SubscriptionJson delete(String uuid) {
        return deleteOne("DeleteSubscription", uuid);
    }

    public SubscriptionJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
