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

package com.eqixiac.equinix.ibxsmartview.client.implementation;

import com.eqixiac.equinix.IBXSmartView;
import com.eqixiac.equinix.ibxsmartview.client.StreamingSubscriptions;
import com.eqixiac.equinix.ibxsmartview.client.internal.StreamingSubscriptionClient;
import com.eqixiac.equinix.ibxsmartview.model.StreamingSubscription;
import com.eqixiac.equinix.ibxsmartview.model.SubscriptionCertificate;
import com.eqixiac.equinix.ibxsmartview.model.SubscriptionData;
import com.eqixiac.equinix.ibxsmartview.model.json.StreamingSubscriptionJson;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.StreamingSubscriptionOperator;
import com.eqixiac.equinix.ibxsmartview.model.wrappers.StreamingSubscriptionWrapper;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StreamingSubscriptionsImpl implements StreamingSubscriptions {

    private final StreamingSubscriptionClient<StreamingSubscription> serviceClient;

    private final IBXSmartView serviceManager;

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
        return this.serviceClient.getSubscriptionData(subscriptionId, null, null, null, null, null);
    }

    public SubscriptionData getSubscriptionData(String subscriptionId, List<String> ibxs, List<String> messageTypes,
                                                List<String> streamIds, Integer offset, Integer limit) {
        return this.serviceClient.getSubscriptionData(subscriptionId, ibxs, messageTypes, streamIds, offset, limit);
    }

    public SubscriptionCertificate getCertificate(String channelType) {
        return this.serviceClient.getCertificate(channelType);
    }
}
