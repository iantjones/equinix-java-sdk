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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.Streams;
import api.equinix.javasdk.fabric.client.internal.StreamClient;
import api.equinix.javasdk.fabric.model.Stream;
import api.equinix.javasdk.fabric.model.json.StreamJson;
import api.equinix.javasdk.fabric.model.json.creators.StreamOperator;
import api.equinix.javasdk.fabric.model.wrappers.StreamWrapper;

public class StreamsImpl implements Streams {

    private final Fabric serviceManager;

    private final StreamClient<Stream> serviceClient;

    public StreamsImpl(StreamClient<Stream> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Stream> list() {
        Page<Stream, StreamJson> responsePage = this.serviceClient.list();
        PaginatedList<Stream> streamList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, StreamWrapper::new);
        return new PaginatedList<>(streamList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Stream getByUuid(String uuid) {
        StreamJson streamJson = this.serviceClient.getByUuid(uuid);
        return new StreamWrapper(streamJson, this.serviceClient);
    }

    public StreamOperator.StreamBuilder define() {
        return new StreamOperator(this.serviceClient).create();
    }
}
