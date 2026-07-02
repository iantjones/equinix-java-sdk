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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.GeoScopeType;
import api.equinix.javasdk.fabric.enums.MetroType;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.GeoZone;
import api.equinix.javasdk.fabric.model.implementation.MetroService;

import java.util.List;

/**
 *
 * @author ianjones
 */
public interface Metro {

    /**
     * @return the metro code as a {@link MetroCode} enum, or {@link MetroCode#UNKNOWN} for a metro
     *         this SDK's enum does not list; use {@link #metroId()} for the exact code of an
     *         unlisted metro
     */
    MetroCode getCode();

    /**
     * @return the metro's code as a forward-compatible {@link MetroId}, preserving the exact value
     *         even for metros absent from {@link MetroCode}
     */
    MetroId metroId();

    MetroType getType();

    String getName();

    String getHref();

    Region getRegion();

    /**
     * @return the IBX data-center codes within this metro (e.g. {@code "SV1"}, {@code "SV5"})
     */
    List<String> getIbxs();

    GeoCoordinate geoCoordinates();

    List<ConnectedMetro> getConnectedMetros();

    /**
     * @return the country code in which the data center is located
     */
    String getCountry();

    /**
     * @return the Equinix autonomous system number (ASN) for this Fabric metro
     */
    Long getEquinixAsn();

    /**
     * @return the maximum local (intra-metro) virtual-connection bandwidth in Mbps
     */
    Long getLocalVCBandwidthMax();

    /**
     * @return the service types supported in this metro (e.g. {@code ETHERNET_IP_SERVICE},
     *         {@code TIME_SERVICE})
     */
    List<MetroService> getServices();

    /**
     * @return the geographic boundaries this metro supports (e.g. {@code CONUS}, {@code EU})
     */
    List<GeoScopeType> getGeoScopes();

    /**
     * @return the geographic zones this metro supports, with per-zone coverage notes
     */
    List<GeoZone> getGeoZones();

    Metro refresh();
}
