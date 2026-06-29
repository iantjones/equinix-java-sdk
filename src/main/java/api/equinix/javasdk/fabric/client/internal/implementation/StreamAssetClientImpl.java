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

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.StreamAssetClient;
import api.equinix.javasdk.fabric.model.StreamAsset;
import api.equinix.javasdk.fabric.model.implementation.StreamAssetPutRequest;
import api.equinix.javasdk.fabric.model.json.StreamAssetJson;

import java.util.Map;

/**
 * Internal client for Fabric Stream Assets (attach / detach / get an asset to/from a stream). The
 * endpoints are keyed by stream id, asset type and asset id, with no list operation, so this client
 * reads/writes the {@link StreamAssetJson} singleton directly.
 *
 * @author ianjones
 */
public class StreamAssetClientImpl extends ClientBase implements StreamAssetClient<StreamAsset> {

    public StreamAssetClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "StreamAssets");
    }

    public StreamAssetJson get(String streamId, String asset, String assetId) {
        return getAs("GetStreamAsset", Map.of("streamId", streamId, "asset", asset, "assetId", assetId), null, StreamAssetJson.class);
    }

    public StreamAssetJson attach(String streamId, String asset, String assetId, Boolean metricsEnabled) {
        return postForType("UpdateStreamAsset", Map.of("streamId", streamId, "asset", asset, "assetId", assetId),
                new StreamAssetPutRequest(metricsEnabled), StreamAssetJson.getSingleTypeRef());
    }

    public StreamAssetJson detach(String streamId, String asset, String assetId) {
        // DELETE returns no body; validate (throws on error) then return the last-known view via GET-less null.
        booleanOp("DeleteStreamAsset", RequestType.SINGLE, Map.of("streamId", streamId, "asset", asset, "assetId", assetId), null, null);
        return null;
    }
}
