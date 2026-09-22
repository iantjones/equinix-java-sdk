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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.client.internal.ServiceProfileClient;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileType;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.EnvironmentActionResponse;
import com.eqixiac.equinix.fabric.model.Pricing;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceProfileAction;
import com.eqixiac.equinix.fabric.model.implementation.ActivationKeyDetails;
import com.eqixiac.equinix.fabric.model.implementation.EnvironmentActionRequest;
import com.eqixiac.equinix.fabric.model.implementation.ProviderEnvironment;
import com.eqixiac.equinix.fabric.model.implementation.ServiceMetro;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.PricingJson;
import com.eqixiac.equinix.fabric.model.json.ServiceProfileJson;
import com.eqixiac.equinix.fabric.model.json.creators.ConnectionOperator;
import com.eqixiac.equinix.fabric.model.json.creators.ServiceProfileOperator;
import com.eqixiac.equinix.fabric.model.wrappers.ConnectionWrapper;
import com.eqixiac.equinix.fabric.model.wrappers.PricingWrapper;
import com.eqixiac.equinix.fabric.model.wrappers.ServiceProfileWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class ServiceProfilesImpl implements ServiceProfiles {

    private final ServiceProfileClient<ServiceProfile> serviceClient;

    public PaginatedList<ServiceProfile> list() {
        return ResponseHandler.toPaginatedList(this.serviceClient.list(), this.serviceClient, ServiceProfileWrapper::new);
    }

    public PaginatedFilteredList<ServiceProfile> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<ServiceProfile> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter, SortPropertyList sort) {
        return ResponseHandler.toPaginatedFilteredList(serviceClient.search(filter, sort), this.serviceClient, ServiceProfileWrapper::new);
    }

    public ServiceProfile getByUuid(String uuid) {
        ServiceProfileJson serviceProfileJson = this.serviceClient.getByUuid(uuid);
        return new ServiceProfileWrapper(serviceProfileJson, this.serviceClient);
    }

    public ServiceProfileOperator.ServiceProfileBuilder define(ServiceProfileType serviceProfileType) {
        return new ServiceProfileOperator(this.serviceClient).create(serviceProfileType);
    }

    public ServiceProfileOperator.ServiceProfileUpdater update(String uuid) {
        return new ServiceProfileOperator(this.serviceClient).update(uuid);
    }

    public ServiceProfileAction createAction(String uuid, String type, String description) {
        return this.serviceClient.createAction(uuid, type, description);
    }

    public List<ServiceMetro> getMetros(String uuid) {
        return this.serviceClient.getMetros(uuid);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProviderEnvironment> getEnvironments(String serviceProfileUuid) {
        Page<ProviderEnvironment> firstPage =
                this.serviceClient.getEnvironments(requireText(serviceProfileUuid, "serviceProfileUuid"));
        if (firstPage == null || firstPage.getItems() == null) {
            return Collections.emptyList();
        }
        // The internal client is a Pageable<ServiceProfile>; its inherited nextPage(...) deserializes
        // each later page with the request's own response type (ProviderEnvironment) and maps the
        // items with the request's page-item mapper (set in ServiceProfileClientImpl#getEnvironments),
        // so reusing it here is correct. Only the generic parameter is laundered.
        Pageable<ProviderEnvironment> pageableClient = (Pageable<ProviderEnvironment>) (Object) this.serviceClient;
        return new PaginatedList<>(firstPage.getItems(), pageableClient, firstPage.getAssociatedRequest(),
                firstPage.getAssociatedResponse(), firstPage.getPagination()).loadAll().toList();
    }

    // validateActivationKey(String, String, String) is inherited: the interface default wraps the key
    // with ActivationKeyDetails.ofValue(...) and calls the overload below.

    @Override
    public EnvironmentActionResponse validateActivationKey(String serviceProfileUuid, String environmentUuid,
                                                           ActivationKeyDetails keyDetails) {
        Objects.requireNonNull(keyDetails, "keyDetails");
        return this.serviceClient.createEnvironmentAction(
                requireText(serviceProfileUuid, "serviceProfileUuid"),
                requireText(environmentUuid, "environmentUuid"),
                EnvironmentActionRequest.validateActivationKey(keyDetails));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }
}
