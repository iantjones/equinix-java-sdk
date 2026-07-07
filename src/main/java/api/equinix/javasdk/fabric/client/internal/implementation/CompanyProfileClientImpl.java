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
import api.equinix.javasdk.fabric.model.CompanyServiceProfile;
import api.equinix.javasdk.fabric.model.PrivateService;
import api.equinix.javasdk.fabric.model.Tag;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortProperty;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CompanyProfileJson;
import api.equinix.javasdk.fabric.model.json.CompanyServiceProfileListResponseJson;
import api.equinix.javasdk.fabric.model.json.PrivateServiceListResponseJson;
import api.equinix.javasdk.fabric.model.json.TagListResponseJson;
import api.equinix.javasdk.fabric.model.json.creators.CompanyProfileCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.CompanyProfileWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CompanyProfileClientImpl extends ResourceClientBase<CompanyProfile, CompanyProfileJson> implements CompanyProfileClient<CompanyProfile> {

    public CompanyProfileClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CompanyProfiles", CompanyProfileJson.class);
    }

    @Override
    protected CompanyProfile wrap(CompanyProfileJson json) {
        return new CompanyProfileWrapper(json, this);
    }

    public Page<CompanyProfileJson> search(FilterPropertyList filter, SortPropertyList sort) {
        // The CompanyProfileSearchRequest spec declares "sort" as a single {property, direction}
        // object (schema Sort), not an array; send the first sort directive in that shape.
        SortProperty sortProperty = (sort != null && sort.getSortProperties() != null && !sort.getSortProperties().isEmpty())
                ? sort.getSortProperties().get(0)
                : null;
        return searchPage("SearchCompanyProfiles", new FilteredSortedPaginatedPost<>(filter, sortProperty));
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

    @SuppressWarnings("unchecked")
    public List<CompanyServiceProfile> getServiceProfiles(String companyProfileId) {
        CompanyServiceProfileListResponseJson response = getAs("GetCompanyProfileServiceProfiles",
                Map.of("companyProfileId", companyProfileId), null, CompanyServiceProfileListResponseJson.class);
        return (response != null && response.getData() != null)
                ? (List<CompanyServiceProfile>) (List<?>) response.getData()
                : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<Tag> getTags(String companyProfileId) {
        TagListResponseJson response = getAs("GetCompanyProfileTags",
                Map.of("companyProfileId", companyProfileId), null, TagListResponseJson.class);
        return (response != null && response.getData() != null)
                ? (List<Tag>) (List<?>) response.getData()
                : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<PrivateService> getPrivateServices(String companyProfileId) {
        PrivateServiceListResponseJson response = getAs("GetCompanyProfilePrivateServices",
                Map.of("companyProfileId", companyProfileId), null, PrivateServiceListResponseJson.class);
        return (response != null && response.getData() != null)
                ? (List<PrivateService>) (List<?>) response.getData()
                : Collections.emptyList();
    }

    public void attachPrivateService(String companyProfileId, String privateServiceId) {
        booleanOp("AttachPrivateService", RequestType.SINGLE,
                Map.of("companyProfileId", companyProfileId, "privateServiceId", privateServiceId), null, null);
    }

    public void detachPrivateService(String companyProfileId, String privateServiceId) {
        booleanOp("DetachPrivateService", RequestType.SINGLE,
                Map.of("companyProfileId", companyProfileId, "privateServiceId", privateServiceId), null, null);
    }

    public byte[] getLogo(String uuid) {
        return bytesOp("GetLogo", Map.of("uuid", uuid), null);
    }

    public void deleteLogo(String uuid) {
        booleanOp("DeleteLogo", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public CompanyProfileJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
