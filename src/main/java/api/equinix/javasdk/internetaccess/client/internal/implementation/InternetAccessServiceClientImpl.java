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

package api.equinix.javasdk.internetaccess.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.InternetAccessServiceClient;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import api.equinix.javasdk.internetaccess.model.json.creators.InternetAccessServiceCreatorJson;
import api.equinix.javasdk.internetaccess.model.wrappers.InternetAccessServiceWrapper;

public class InternetAccessServiceClientImpl extends ResourceClientBase<InternetAccessService, InternetAccessServiceJson> implements InternetAccessServiceClient<InternetAccessService> {

    public InternetAccessServiceClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "Services", InternetAccessServiceJson.class);
    }

    @Override
    protected InternetAccessService wrap(InternetAccessServiceJson json) {
        return new InternetAccessServiceWrapper(json, this);
    }

    public Page<InternetAccessService, InternetAccessServiceJson> list() {
        return listPage("ListServices");
    }

    public InternetAccessServiceJson getByUuid(String uuid) {
        return getOne("GetService", uuid);
    }

    public InternetAccessServiceJson create(InternetAccessServiceCreatorJson internetAccessServiceCreatorJson) {
        return postOne("CreateService", internetAccessServiceCreatorJson);
    }

    public InternetAccessServiceJson update(String uuid, InternetAccessServiceCreatorJson internetAccessServiceCreatorJson) {
        return updateOne("UpdateService", uuid, internetAccessServiceCreatorJson);
    }

    public InternetAccessServiceJson delete(String uuid) {
        return deleteOne("DeleteService", uuid);
    }

    public InternetAccessServiceJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
