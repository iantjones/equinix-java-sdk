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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.Shipments;
import api.equinix.javasdk.customerportal.client.internal.ShipmentClient;
import api.equinix.javasdk.customerportal.model.Shipment;
import api.equinix.javasdk.customerportal.model.json.ShipmentJson;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentOperator;
import api.equinix.javasdk.customerportal.model.wrappers.ShipmentWrapper;

public class ShipmentsImpl implements Shipments {

    private final CustomerPortal serviceManager;

    private final ShipmentClient<Shipment> serviceClient;

    public ShipmentsImpl(ShipmentClient<Shipment> serviceClient, CustomerPortal serviceManager) {
        this.serviceClient = serviceClient;
        this.serviceManager = serviceManager;
    }

    public PaginatedList<Shipment> list() {
        Page<Shipment, ShipmentJson> responsePage = this.serviceClient.list();
        PaginatedList<Shipment> shipmentList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, ShipmentWrapper::new);
        return new PaginatedList<>(shipmentList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Shipment getByUuid(String uuid) {
        ShipmentJson shipmentJson = this.serviceClient.getByUuid(uuid);
        return new ShipmentWrapper(shipmentJson, this.serviceClient);
    }

    public ShipmentOperator.ShipmentBuilder define() {
        return new ShipmentOperator(this.serviceClient).create();
    }
}
