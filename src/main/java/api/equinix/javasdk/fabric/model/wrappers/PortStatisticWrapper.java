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
import api.equinix.javasdk.core.enums.PortType;
import api.equinix.javasdk.fabric.client.internal.implementation.PortStatisticClientImpl;
import api.equinix.javasdk.fabric.model.PortStatistic;
import api.equinix.javasdk.fabric.model.implementation.PortStat;
import api.equinix.javasdk.fabric.model.json.PortStatisticJson;
import lombok.Getter;

/**
 * <p>PortStatisticWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortStatisticWrapper extends ResourceImpl<PortStatistic> implements PortStatistic {

    private PortStatisticJson jsonObject;
    @Getter
    private final Pageable<PortStatistic> serviceClient;

    /**
     * <p>Constructor for PortStatisticWrapper.</p>
     *
     * @param portJson a {@link api.equinix.javasdk.fabric.model.json.PortStatisticJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public PortStatisticWrapper(PortStatisticJson portJson, Pageable<PortStatistic> serviceClient) {
        this.jsonObject = portJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUuid() {
        return this.jsonObject.getUuid();
    }

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.PortType} object.
     */
    public PortType getType() {
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
     * <p>getBandwidth.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getBandwidth() {
        return this.jsonObject.getBandwidth();
    }

    /**
     * <p>getStats.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.PortStat} object.
     */
    public PortStat getStats() {
        return this.jsonObject.getStats();
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.PortStatistic} object.
     */
    public PortStatistic refresh() {
        this.jsonObject = ((PortStatisticClientImpl)this.serviceClient).refreshStatistics(this.getUuid(),
                this.getStats().getStartDateTime(), this.getStats().getEndDateTime());
        return this;
    }
}
