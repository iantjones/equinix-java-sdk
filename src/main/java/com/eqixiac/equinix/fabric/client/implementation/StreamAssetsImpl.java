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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.fabric.client.StreamAssets;
import com.eqixiac.equinix.fabric.client.internal.StreamAssetClient;
import com.eqixiac.equinix.fabric.model.StreamAsset;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.model.implementation.StreamDevicePutRequest;
import com.eqixiac.equinix.fabric.model.json.StreamAssetJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StreamAssetsImpl implements StreamAssets {

    private final StreamAssetClient<StreamAsset> serviceClient;

    public StreamAsset get(String streamId, String asset, String assetId) {
        return this.serviceClient.get(streamId, asset, assetId);
    }

    public StreamAsset attach(String streamId, String asset, String assetId, Boolean metricsEnabled) {
        return this.serviceClient.attach(streamId, asset, assetId, metricsEnabled);
    }

    public Boolean detach(String streamId, String asset, String assetId) {
        this.serviceClient.detach(streamId, asset, assetId);
        return true;
    }

    @Override
    public PaginatedFilteredList<StreamAsset> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<StreamAssetJson> page = this.serviceClient.search(filter, sort);
        return ResponseHandler.toPaginatedFilteredList(page, this.serviceClient, (json, client) -> json);
    }

    @Override
    public StreamAsset getDevice(String streamId, String deviceId) {
        return this.serviceClient.getDevice(streamId, deviceId);
    }

    @Override
    public StreamAsset attachDevice(String streamId, String deviceId, java.util.List<Integer> interfaceIds) {
        if (interfaceIds == null || interfaceIds.isEmpty()) {
            throw new IllegalArgumentException("interfaceIds must not be empty; use detachDevice to stop streaming");
        }
        return this.serviceClient.updateDevice(streamId, deviceId, new StreamDevicePutRequest(true, interfaceIds));
    }

    @Override
    public StreamAsset detachDevice(String streamId, String deviceId) {
        return this.serviceClient.deleteDevice(streamId, deviceId);
    }
}
