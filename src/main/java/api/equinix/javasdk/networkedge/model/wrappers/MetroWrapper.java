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

package api.equinix.javasdk.networkedge.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.networkedge.model.Metro;
import api.equinix.javasdk.networkedge.model.implementation.Zone;
import api.equinix.javasdk.networkedge.model.json.MetroJson;
import lombok.Getter;

import java.util.ArrayList;

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
     * @param accountJson a {@link api.equinix.javasdk.networkedge.model.json.MetroJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public MetroWrapper(MetroJson accountJson, Pageable<Metro> serviceClient) {
        this.jsonObject = accountJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getMetroCode.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     */
    public MetroCode getMetroCode() {
        return  this.jsonObject.getMetroCode();
    }

    /**
     * <p>getRegion.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.Region} object.
     */
    public Region getRegion() {
        return  this.jsonObject.getRegion();
    }

    /**
     * <p>getAvailableZones.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<Zone> getAvailableZones() {
        return  this.jsonObject.getAvailableZones();
    }

    /**
     * <p>getClusterSupported.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getClusterSupported() {
        return  this.jsonObject.getClusterSupported();
    }

    /**
     * <p>getMetroDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetroDescription() {
        return  this.jsonObject.getMetroDescription();
    }
}
