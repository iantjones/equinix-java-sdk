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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.ShipmentClient;
import api.equinix.javasdk.customerportal.model.Shipment;
import api.equinix.javasdk.customerportal.model.json.ShipmentJson;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.ShipmentWrapper;

public class ShipmentClientImpl extends ResourceClientBase<Shipment, ShipmentJson> implements ShipmentClient<Shipment> {

    public ShipmentClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Shipments", ShipmentJson.class);
    }

    @Override
    protected Shipment wrap(ShipmentJson json) {
        return new ShipmentWrapper(json, this);
    }

    public Page<Shipment, ShipmentJson> list() {
        return listPage("ListShipments");
    }

    public ShipmentJson getByUuid(String uuid) {
        return getOne("GetShipment", uuid);
    }

    public ShipmentJson create(ShipmentCreatorJson shipmentCreatorJson) {
        return postOne("CreateShipment", shipmentCreatorJson);
    }

    public ShipmentJson update(String uuid, ShipmentCreatorJson shipmentCreatorJson) {
        return updateOne("UpdateShipment", uuid, shipmentCreatorJson);
    }

    public ShipmentJson cancel(String uuid) {
        return deleteOne("CancelShipment", uuid);
    }

    public ShipmentJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }
}
