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
import api.equinix.javasdk.fabric.client.internal.StreamClient;
import api.equinix.javasdk.fabric.model.Stream;
import api.equinix.javasdk.fabric.model.json.StreamJson;
import api.equinix.javasdk.fabric.model.json.creators.StreamCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.StreamWrapper;

public class StreamClientImpl extends ResourceClientBase<Stream, StreamJson> implements StreamClient<Stream> {

    public StreamClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Streams", StreamJson.class);
    }

    @Override
    protected Stream wrap(StreamJson json) {
        return new StreamWrapper(json, this);
    }

    public Page<Stream, StreamJson> list() {
        return listPage("GetStreams");
    }

    public StreamJson getByUuid(String uuid) {
        return getOne("GetStream", uuid);
    }

    public StreamJson create(StreamCreatorJson streamCreatorJson) {
        return postOne("PostStream", streamCreatorJson);
    }

    public StreamJson update(String uuid, StreamCreatorJson streamCreatorJson) {
        return updateOne("UpdateStream", uuid, streamCreatorJson);
    }

    public StreamJson delete(String uuid) {
        return deleteOne("DeleteStream", uuid);
    }

    public StreamJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
