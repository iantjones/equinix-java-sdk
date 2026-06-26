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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.WorkVisitClient;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import api.equinix.javasdk.customerportal.model.json.WorkVisitJson;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.WorkVisitWrapper;

public class WorkVisitClientImpl extends ResourceClientBase<WorkVisit, WorkVisitJson> implements WorkVisitClient<WorkVisit> {

    public WorkVisitClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "WorkVisits", WorkVisitJson.class);
    }

    @Override
    protected WorkVisit wrap(WorkVisitJson json) {
        return new WorkVisitWrapper(json, this);
    }

    public Page<WorkVisit, WorkVisitJson> list() {
        return listPage("ListWorkVisits");
    }

    public WorkVisitJson getByUuid(String uuid) {
        return getOne("GetWorkVisit", uuid);
    }

    public WorkVisitJson create(WorkVisitCreatorJson workVisitCreatorJson) {
        return postOne("CreateWorkVisit", workVisitCreatorJson);
    }

    public WorkVisitJson update(String uuid, WorkVisitCreatorJson workVisitCreatorJson) {
        return updateOne("UpdateWorkVisit", uuid, workVisitCreatorJson);
    }

    public WorkVisitJson cancel(String uuid) {
        return deleteOne("CancelWorkVisit", uuid);
    }

    public WorkVisitJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
