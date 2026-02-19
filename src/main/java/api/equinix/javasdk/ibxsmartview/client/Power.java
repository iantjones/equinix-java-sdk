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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.model.PowerReading;

/**
 * Client interface for accessing IBX SmartView power consumption data. Provides methods
 * to list and retrieve power readings for cabinets within an Equinix IBX data center.
 */
public interface Power {

    /**
     * Lists all power readings for the specified IBX.
     *
     * @param ibx the IBX code identifying the data center
     * @return a paginated list of power readings
     */
    PaginatedList<PowerReading> list(String ibx);

    /**
     * Retrieves the power reading for a specific cabinet within an IBX.
     *
     * @param ibx the IBX code identifying the data center
     * @param cabinetId the unique identifier of the cabinet
     * @return the power reading for the specified cabinet
     */
    PowerReading getPowerReading(String ibx, String cabinetId);
}
