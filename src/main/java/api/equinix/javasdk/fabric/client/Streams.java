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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.Stream;
import api.equinix.javasdk.fabric.model.json.creators.StreamOperator;

/**
 * Client interface for managing Equinix Fabric event streams. Streams provide real-time
 * event delivery for monitoring changes to Fabric resources.
 */
public interface Streams {

    /**
     * Lists all streams accessible to the current account.
     *
     * @return a paginated list of streams
     */
    PaginatedList<Stream> list();

    /**
     * Retrieves a single stream by its unique identifier.
     *
     * @param uuid the unique identifier of the stream
     * @return the stream matching the given UUID
     */
    Stream getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new stream.
     * Call methods on the returned builder to configure the stream, then call {@code create()}.
     *
     * @return a builder for configuring the new stream
     */
    StreamOperator.StreamBuilder define();
}
