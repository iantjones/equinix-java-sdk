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

package api.equinix.javasdk.ibxsmartview.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.StreamingSubscriptionClient;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.json.StreamingSubscriptionJson;
import api.equinix.javasdk.ibxsmartview.model.json.SubscriptionCertificateJson;
import api.equinix.javasdk.ibxsmartview.model.json.SubscriptionDataJson;
import api.equinix.javasdk.ibxsmartview.model.json.creators.StreamingSubscriptionCreatorJson;
import api.equinix.javasdk.ibxsmartview.model.wrappers.StreamingSubscriptionWrapper;

import java.util.List;
import java.util.Map;

public class StreamingSubscriptionClientImpl extends ResourceClientBase<StreamingSubscription, StreamingSubscriptionJson> implements StreamingSubscriptionClient<StreamingSubscription> {

    public StreamingSubscriptionClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "StreamingSubscriptions", StreamingSubscriptionJson.class);
    }

    @Override
    protected StreamingSubscription wrap(StreamingSubscriptionJson json) {
        return new StreamingSubscriptionWrapper(json, this);
    }

    public List<StreamingSubscriptionJson> list() {
        EquinixRequest<StreamingSubscription> equinixRequest = this.buildRequest("ListSubscriptions", RequestType.LIST, StreamingSubscriptionJson.class);
        EquinixResponse<StreamingSubscription> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleListResponse(equinixResponse, equinixRequest);
    }

    public StreamingSubscriptionJson getByUuid(String uuid) {
        return getOne("GetSubscription", uuid);
    }

    public StreamingSubscriptionJson create(StreamingSubscriptionCreatorJson creatorJson) {
        return postOne("CreateSubscription", creatorJson);
    }

    public StreamingSubscriptionJson update(String uuid, StreamingSubscriptionCreatorJson creatorJson) {
        return updateOne("UpdateSubscription", uuid, creatorJson);
    }

    public StreamingSubscriptionJson delete(String uuid) {
        return deleteOne("DeleteSubscription", uuid);
    }

    public StreamingSubscriptionJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public SubscriptionDataJson getSubscriptionData(String subscriptionId) {
        return getOneAs("GetSubscriptionData", Map.of("subscriptionId", subscriptionId), Map.of(), SubscriptionDataJson.class);
    }

    public SubscriptionCertificateJson getCertificate(String channelType) {
        return getOneAs("GetCertificate", Map.of(), Map.of("channelType", List.of(channelType)), SubscriptionCertificateJson.class);
    }
}
