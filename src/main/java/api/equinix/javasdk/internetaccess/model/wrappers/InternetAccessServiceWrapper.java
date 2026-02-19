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

package api.equinix.javasdk.internetaccess.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.internetaccess.client.internal.implementation.InternetAccessServiceClientImpl;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class InternetAccessServiceWrapper extends ResourceImpl<InternetAccessService> implements InternetAccessService {

    @Delegate(excludes = InternetAccessServiceMutability.class)
    private InternetAccessServiceJson jsonObject;
    @Getter
    private final Pageable<InternetAccessService> serviceClient;

    public InternetAccessServiceWrapper(InternetAccessServiceJson internetAccessServiceJson, Pageable<InternetAccessService> serviceClient) {
        this.jsonObject = internetAccessServiceJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete() {
        this.jsonObject = ((InternetAccessServiceClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((InternetAccessServiceClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface InternetAccessServiceMutability {
        Boolean delete();
        void refresh();
    }
}
