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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.CompanyProfiles;
import api.equinix.javasdk.fabric.client.internal.CompanyProfileClient;
import api.equinix.javasdk.fabric.model.CompanyProfile;
import api.equinix.javasdk.fabric.model.CompanyServiceProfile;
import api.equinix.javasdk.fabric.model.PrivateService;
import api.equinix.javasdk.fabric.model.Tag;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CompanyProfileJson;
import api.equinix.javasdk.fabric.model.json.creators.CompanyProfileOperator;
import api.equinix.javasdk.fabric.model.wrappers.CompanyProfileWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CompanyProfilesImpl implements CompanyProfiles {

    private final CompanyProfileClient<CompanyProfile> serviceClient;

    public PaginatedFilteredList<CompanyProfile> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<CompanyProfile> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<CompanyProfile> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<CompanyProfile> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<CompanyProfileJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<CompanyProfile> profileList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, CompanyProfileWrapper::new);
        return new PaginatedFilteredList<>(profileList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public CompanyProfile getByUuid(String uuid) {
        CompanyProfileJson json = this.serviceClient.getByUuid(uuid);
        return new CompanyProfileWrapper(json, this.serviceClient);
    }

    public CompanyProfileOperator.CompanyProfileBuilder define(String type) {
        return new CompanyProfileOperator(this.serviceClient).create(type);
    }

    public Boolean attachServiceProfile(String companyProfileId, String serviceProfileId) {
        this.serviceClient.attachServiceProfile(companyProfileId, serviceProfileId);
        return true;
    }

    public Boolean detachServiceProfile(String companyProfileId, String serviceProfileId) {
        this.serviceClient.detachServiceProfile(companyProfileId, serviceProfileId);
        return true;
    }

    public Boolean attachTag(String companyProfileId, String tagId) {
        this.serviceClient.attachTag(companyProfileId, tagId);
        return true;
    }

    public Boolean detachTag(String companyProfileId, String tagId) {
        this.serviceClient.detachTag(companyProfileId, tagId);
        return true;
    }

    public List<CompanyServiceProfile> getServiceProfiles(String companyProfileId) {
        return this.serviceClient.getServiceProfiles(companyProfileId);
    }

    public List<Tag> getTags(String companyProfileId) {
        return this.serviceClient.getTags(companyProfileId);
    }

    public List<PrivateService> getPrivateServices(String companyProfileId) {
        return this.serviceClient.getPrivateServices(companyProfileId);
    }

    public Boolean attachPrivateService(String companyProfileId, String privateServiceId) {
        this.serviceClient.attachPrivateService(companyProfileId, privateServiceId);
        return true;
    }

    public Boolean detachPrivateService(String companyProfileId, String privateServiceId) {
        this.serviceClient.detachPrivateService(companyProfileId, privateServiceId);
        return true;
    }

    public byte[] getLogo(String uuid) {
        return this.serviceClient.getLogo(uuid);
    }

    public Boolean deleteLogo(String uuid) {
        this.serviceClient.deleteLogo(uuid);
        return true;
    }
}
