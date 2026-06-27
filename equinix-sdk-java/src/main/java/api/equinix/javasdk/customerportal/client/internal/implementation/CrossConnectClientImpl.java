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
import api.equinix.javasdk.customerportal.client.internal.CrossConnectClient;
import api.equinix.javasdk.customerportal.model.CrossConnect;
import api.equinix.javasdk.customerportal.model.json.CrossConnectJson;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.CrossConnectWrapper;

public class CrossConnectClientImpl extends ResourceClientBase<CrossConnect, CrossConnectJson> implements CrossConnectClient<CrossConnect> {

    public CrossConnectClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "CrossConnects", CrossConnectJson.class);
    }

    @Override
    protected CrossConnect wrap(CrossConnectJson json) {
        return new CrossConnectWrapper(json, this);
    }

    public Page<CrossConnect, CrossConnectJson> list() {
        return listPage("ListCrossConnects");
    }

    public CrossConnectJson getByUuid(String uuid) {
        return getOne("GetCrossConnect", uuid);
    }

    public CrossConnectJson create(CrossConnectCreatorJson crossConnectCreatorJson) {
        return postOne("CreateCrossConnect", crossConnectCreatorJson);
    }

    public CrossConnectJson update(String uuid, CrossConnectCreatorJson crossConnectCreatorJson) {
        return updateOne("UpdateCrossConnect", uuid, crossConnectCreatorJson);
    }

    public CrossConnectJson delete(String uuid) {
        return deleteOne("DeleteCrossConnect", uuid);
    }

    public CrossConnectJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
