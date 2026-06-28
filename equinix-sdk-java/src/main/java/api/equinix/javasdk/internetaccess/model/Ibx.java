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

package api.equinix.javasdk.internetaccess.model;

import api.equinix.javasdk.internetaccess.enums.Region;
import api.equinix.javasdk.internetaccess.model.implementation.GeoCoordinates;

/**
 * An Equinix IBX data center where Equinix Internet Access (EIA) is available, as returned by
 * the product-availability lookup {@code GET /internetAccess/v2/ibxs}.
 *
 * <p>This is a read-only response view.</p>
 */
public interface Ibx {

    /**
     * @return the URI of the IBX
     */
    String getHref();

    /**
     * @return the two-letter ISO country code of the IBX
     */
    String getCountryCode();

    /**
     * @return the country name of the IBX
     */
    String getCountryName();

    /**
     * @return the geographic region of the IBX ({@code APAC}, {@code EMEA} or {@code AMER})
     */
    Region getRegion();

    /**
     * @return the metro code of the IBX
     */
    String getMetroCode();

    /**
     * @return the IBX data center code (e.g. {@code WA1})
     */
    String getIbxCode();

    /**
     * @return the geographic coordinates of the IBX
     */
    GeoCoordinates getGeoCoordinates();
}
