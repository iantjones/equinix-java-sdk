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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.fabric.client.internal.implementation.MetroClientImpl;
import api.equinix.javasdk.fabric.enums.MetroType;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.json.MetroJson;
import lombok.Getter;

import java.util.List;

/**
 * <p>MetroWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class MetroWrapper extends ResourceImpl<Metro> implements Metro {

    private MetroJson jsonObject;
    @Getter
    private final Pageable<Metro> serviceClient;

    /**
     * <p>Constructor for MetroWrapper.</p>
     *
     * @param metroJson a {@link api.equinix.javasdk.fabric.model.json.MetroJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public MetroWrapper (MetroJson metroJson, Pageable<Metro> serviceClient) {
        this.jsonObject = metroJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getCode.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     */
    public MetroCode getCode() {
        return this.jsonObject.getCode();
    }

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.MetroType} object.
     */
    public MetroType getType() {
        return this.jsonObject.getType();
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.jsonObject.getName();
    }

    /**
     * <p>getHref.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getHref() {
        return this.jsonObject.getHref();
    }

    /**
     * <p>getRegion.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.Region} object.
     */
    public Region getRegion() {
        return this.jsonObject.getRegion();
    }

    public GeoCoordinate geoCoordinates() {
        return this.jsonObject.getGeoCoordinates();
    }

    /**
     * <p>getConnectedMetros.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<ConnectedMetro> getConnectedMetros() {
        return this.jsonObject.getConnectedMetros();
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.Metro} object.
     */
    public Metro refresh() {
        this.jsonObject = ((MetroClientImpl)serviceClient).refresh(this.getCode());
        return this;
    }
}
