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

package api.equinix.javasdk.networkedge.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.networkedge.model.implementation.Zone;

import java.util.ArrayList;

/**
 * <p>Metro interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Metro {

    /**
     * <p>getMetroCode.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     */
    MetroCode getMetroCode();

    /**
     * <p>getRegion.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.Region} object.
     */
    Region getRegion();

    /**
     * <p>getAvailableZones.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    ArrayList<Zone> getAvailableZones();

    /**
     * <p>getClusterSupported.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean getClusterSupported();

    /**
     * <p>getMetroDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getMetroDescription();
}
