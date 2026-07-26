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

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.Streams;
import com.eqixiac.equinix.fabric.client.internal.StreamClient;
import com.eqixiac.equinix.fabric.model.Stream;
import com.eqixiac.equinix.fabric.model.json.StreamJson;
import com.eqixiac.equinix.fabric.model.json.creators.StreamOperator;
import com.eqixiac.equinix.fabric.model.wrappers.StreamWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StreamsImpl implements Streams {

    private final StreamClient<Stream> serviceClient;

    public PaginatedList<Stream> list() {
        Page<StreamJson> responsePage = this.serviceClient.list();
        PaginatedList<Stream> streamList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, StreamWrapper::new);
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
