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
import api.equinix.javasdk.fabric.client.internal.implementation.EiaServiceClientImpl;
import api.equinix.javasdk.fabric.model.EiaService;
import api.equinix.javasdk.fabric.model.json.EiaServiceJson;
import api.equinix.javasdk.fabric.model.json.creators.EiaServiceOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class EiaServiceWrapper extends ResourceImpl<EiaService> implements EiaService {

    @Delegate(excludes = EiaServiceMutability.class)
    private EiaServiceJson jsonObject;
    @Getter
    private final Pageable<EiaService> serviceClient;

    public EiaServiceWrapper(EiaServiceJson eiaServiceJson, Pageable<EiaService> serviceClient) {
        this.jsonObject = eiaServiceJson;
        this.serviceClient = serviceClient;
    }

    public EiaServiceOperator.EiaServiceUpdater update() {
        return new EiaServiceOperator((EiaServiceClientImpl) this.serviceClient).update(this.getUuid());
    }

    public Boolean delete() {
        this.jsonObject = ((EiaServiceClientImpl) this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((EiaServiceClientImpl) this.serviceClient).refresh(this.getUuid());
    }

    private interface EiaServiceMutability {
        EiaServiceOperator.EiaServiceUpdater update();
        Boolean delete();
        void refresh();
    }
}
