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
import api.equinix.javasdk.customerportal.client.internal.SecureCabinetClient;
import api.equinix.javasdk.customerportal.model.SecureCabinet;
import api.equinix.javasdk.customerportal.model.json.SecureCabinetJson;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.SecureCabinetWrapper;

public class SecureCabinetClientImpl extends ResourceClientBase<SecureCabinet, SecureCabinetJson> implements SecureCabinetClient<SecureCabinet> {

    public SecureCabinetClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "SecureCabinets", SecureCabinetJson.class);
    }

    @Override
    protected SecureCabinet wrap(SecureCabinetJson json) {
        return new SecureCabinetWrapper(json, this);
    }

    public Page<SecureCabinet, SecureCabinetJson> list() {
        return listPage("ListSecureCabinets");
    }

    public SecureCabinetJson getByUuid(String uuid) {
        return getOne("GetSecureCabinet", uuid);
    }

    public SecureCabinetJson create(SecureCabinetCreatorJson secureCabinetCreatorJson) {
        return postOne("CreateSecureCabinet", secureCabinetCreatorJson);
    }

    public SecureCabinetJson update(String uuid, SecureCabinetCreatorJson secureCabinetCreatorJson) {
        return updateOne("UpdateSecureCabinet", uuid, secureCabinetCreatorJson);
    }

    public SecureCabinetJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
