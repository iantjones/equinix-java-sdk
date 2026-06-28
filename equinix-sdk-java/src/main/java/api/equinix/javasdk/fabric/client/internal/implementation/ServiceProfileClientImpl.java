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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.ServiceProfileClient;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.ServiceProfileAction;
import api.equinix.javasdk.fabric.model.implementation.ServiceMetro;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileActionRequest;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ServiceProfileActionJson;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import api.equinix.javasdk.fabric.model.json.creators.ServiceProfileCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceProfileWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Service Profiles. Standard request/response plumbing and paging are
 * provided by {@link ResourceClientBase}; this class supplies only the JSON class, the wrapper
 * factory, and the per-operation endpoint names.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceProfileClientImpl extends ResourceClientBase<ServiceProfile, ServiceProfileJson>
        implements ServiceProfileClient<ServiceProfile> {

    public ServiceProfileClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "ServiceProfiles", ServiceProfileJson.class);
    }

    @Override
    protected ServiceProfile wrap(ServiceProfileJson json) {
        return new ServiceProfileWrapper(json, this);
    }

    public Page<ServiceProfile, ServiceProfileJson> list() {
        return listPage("ListServiceProfiles");
    }

    public Page<ServiceProfile, ServiceProfileJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchServiceProfiles", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public ServiceProfileJson getByUuid(String uuid) {
        return getOne("GetServiceProfile", uuid);
    }

    public ServiceProfileJson create(ServiceProfileCreatorJson serviceProfileCreatorJson) {
        return postOne("PostServiceProfile", serviceProfileCreatorJson);
    }

    public ServiceProfileJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("UpdateServiceProfile", uuid, operations);
    }

    public ServiceProfileJson put(String uuid, ServiceProfileCreatorJson serviceProfileCreatorJson) {
        return updateOne("PutServiceProfile", uuid, serviceProfileCreatorJson);
    }

    public ServiceProfileJson delete(String uuid) {
        return deleteOne("DeleteServiceProfile", uuid);
    }

    public ServiceProfileAction createAction(String uuid, String type, String description) {
        return postForType("PostServiceProfileAction", Map.of("uuid", uuid),
                new ServiceProfileActionRequest(type, description), ServiceProfileActionJson.getSingleTypeRef());
    }

    public List<ServiceMetro> getMetros(String uuid) {
        EquinixRequest<ServiceMetro> request = buildRequestWithPathParams("GetServiceProfileMetros", RequestType.PAGINATED,
                Map.of("uuid", uuid), ServiceMetro.class);
        Page<ServiceMetro, ServiceMetro> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public ServiceProfileJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
