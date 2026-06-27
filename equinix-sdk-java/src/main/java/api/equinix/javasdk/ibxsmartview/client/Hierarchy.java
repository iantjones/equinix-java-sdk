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

package api.equinix.javasdk.ibxsmartview.client;

import api.equinix.javasdk.ibxsmartview.model.LocationHierarchy;
import api.equinix.javasdk.ibxsmartview.model.PowerHierarchy;

/**
 * Client interface for retrieving IBX SmartView hierarchy structures. Provides methods
 * to obtain the location and power hierarchy trees for an IBX, which define the
 * organizational structure of cages, cabinets, and power circuits.
 */
public interface Hierarchy {

    /**
     * Retrieves the location hierarchy for an IBX, describing the structure of cages and cabinets.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @return the location hierarchy tree
     */
    LocationHierarchy getLocationHierarchy(String accountNo, String ibx);

    /**
     * Retrieves the power hierarchy for an IBX, describing the structure of power circuits and feeds.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @return the power hierarchy tree
     */
    PowerHierarchy getPowerHierarchy(String accountNo, String ibx);
}
