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
import api.equinix.javasdk.customerportal.client.internal.implementation.WorkVisitClientImpl;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import api.equinix.javasdk.customerportal.model.json.WorkVisitJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class WorkVisitWrapper extends ResourceImpl<WorkVisit> implements WorkVisit {

    @Delegate(excludes = WorkVisitMutability.class)
    private WorkVisitJson jsonObject;
    @Getter
    private final Pageable<WorkVisit> serviceClient;

    public WorkVisitWrapper(WorkVisitJson workVisitJson, Pageable<WorkVisit> serviceClient) {
        this.jsonObject = workVisitJson;
        this.serviceClient = serviceClient;
    }

    public Boolean cancel() {
        this.jsonObject = ((WorkVisitClientImpl)this.serviceClient).cancel(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((WorkVisitClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface WorkVisitMutability {
        Boolean cancel();
        void refresh();
    }
}
