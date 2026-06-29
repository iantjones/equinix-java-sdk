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
import api.equinix.javasdk.internetaccess.model.OperationalUnit;

/**
 * Client interface for the Equinix Internet Access (EIA) v1 operational-units lookup
 * ({@code GET /internetAccess/v1/operationalUnits}) — the Equinix operational units serving a given
 * IBX.
 */
public interface InternetAccessOperationalUnits {

    /**
     * Returns the Equinix operational units serving the given IBX.
     *
     * @param ibx the IBX data center code (e.g. {@code WA1})
     * @return a paginated list of matching operational units
     */
    PaginatedList<OperationalUnit> list(String ibx);
}
