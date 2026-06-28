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

package api.equinix.javasdk.internetaccess.client;

import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.creators.ChangeOperationUpdate;
import api.equinix.javasdk.internetaccess.model.json.creators.InternetAccessServiceOperator;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceSearchRequest;

import java.util.List;

/**
 * Client interface for the Equinix Internet Access (EIA) v2 service lifecycle.
 *
 * <p>Supports creating a service via the {@link #define()} builder
 * ({@code POST /internetAccess/v2/services}), retrieving it by id
 * ({@code GET /internetAccess/v2/services/{serviceId}}), updating its mutable properties
 * ({@code PATCH /internetAccess/v2/services/{serviceId}}), deleting it
 * ({@code DELETE /internetAccess/v2/services/{serviceId}}) and searching across services
 * ({@code POST /internetAccess/v2/services/search}).</p>
 */
public interface InternetAccessServices {

    /**
     * Returns a builder for defining and creating a new Equinix Internet Access v2 service.
     *
     * @return an internet access service builder
     */
    InternetAccessServiceOperator.InternetAccessServiceBuilder define();

    /**
     * Retrieves a single Equinix Internet Access v2 service by its unique identifier.
     *
     * @param serviceId the unique identifier of the service
     * @return the service matching the given identifier
     */
    InternetAccessService getByUuid(String serviceId);

    /**
     * Updates the mutable properties of an Equinix Internet Access v2 service by applying the
     * supplied change operations (for example replacing {@code /bandwidth}).
     *
     * @param serviceId the unique identifier of the service to update
     * @param operations the change operations to apply
     * @return the updated service
     */
    InternetAccessService update(String serviceId, List<ChangeOperationUpdate> operations);

    /**
     * Deletes an Equinix Internet Access v2 service by its unique identifier.
     *
     * @param serviceId the unique identifier of the service to delete
     * @return {@code true} if the deletion request was accepted
     */
    Boolean delete(String serviceId);

    /**
     * Searches for Equinix Internet Access v2 services matching the specified filter criteria.
     *
     * @param searchRequest the search filter criteria
     * @return a paginated, filtered list of matching services
     */
    PaginatedFilteredList<InternetAccessService> search(ServiceSearchRequest searchRequest);
}
