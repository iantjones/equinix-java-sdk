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
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.ServiceProfileClient;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceProfileAction;
import com.eqixiac.equinix.fabric.model.implementation.ServiceMetro;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileActionRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ServiceProfileActionJson;
import com.eqixiac.equinix.fabric.model.json.ServiceProfileJson;
import com.eqixiac.equinix.fabric.model.json.creators.ServiceProfileCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.ServiceProfileWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Service Profiles. Standard request/response plumbing and paging are
 * provided by {@link ResourceClientBase}; this class supplies only the JSON class, the wrapper
 * factory, and the per-operation endpoint names.
 *
 * @author ianjones
 */
public class ServiceProfileClientImpl extends ResourceClientBase<ServiceProfile, ServiceProfileJson>
        implements ServiceProfileClient<ServiceProfile> {

    public ServiceProfileClientImpl(FabricConfigImpl configClient) {
        // "ServiceProfile" drives the derived endpoint names (ListServiceProfiles,
        // SearchServiceProfiles, GetServiceProfile, DeleteServiceProfile, ...).
        super(configClient, "Fabric", "ServiceProfiles", ServiceProfileJson.class, "ServiceProfile");
    }

    @Override
    protected ServiceProfile wrap(ServiceProfileJson json) {
        return new ServiceProfileWrapper(json, this);
    }

    public Page<ServiceProfileJson> list() {
        return listPage();
    }

    public Page<ServiceProfileJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage(new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public ServiceProfileJson getByUuid(String uuid) {
        return getOneByUuid(uuid);
    }

    public ServiceProfileJson create(ServiceProfileCreatorJson serviceProfileCreatorJson) {
        // apiParams names this operation PostServiceProfile (not CreateServiceProfile) — explicit.
        return postOne("PostServiceProfile", serviceProfileCreatorJson);
    }

    public ServiceProfileJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("UpdateServiceProfile", uuid, operations);
    }

    public ServiceProfileJson put(String uuid, ServiceProfileCreatorJson serviceProfileCreatorJson) {
        return updateOne("PutServiceProfile", uuid, serviceProfileCreatorJson);
    }

    public ServiceProfileJson delete(String uuid) {
        return deleteOneByUuid(uuid);
    }

    public ServiceProfileAction createAction(String uuid, String type, String description) {
        return postForType("PostServiceProfileAction", Map.of("uuid", uuid),
                new ServiceProfileActionRequest(type, description), ServiceProfileActionJson.getSingleTypeRef());
    }

    public List<ServiceMetro> getMetros(String uuid) {
        EquinixRequest<ServiceMetro> request = buildRequestWithPathParams("GetServiceProfileMetros", RequestType.PAGINATED,
                Map.of("uuid", uuid), ServiceMetro.class);
        Page<ServiceMetro> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public ServiceProfileJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
