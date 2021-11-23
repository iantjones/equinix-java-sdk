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
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.ConnectionPricing;
import api.equinix.javasdk.fabric.model.implementation.Price;
import api.equinix.javasdk.fabric.model.json.ConnectionPricingJson;
import lombok.Getter;

import java.util.ArrayList;

/**
 * <p>ConnectionPricingWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ConnectionPricingWrapper extends ResourceImpl<Connection> implements ConnectionPricing {

    private ConnectionPricingJson jsonObject;
    @Getter
    private final Pageable<Connection> serviceClient;

    /**
     * <p>Constructor for ConnectionPricingWrapper.</p>
     *
     * @param connectionPricingJson a {@link api.equinix.javasdk.fabric.model.json.ConnectionPricingJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ConnectionPricingWrapper(ConnectionPricingJson connectionPricingJson, Pageable<Connection> serviceClient) {
        this.jsonObject = connectionPricingJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getCurrency.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCurrency() {
        return  this.jsonObject.getCurrency();
    }

    /**
     * <p>getOriginMetro.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     */
    public MetroCode getOriginMetro() {
        return  this.jsonObject.getOriginMetro();
    }

    /**
     * <p>getDestinationMetro.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     */
    public MetroCode getDestinationMetro() {
        return  this.jsonObject.getDestinationMetro();
    }

    /**
     * <p>getPriceList.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<Price> getPriceList() {
        return  this.jsonObject.getPriceList();
    }
//
//
//    public ConnectionPricing refresh() {
//        this.jsonObject = ((ConnectionsClientImpl)this.serviceClient).refreshStatistics(this.getUuid(),
//                this.getStats().getStartDateTime(), this.getStats().getEndDateTime(), this.getStats().getViewPoint());
//        return this;
//    }
}
