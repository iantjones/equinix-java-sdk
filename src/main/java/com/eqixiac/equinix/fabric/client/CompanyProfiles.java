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

package com.eqixiac.equinix.fabric.client;

import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.model.CompanyProfile;
import com.eqixiac.equinix.fabric.model.CompanyServiceProfile;
import com.eqixiac.equinix.fabric.model.PrivateService;
import com.eqixiac.equinix.fabric.model.Tag;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.creators.CompanyProfileOperator;

import java.util.List;

/**
 * Client interface for managing Equinix Fabric company profiles and their service-profile / tag
 * attachments.
 */
public interface CompanyProfiles {

    /**
     * Searches for company profiles using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching company profiles
     */
    PaginatedFilteredList<CompanyProfile> search();

    /**
     * Searches for company profiles matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching company profiles
     */
    PaginatedFilteredList<CompanyProfile> search(FilterPropertyList filter);

    /**
     * Searches for company profiles with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching company profiles
     */
    PaginatedFilteredList<CompanyProfile> search(SortPropertyList sort);

    /**
     * Searches for company profiles matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching company profiles
     */
    PaginatedFilteredList<CompanyProfile> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single company profile by its unique identifier.
     *
     * @param uuid the unique identifier of the company profile
     * @return the company profile matching the given UUID
     */
    CompanyProfile getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new company profile.
     *
     * @param type the company profile type (for example {@code COMPANY_PROFILE})
     * @return a builder for configuring the new company profile
     */
    CompanyProfileOperator.CompanyProfileBuilder define(String type);

    /**
     * Attaches a service profile to a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @param serviceProfileId the service profile uuid
     * @return {@code true} if the attach was accepted
     */
    Boolean attachServiceProfile(String companyProfileId, String serviceProfileId);

    /**
     * Detaches a service profile from a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @param serviceProfileId the service profile uuid
     * @return {@code true} if the detach was accepted
     */
    Boolean detachServiceProfile(String companyProfileId, String serviceProfileId);

    /**
     * Attaches a tag to a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @param tagId the tag uuid
     * @return {@code true} if the attach was accepted
     */
    Boolean attachTag(String companyProfileId, String tagId);

    /**
     * Detaches a tag from a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @param tagId the tag uuid
     * @return {@code true} if the detach was accepted
     */
    Boolean detachTag(String companyProfileId, String tagId);

    /**
     * Lists the service profiles attached to a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @return the attached service profiles (href / uuid references)
     */
    List<CompanyServiceProfile> getServiceProfiles(String companyProfileId);

    /**
     * Lists the tags attached to a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @return the attached tags
     */
    List<Tag> getTags(String companyProfileId);

    /**
     * Lists the private services attached to a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @return the attached private services (href / uuid references)
     */
    List<PrivateService> getPrivateServices(String companyProfileId);

    /**
     * Attaches a private service to a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @param privateServiceId the private service uuid
     * @return {@code true} if the attach was accepted
     */
    Boolean attachPrivateService(String companyProfileId, String privateServiceId);

    /**
     * Detaches a private service from a company profile.
     *
     * @param companyProfileId the company profile uuid
     * @param privateServiceId the private service uuid
     * @return {@code true} if the detach was accepted
     */
    Boolean detachPrivateService(String companyProfileId, String privateServiceId);

    /**
     * Downloads the raw bytes of a company profile logo.
     *
     * @param uuid the logo uuid
     * @return the logo image content
     */
    byte[] getLogo(String uuid);

    /**
     * Deletes a company profile logo.
     *
     * @param uuid the logo uuid
     * @return {@code true} if the delete was accepted
     */
    Boolean deleteLogo(String uuid);
}
