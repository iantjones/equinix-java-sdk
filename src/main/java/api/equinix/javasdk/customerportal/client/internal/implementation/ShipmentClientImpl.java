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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.ShipmentClient;
import api.equinix.javasdk.customerportal.model.Shipment;
import api.equinix.javasdk.customerportal.model.json.ShipmentJson;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.ShipmentWrapper;

import java.util.Map;

public class ShipmentClientImpl extends PageableBase implements ShipmentClient<Shipment> {

    public ShipmentClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Shipments");
    }

    public Page<Shipment, ShipmentJson> list() {
        EquinixRequest<Shipment> equinixRequest = this.buildRequest("ListShipments", RequestType.PAGINATED, ShipmentJson.getPagedTypeRef());
        EquinixResponse<Shipment> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public ShipmentJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ShipmentJson> equinixRequest = this.buildRequestWithPathParams("GetShipment", RequestType.SINGLE, pParams, ShipmentJson.getSingleTypeRef());
        EquinixResponse<ShipmentJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ShipmentJson create(ShipmentCreatorJson shipmentCreatorJson) {
        EquinixRequest<ShipmentJson> equinixRequest = this.buildRequest("CreateShipment", RequestType.SINGLE, ShipmentJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, shipmentCreatorJson);
        EquinixResponse<ShipmentJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ShipmentJson update(String uuid, ShipmentCreatorJson shipmentCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ShipmentJson> equinixRequest = this.buildRequestWithPathParams("UpdateShipment", RequestType.SINGLE, pParams, ShipmentJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, shipmentCreatorJson);
        EquinixResponse<ShipmentJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ShipmentJson cancel(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ShipmentJson> equinixRequest = this.buildRequestWithPathParams("CancelShipment", RequestType.SINGLE, pParams, ShipmentJson.getSingleTypeRef());
        EquinixResponse<ShipmentJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ShipmentJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<Shipment> nextPage(PaginatedRequest<Shipment> equinixRequest) {
        EquinixResponse<Shipment> equinixResponse = this.invoke(equinixRequest);
        Page<Shipment, ShipmentJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Shipment> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ShipmentWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
