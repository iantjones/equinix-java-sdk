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

package com.eqixiac.equinix.ibxsmartview.client;

import com.eqixiac.equinix.ibxsmartview.model.implementation.HierarchyNode;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerHierarchyNode;

import java.util.List;

/**
 * Client interface for retrieving IBX SmartView hierarchy structures. Provides methods
 * to obtain the location and power hierarchy trees for an IBX, which define the
 * organizational structure of cages, cabinets, sensors and power circuits.
 *
 * <p>Both endpoints return a top-level array of recursive nodes, each describing one
 * level of the hierarchy and its children.</p>
 */
public interface Hierarchy {

    /**
     * Retrieves the location hierarchy for an account/IBX, returned as a tree of recursive nodes
     * spanning ibx, zone, cage, cabinet and sensor levels.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center (optional; may be {@code null} for all entitled IBXs)
     * @return the top-level location hierarchy nodes
     */
    List<HierarchyNode> getLocationHierarchy(String accountNo, String ibx);

    /**
     * Retrieves the power hierarchy for an account/IBX, returned as a tree of recursive nodes
     * spanning ibx, cage, cabinet and circuit levels.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center (optional; may be {@code null} for all entitled IBXs)
     * @return the top-level power hierarchy nodes
     */
    List<PowerHierarchyNode> getPowerHierarchy(String accountNo, String ibx);
}
