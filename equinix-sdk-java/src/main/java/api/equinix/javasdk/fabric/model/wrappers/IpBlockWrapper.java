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
import api.equinix.javasdk.fabric.client.internal.implementation.IpBlockClientImpl;
import api.equinix.javasdk.fabric.model.IpBlock;
import api.equinix.javasdk.fabric.model.json.IpBlockJson;
import api.equinix.javasdk.fabric.model.json.creators.IpBlockOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class IpBlockWrapper extends ResourceImpl<IpBlock> implements IpBlock {

    @Delegate(excludes = IpBlockMutability.class)
    private IpBlockJson jsonObject;
    @Getter
    private final Pageable<IpBlock> serviceClient;

    public IpBlockWrapper(IpBlockJson ipBlockJson, Pageable<IpBlock> serviceClient) {
        this.jsonObject = ipBlockJson;
        this.serviceClient = serviceClient;
    }

    public IpBlockOperator.IpBlockUpdater update() {
        return new IpBlockOperator((IpBlockClientImpl) this.serviceClient).update(this.getUuid());
    }

    public Boolean delete() {
        this.jsonObject = ((IpBlockClientImpl) this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((IpBlockClientImpl) this.serviceClient).refresh(this.getUuid());
    }

    private interface IpBlockMutability {
        IpBlockOperator.IpBlockUpdater update();
        Boolean delete();
        void refresh();
    }
}
