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

package com.eqixiac.equinix.customerportal.model;

import com.eqixiac.equinix.customerportal.enums.FabricPortSpeed;
import com.eqixiac.equinix.customerportal.model.implementation.AcCircuitConfig;
import com.eqixiac.equinix.customerportal.model.implementation.CabinetDimensionsInfo;
import com.eqixiac.equinix.customerportal.model.implementation.PduConfig;

/**
 * Secure cabinet product availability for an IBX, describing the cabinet capacity and power
 * configuration that may be ordered at a given location.
 */
public interface ProductAvailability {

    /**
     * Returns the IBX code this availability applies to.
     *
     * @return the IBX code
     */
    String getIbx();

    /**
     * Returns the maximum number of secure cabinets that may be ordered.
     *
     * @return the maximum number of cabinets, or {@code null} if not provided
     */
    Integer getMaximumNumberOfCabinetsToOrder();

    /**
     * Returns the minimum draw capacity (in kW) available per cabinet.
     *
     * @return the minimum draw capacity, or {@code null} if not provided
     */
    Double getMinimumDrawCapacityPerCabinet();

    /**
     * Returns the maximum draw capacity (in kW) available per cabinet.
     *
     * @return the maximum draw capacity, or {@code null} if not provided
     */
    Double getMaximumDrawCapacityPerCabinet();

    /**
     * Returns the cabinet dimensions available at the location.
     *
     * @return the cabinet dimensions, or {@code null} if not provided
     */
    CabinetDimensionsInfo getCabinetDimensions();

    /**
     * Returns the AC power circuit configuration available at the location.
     *
     * @return the AC circuit configuration, or {@code null} if not provided
     */
    AcCircuitConfig getAcCircuitConfiguration();

    /**
     * Returns the PDU configuration available at the location. A {@code null} value means PDUs are
     * not offered at the IBX.
     *
     * @return the PDU configuration, or {@code null} if not offered
     */
    PduConfig getPduConfiguration();

    /**
     * Returns the supported Fabric port speed for the location. A {@code null} value means a Fabric
     * port is not available at the IBX.
     *
     * @return the Fabric port speed, or {@code null} if not available
     */
    FabricPortSpeed getFabricPortSpeed();
}
