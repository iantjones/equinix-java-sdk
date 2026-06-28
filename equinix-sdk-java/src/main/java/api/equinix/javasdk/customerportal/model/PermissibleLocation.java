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

package api.equinix.javasdk.customerportal.model;

import java.util.List;
import java.util.Map;

/**
 * A location the current user may filter order history by: an IBX and its permitted cages.
 */
public interface PermissibleLocation {

    /**
     * Returns the IBX detail (code, metro, region, country, city, state, address, postalCode).
     *
     * @return the IBX detail map
     */
    Map<String, Object> getIbx();

    /**
     * Returns the cage ids permitted for the current user at this IBX.
     *
     * @return the list of cage ids
     */
    List<String> getCages();
}
