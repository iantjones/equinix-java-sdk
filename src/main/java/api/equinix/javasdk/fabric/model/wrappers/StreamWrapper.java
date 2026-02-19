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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.StreamClientImpl;
import api.equinix.javasdk.fabric.model.Stream;
import api.equinix.javasdk.fabric.model.json.StreamJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class StreamWrapper extends ResourceImpl<Stream> implements Stream {

    @Delegate(excludes = StreamMutability.class)
    private StreamJson jsonObject;
    @Getter
    private final Pageable<Stream> serviceClient;

    public StreamWrapper(StreamJson streamJson, Pageable<Stream> serviceClient) {
        this.jsonObject = streamJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete() {
        this.jsonObject = ((StreamClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((StreamClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface StreamMutability {
        Boolean delete();
        void refresh();
    }
}
