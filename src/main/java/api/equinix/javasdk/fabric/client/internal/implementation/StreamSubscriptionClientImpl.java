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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.StreamSubscriptionClient;
import api.equinix.javasdk.fabric.model.StreamSubscription;
import api.equinix.javasdk.fabric.model.json.StreamSubscriptionJson;
import api.equinix.javasdk.fabric.model.json.creators.StreamSubscriptionCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.StreamSubscriptionWrapper;

import java.util.Map;

public class StreamSubscriptionClientImpl extends ResourceClientBase<StreamSubscription, StreamSubscriptionJson> implements StreamSubscriptionClient<StreamSubscription> {

    public StreamSubscriptionClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "StreamSubscriptions", StreamSubscriptionJson.class);
    }

    @Override
    protected StreamSubscription wrap(StreamSubscriptionJson json) {
        return new StreamSubscriptionWrapper(json, this);
    }

    public Page<StreamSubscriptionJson> list(String streamId) {
        return listPagePath("GetStreamSubscriptions", Map.of("streamId", streamId));
    }

    public StreamSubscriptionJson getByUuid(String streamId, String uuid) {
        return getOne("GetStreamSubscription", Map.of("streamId", streamId, "uuid", uuid));
    }

    public StreamSubscriptionJson create(String streamId, StreamSubscriptionCreatorJson streamSubscriptionCreatorJson) {
        return postOne("PostStreamSubscription", Map.of("streamId", streamId), streamSubscriptionCreatorJson);
    }

    public StreamSubscriptionJson update(String streamId, String uuid, StreamSubscriptionCreatorJson streamSubscriptionCreatorJson) {
        return updateOne("UpdateStreamSubscription", Map.of("streamId", streamId, "uuid", uuid), streamSubscriptionCreatorJson);
    }

    public StreamSubscriptionJson delete(String streamId, String uuid) {
        return deleteOne("DeleteStreamSubscription", Map.of("streamId", streamId, "uuid", uuid));
    }

    public StreamSubscriptionJson refresh(String streamId, String uuid) {
        return getByUuid(streamId, uuid);
    }
}
