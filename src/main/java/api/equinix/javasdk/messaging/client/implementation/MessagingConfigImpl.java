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

package api.equinix.javasdk.messaging.client.implementation;

import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.messaging.client.MessagingConfig;
import api.equinix.javasdk.messaging.client.internal.implementation.EventClientImpl;
import api.equinix.javasdk.messaging.client.internal.implementation.SubscriptionClientImpl;
import lombok.Getter;

@Getter
public class MessagingConfigImpl extends Config implements MessagingConfig {

    private final SubscriptionClientImpl subscriptionClient;

    private final EventClientImpl eventClient;

    public MessagingConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.subscriptionClient = new SubscriptionClientImpl(this);
        this.eventClient = new EventClientImpl(this);
    }
}
