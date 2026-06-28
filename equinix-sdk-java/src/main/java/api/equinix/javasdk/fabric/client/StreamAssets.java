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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.fabric.model.StreamAsset;

/**
 * Client interface for attaching and detaching assets (ports, connections, routers, etc.) to and
 * from Equinix Fabric streams for telemetry.
 */
public interface StreamAssets {

    /**
     * Retrieves a single asset attached to a stream.
     *
     * @param streamId the unique identifier of the stream
     * @param asset the asset type segment (for example {@code connections}, {@code ports}, {@code routers})
     * @param assetId the unique identifier of the asset
     * @return the stream asset
     */
    StreamAsset get(String streamId, String asset, String assetId);

    /**
     * Attaches an asset to a stream (enabling telemetry).
     *
     * @param streamId the unique identifier of the stream
     * @param asset the asset type segment (for example {@code connections}, {@code ports}, {@code routers})
     * @param assetId the unique identifier of the asset
     * @param metricsEnabled whether metrics are enabled for the attached asset
     * @return the attached stream asset
     */
    StreamAsset attach(String streamId, String asset, String assetId, Boolean metricsEnabled);

    /**
     * Detaches an asset from a stream.
     *
     * @param streamId the unique identifier of the stream
     * @param asset the asset type segment (for example {@code connections}, {@code ports}, {@code routers})
     * @param assetId the unique identifier of the asset
     * @return {@code true} if the detach was accepted
     */
    Boolean detach(String streamId, String asset, String assetId);
}
