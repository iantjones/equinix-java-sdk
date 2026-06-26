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
import api.equinix.javasdk.customerportal.client.internal.DigitalLOAClient;
import api.equinix.javasdk.customerportal.model.DigitalLOA;
import api.equinix.javasdk.customerportal.model.json.DigitalLOAJson;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLOACreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.DigitalLOAWrapper;

public class DigitalLOAClientImpl extends ResourceClientBase<DigitalLOA, DigitalLOAJson> implements DigitalLOAClient<DigitalLOA> {

    public DigitalLOAClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "DigitalLOA", DigitalLOAJson.class);
    }

    @Override
    protected DigitalLOA wrap(DigitalLOAJson json) {
        return new DigitalLOAWrapper(json, this);
    }

    public Page<DigitalLOA, DigitalLOAJson> list() {
        return listPage("ListDigitalLOAs");
    }

    public DigitalLOAJson getByUuid(String uuid) {
        return getOne("GetDigitalLOA", uuid);
    }

    public DigitalLOAJson create(DigitalLOACreatorJson digitalLOACreatorJson) {
        return postOne("CreateDigitalLOA", digitalLOACreatorJson);
    }

    public DigitalLOAJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
