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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.EiaServiceClient;
import com.eqixiac.equinix.fabric.model.EiaService;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.EiaServiceJson;
import com.eqixiac.equinix.fabric.model.json.creators.EiaServiceCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.EiaServiceWrapper;

import java.util.List;

public class EiaServiceClientImpl extends ResourceClientBase<EiaService, EiaServiceJson> implements EiaServiceClient<EiaService> {

    public EiaServiceClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "EiaServices", EiaServiceJson.class);
    }

    @Override
    protected EiaService wrap(EiaServiceJson json) {
        return new EiaServiceWrapper(json, this);
    }

    public Page<EiaServiceJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchEiaServices", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public EiaServiceJson getByUuid(String uuid) {
        return getOne("GetEiaService", uuid);
    }

    public EiaServiceJson create(EiaServiceCreatorJson eiaServiceCreatorJson) {
        return postOne("CreateEiaService", eiaServiceCreatorJson);
    }

    public EiaServiceJson update(String uuid, List<PatchOperation> operations) {
        // PATCH /internetAccessServices/{uuid} with an op/path/value array sent as application/json
        // (not json-patch+json), so updateOne (default content-type) is correct here.
        return updateOne("PatchEiaService", uuid, operations);
    }

    public EiaServiceJson delete(String uuid) {
        return deleteOne("DeleteEiaService", uuid);
    }

    public EiaServiceJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
