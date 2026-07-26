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

package com.eqixiac.equinix.fabric.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.StreamClientImpl;
import com.eqixiac.equinix.fabric.model.Stream;
import com.eqixiac.equinix.fabric.model.json.StreamJson;
import com.eqixiac.equinix.fabric.model.json.creators.StreamOperator;
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

    public StreamOperator.StreamBuilder update() {
        return new StreamOperator(this.serviceClient).update(this.jsonObject);
    }

    public Boolean delete() {
        this.jsonObject = ((StreamClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((StreamClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface StreamMutability {
        StreamOperator.StreamBuilder update();
        Boolean delete();
        void refresh();
    }
}
