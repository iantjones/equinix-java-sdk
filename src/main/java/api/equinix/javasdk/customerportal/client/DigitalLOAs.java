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
import api.equinix.javasdk.customerportal.model.DigitalLOA;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLOAOperator;

/**
 * Client interface for managing digital Letters of Authorization (LOAs) in the Equinix
 * Customer Portal. Provides operations to list, retrieve, and create digital LOAs
 * that authorize cross-connect installations.
 */
public interface DigitalLOAs {

    /**
     * Lists all digital LOAs for the current account.
     *
     * @return a paginated list of digital LOAs
     */
    PaginatedList<DigitalLOA> list();

    /**
     * Retrieves a specific digital LOA by its unique identifier.
     *
     * @param uuid the unique identifier of the digital LOA
     * @return the matching digital LOA
     */
    DigitalLOA getByUuid(String uuid);

    /**
     * Returns a builder for defining a new digital LOA.
     *
     * @return a new DigitalLOABuilder instance
     */
    DigitalLOAOperator.DigitalLOABuilder define();
}
