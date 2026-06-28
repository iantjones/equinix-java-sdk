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
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.CompanyProfileClient;
import api.equinix.javasdk.fabric.model.CompanyProfile;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CompanyProfileJson;
import api.equinix.javasdk.fabric.model.json.creators.CompanyProfileCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.CompanyProfileWrapper;

import java.util.Map;

public class CompanyProfileClientImpl extends ResourceClientBase<CompanyProfile, CompanyProfileJson> implements CompanyProfileClient<CompanyProfile> {

    public CompanyProfileClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CompanyProfiles", CompanyProfileJson.class);
    }

    @Override
    protected CompanyProfile wrap(CompanyProfileJson json) {
        return new CompanyProfileWrapper(json, this);
    }

    public Page<CompanyProfile, CompanyProfileJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchCompanyProfiles", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public CompanyProfileJson getByUuid(String uuid) {
        return getOne("GetCompanyProfile", uuid);
    }

    public CompanyProfileJson create(CompanyProfileCreatorJson creatorJson) {
        return postOne("PostCompanyProfile", creatorJson);
    }

    public CompanyProfileJson delete(String uuid) {
        return deleteOne("DeleteCompanyProfile", uuid);
    }

    public void attachServiceProfile(String companyProfileId, String serviceProfileId) {
        booleanOp("AttachServiceProfile", RequestType.SINGLE,
                Map.of("companyProfileId", companyProfileId, "serviceProfileId", serviceProfileId), null, null);
    }

    public void detachServiceProfile(String companyProfileId, String serviceProfileId) {
        booleanOp("DetachServiceProfile", RequestType.SINGLE,
                Map.of("companyProfileId", companyProfileId, "serviceProfileId", serviceProfileId), null, null);
    }

    public void attachTag(String companyProfileId, String tagId) {
        booleanOp("AttachTag", RequestType.SINGLE,
                Map.of("companyProfileId", companyProfileId, "tagId", tagId), null, null);
    }

    public void detachTag(String companyProfileId, String tagId) {
        booleanOp("DetachTag", RequestType.SINGLE,
                Map.of("companyProfileId", companyProfileId, "tagId", tagId), null, null);
    }

    public CompanyProfileJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
