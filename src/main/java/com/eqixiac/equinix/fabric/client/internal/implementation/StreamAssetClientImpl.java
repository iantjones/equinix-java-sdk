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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.model.implementation.StreamDevicePutRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.StreamAssetClient;
import com.eqixiac.equinix.fabric.model.StreamAsset;
import com.eqixiac.equinix.fabric.model.implementation.StreamAssetPutRequest;
import com.eqixiac.equinix.fabric.model.json.StreamAssetJson;

import java.util.Map;

/**
 * Internal client for Fabric Stream Assets (attach / detach / get an asset to/from a stream). The
 * endpoints are keyed by stream id, asset type and asset id, with no list operation, so this client
 * reads/writes the {@link StreamAssetJson} singleton directly.
 *
 * @author ianjones
 */
public class StreamAssetClientImpl extends ResourceClientBase<StreamAsset, StreamAssetJson> implements StreamAssetClient<StreamAsset> {

    public StreamAssetClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "StreamAssets", StreamAssetJson.class);
    }

    @Override
    protected StreamAsset wrap(StreamAssetJson json) {
        return json;
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

    public Page<StreamAssetJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchStreamAssets", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public StreamAssetJson getDevice(String streamId, String deviceId) {
        return getOne("GetStreamDevice", Map.of("streamId", streamId, "deviceId", deviceId));
    }

    public StreamAssetJson updateDevice(String streamId, String deviceId, StreamDevicePutRequest request) {
        return updateOne("UpdateStreamDevice", Map.of("streamId", streamId, "deviceId", deviceId), request);
    }

    public StreamAssetJson deleteDevice(String streamId, String deviceId) {
        return deleteOne("DeleteStreamDevice", Map.of("streamId", streamId, "deviceId", deviceId));
    }
}
