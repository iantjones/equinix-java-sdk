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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.SmartHands;
import api.equinix.javasdk.customerportal.model.json.creators.SmartHandsOperator;

/**
 * Client interface for managing Smart Hands service requests in the Equinix Customer Portal.
 * Provides operations to list, retrieve, and create Smart Hands requests where Equinix
 * technicians perform on-site tasks on behalf of customers.
 */
public interface SmartHandsRequests {

    /**
     * Lists all Smart Hands requests for the current account.
     *
     * @return a paginated list of Smart Hands requests
     */
    PaginatedList<SmartHands> list();

    /**
     * Retrieves a specific Smart Hands request by its unique identifier.
     *
     * @param uuid the unique identifier of the Smart Hands request
     * @return the matching Smart Hands request
     */
    SmartHands getByUuid(String uuid);

    /**
     * Returns a builder for defining a new Smart Hands request.
     *
     * @return a new SmartHandsBuilder instance
     */
    SmartHandsOperator.SmartHandsBuilder define();
}
