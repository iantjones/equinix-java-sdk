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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.creators.InternetAccessServiceOperator;

/**
 * Client interface for managing Equinix Internet Access services. Provides methods to
 * list, retrieve, and create internet access service instances that deliver dedicated
 * internet connectivity from Equinix IBX data centers.
 */
public interface InternetAccessServices {

    /**
     * Lists all internet access services for the current account.
     *
     * @return a paginated list of internet access services
     */
    PaginatedList<InternetAccessService> list();

    /**
     * Retrieves a specific internet access service by its unique identifier.
     *
     * @param uuid the unique identifier of the internet access service
     * @return the internet access service
     */
    InternetAccessService getByUuid(String uuid);

    /**
     * Returns a builder for defining and creating a new internet access service.
     *
     * @return an internet access service builder
     */
    InternetAccessServiceOperator.InternetAccessServiceBuilder define();
}
