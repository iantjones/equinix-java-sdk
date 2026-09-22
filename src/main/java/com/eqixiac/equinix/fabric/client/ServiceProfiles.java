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
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.enums.ServiceProfileType;
import com.eqixiac.equinix.fabric.model.EnvironmentActionResponse;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceProfileAction;
import com.eqixiac.equinix.fabric.model.implementation.ActivationKeyDetails;
import com.eqixiac.equinix.fabric.model.implementation.ProviderEnvironment;
import com.eqixiac.equinix.fabric.model.implementation.ServiceMetro;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.creators.ServiceProfileOperator;

import java.util.List;

/**
 * Client interface for managing Equinix Fabric service profiles. Service profiles define the
 * attributes and access configurations that providers publish for customers to connect to.
 */
public interface ServiceProfiles {

    /**
     * Lists all service profiles owned by the current account.
     *
     * @return a paginated list of service profiles
     */
    PaginatedList<ServiceProfile> list();

    /**
     * Searches for service profiles using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching service profiles
     */
    PaginatedFilteredList<ServiceProfile> search();

    /**
     * Searches for service profiles matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching service profiles
     */
    PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter);

    /**
     * Searches for service profiles with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching service profiles
     */
    PaginatedFilteredList<ServiceProfile> search(SortPropertyList sort);

    /**
     * Searches for service profiles matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching service profiles
     */
    PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single service profile by its unique identifier.
     *
     * @param uuid the unique identifier of the service profile
     * @return the service profile matching the given UUID
     */
    ServiceProfile getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new service profile.
     * Call methods on the returned builder to configure the profile, then call {@code create()}.
     *
     * @param serviceProfileType the type of service profile to create
     * @return a builder for configuring the new service profile
     */
    ServiceProfileOperator.ServiceProfileBuilder define(ServiceProfileType serviceProfileType);

    /**
     * Begins a fluent PATCH update of an existing service profile, e.g.
     * {@code serviceProfiles.update(uuid).name("New-Name").save()}.
     *
     * @param uuid the unique identifier of the service profile to update
     * @return a fluent updater
     */
    ServiceProfileOperator.ServiceProfileUpdater update(String uuid);

    /**
     * Accepts or rejects a pending update on a service profile.
     *
     * @param uuid the unique identifier of the service profile
     * @param type the action type (for example {@code PROFILE_UPDATE_ACCEPTANCE} or {@code PROFILE_UPDATE_REJECTION})
     * @param description an optional description for the action (may be {@code null})
     * @return the action result
     */
    ServiceProfileAction createAction(String uuid, String type, String description);

    /**
     * Lists the metros in which a service profile is available.
     *
     * @param uuid the unique identifier of the service profile
     * @return the list of service metros
     */
    List<ServiceMetro> getMetros(String uuid);

    /**
     * Lists the provider environments of an {@code IC_PROFILE} service profile:
     * {@code GET /fabric/v4/serviceProfiles/{serviceProfileId}/environments}
     * (catalog operation {@code getServiceProfileEnvironmentsByUuid}).
     *
     * <p>The endpoint is paginated with {@code offset}/{@code limit} query parameters. This
     * method follows the server-reported pagination and issues one GET per page until the last
     * page, so the returned list holds every environment. The list is an unmodifiable snapshot.
     * A failure on any page propagates as {@code EquinixServiceException} (or its status-specific
     * subclass) and no partial list is returned.</p>
     *
     * <p>The same objects are embedded in {@code ServiceProfile.getEnvironments()} when the
     * profile itself is fetched.</p>
     *
     * <p><b>Beta</b>: the operation and the {@code ProviderEnvironment} schema carry the Fabric
     * v4 catalog's Beta marker (catalog fetched 2026-09-21). The behaviour for a profile whose
     * type is not {@code IC_PROFILE} is not documented.</p>
     *
     * <p>Declared {@code default} so implementations of this interface written before the method
     * existed still compile. The default throws; the client returned by
     * {@code Fabric.serviceProfiles()} overrides it.</p>
     *
     * @param serviceProfileUuid the unique identifier of the service profile
     * @return every provider environment of the profile; empty if there are none
     * @throws IllegalArgumentException      if {@code serviceProfileUuid} is {@code null} or blank
     * @throws UnsupportedOperationException if the implementation does not override this method
     */
    default List<ProviderEnvironment> getEnvironments(String serviceProfileUuid) {
        throw new UnsupportedOperationException(getClass().getName() + " does not implement getEnvironments");
    }

    /**
     * Validates an activation key against a provider environment:
     * {@code POST /fabric/v4/serviceProfiles/{serviceProfileId}/environments/{environmentId}/actions}
     * (catalog operation {@code serviceProfileEnvironmentAction}) with the body of the catalog's
     * {@code ValidateActivationKey} example:
     * <pre>{@code
     * {"type": "VALIDATE_ACTIVATION_KEY", "keyDetails": {"value": "<activationKey>"}}
     * }</pre>
     *
     * <p>The key is sent unmodified and is not decoded. It is unrelated to an access point's
     * {@code authenticationKey}. In the response, {@code getState()} is {@code INACTIVE} for a key
     * that has not been used and {@code ACTIVE} for a key that has
     * ({@code EnvironmentActionStateEnum}: "ACTIVE - already used , INACTIVE - not used");
     * {@code isKeyUnused()} tests for {@code INACTIVE}.</p>
     *
     * <p><b>Beta</b>: the operation, {@code ActivationKeyDetails} and
     * {@code EnvironmentActionResponse} carry the Fabric v4 catalog's Beta marker (catalog fetched
     * 2026-09-21). The catalog publishes no response example and does not say how a malformed or
     * unrecognised key is reported (an {@code ACTIVE}/{@code INACTIVE} body or a {@code 400}).
     * A {@code 4xx}/{@code 5xx} surfaces as {@code EquinixServiceException} or its status-specific
     * subclass.</p>
     *
     * <p>The default wraps the key with {@code ActivationKeyDetails.ofValue(activationKey)} and
     * delegates to {@link #validateActivationKey(String, String, ActivationKeyDetails)}.</p>
     *
     * @param serviceProfileUuid the unique identifier of the {@code IC_PROFILE} service profile
     * @param environmentUuid    the provider environment identifier, from
     *                           {@link #getEnvironments(String)}
     * @param activationKey      the provider-encoded activation key
     * @return the action response
     * @throws IllegalArgumentException if any argument is {@code null} or blank
     */
    default EnvironmentActionResponse validateActivationKey(String serviceProfileUuid, String environmentUuid,
                                                            String activationKey) {
        return validateActivationKey(serviceProfileUuid, environmentUuid, ActivationKeyDetails.ofValue(activationKey));
    }

    /**
     * Variant of {@link #validateActivationKey(String, String, String)} that sends a caller-built
     * {@code keyDetails} object. The {@code ActivationKeyDetails} schema marks no property
     * required or read-only, so {@code providerId}, {@code accountId}, {@code bandwidth} (Mbps)
     * and {@code region} can be sent with {@code value}. Null properties are omitted.
     *
     * <p><b>Beta</b>: the catalog's only request example sends {@code value} alone. How the
     * service treats the other properties on a request is unverified.</p>
     *
     * <p>Declared {@code default} for the same reason as {@link #getEnvironments(String)}. The
     * default throws; the client returned by {@code Fabric.serviceProfiles()} overrides it.</p>
     *
     * @param serviceProfileUuid the unique identifier of the {@code IC_PROFILE} service profile
     * @param environmentUuid    the provider environment identifier
     * @param keyDetails         the activation key details to send
     * @return the action response
     * @throws IllegalArgumentException      if an identifier is {@code null} or blank
     * @throws NullPointerException          if {@code keyDetails} is {@code null}
     * @throws UnsupportedOperationException if the implementation does not override this method
     */
    default EnvironmentActionResponse validateActivationKey(String serviceProfileUuid, String environmentUuid,
                                                            ActivationKeyDetails keyDetails) {
        throw new UnsupportedOperationException(getClass().getName() + " does not implement validateActivationKey");
    }
}
