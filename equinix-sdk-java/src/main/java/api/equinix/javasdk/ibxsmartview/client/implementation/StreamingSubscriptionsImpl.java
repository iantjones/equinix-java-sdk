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

package api.equinix.javasdk.ibxsmartview.client.implementation;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.ibxsmartview.client.StreamingSubscriptions;
import api.equinix.javasdk.ibxsmartview.client.internal.StreamingSubscriptionClient;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.SubscriptionCertificate;
import api.equinix.javasdk.ibxsmartview.model.SubscriptionData;
import api.equinix.javasdk.ibxsmartview.model.json.StreamingSubscriptionJson;
import api.equinix.javasdk.ibxsmartview.model.json.creators.StreamingSubscriptionOperator;
import api.equinix.javasdk.ibxsmartview.model.wrappers.StreamingSubscriptionWrapper;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class StreamingSubscriptionsImpl implements StreamingSubscriptions {

    private final IBXSmartView serviceManager;

    private final StreamingSubscriptionClient<StreamingSubscription> serviceClient;

    public StreamingSubscriptionsImpl(StreamingSubscriptionClient<StreamingSubscription> serviceClient, IBXSmartView serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public List<StreamingSubscription> list() {
        List<StreamingSubscriptionJson> jsonList = this.serviceClient.list();
        return jsonList.stream()
                .map(json -> (StreamingSubscription) new StreamingSubscriptionWrapper(json, this.serviceClient))
                .collect(Collectors.toList());
    }

    public StreamingSubscription getByUuid(String uuid) {
        StreamingSubscriptionJson subscriptionJson = this.serviceClient.getByUuid(uuid);
        return new StreamingSubscriptionWrapper(subscriptionJson, this.serviceClient);
    }

    public StreamingSubscriptionOperator.StreamingSubscriptionBuilder define() {
        return new StreamingSubscriptionOperator(this.serviceClient).create();
    }

    public SubscriptionData getSubscriptionData(String subscriptionId) {
        return this.serviceClient.getSubscriptionData(subscriptionId);
    }

    public SubscriptionCertificate getCertificate(String channelType) {
        return this.serviceClient.getCertificate(channelType);
    }
}
