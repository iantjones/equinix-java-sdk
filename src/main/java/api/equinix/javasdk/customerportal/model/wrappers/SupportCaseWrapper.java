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

package api.equinix.javasdk.customerportal.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.SupportCaseClientImpl;
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.json.SupportCaseJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class SupportCaseWrapper extends ResourceImpl<SupportCase> implements SupportCase {

    @Delegate(excludes = SupportCaseMutability.class)
    private SupportCaseJson jsonObject;
    @Getter
    private final Pageable<SupportCase> serviceClient;

    public SupportCaseWrapper(SupportCaseJson supportCaseJson, Pageable<SupportCase> serviceClient) {
        this.jsonObject = supportCaseJson;
        this.serviceClient = serviceClient;
    }

    public Boolean close() {
        this.jsonObject = ((SupportCaseClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((SupportCaseClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface SupportCaseMutability {
        Boolean close();
        void refresh();
    }
}
