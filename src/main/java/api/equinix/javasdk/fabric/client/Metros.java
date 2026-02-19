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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.MetroPresence;
import api.equinix.javasdk.fabric.model.Metro;

/**
 * Client interface for querying Equinix Fabric metro locations. Metros represent
 * geographic data center regions where Fabric resources can be provisioned.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Metros {

    /**
     * Lists all available Equinix metro locations.
     *
     * @return a paginated list of metros
     */
    PaginatedList<Metro> list();

    /**
     * Lists metro locations filtered by the specified presence type.
     *
     * @param presence the presence type to filter metros by (e.g., available for connections)
     * @return a paginated list of metros matching the presence filter
     */
    PaginatedList<Metro> list(MetroPresence presence);

    /**
     * Retrieves a single metro by its metro code.
     *
     * @param metroCode the code identifying the metro location (e.g., SV, DC, AM)
     * @return the metro matching the given code
     */
    Metro getByMetroCode(MetroCode metroCode);
}
