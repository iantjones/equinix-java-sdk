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
import api.equinix.javasdk.customerportal.model.Shipment;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentOperator;

/**
 * Client interface for managing shipment requests in the Equinix Customer Portal.
 * Provides operations to list, retrieve, and create inbound and outbound shipment
 * requests for equipment deliveries to IBX data centers.
 */
public interface Shipments {

    /**
     * Lists all shipments for the current account.
     *
     * @return a paginated list of shipments
     */
    PaginatedList<Shipment> list();

    /**
     * Retrieves a specific shipment by its unique identifier.
     *
     * @param uuid the unique identifier of the shipment
     * @return the matching shipment
     */
    Shipment getByUuid(String uuid);

    /**
     * Returns a builder for defining a new shipment request.
     *
     * @return a new ShipmentBuilder instance
     */
    ShipmentOperator.ShipmentBuilder define();
}
