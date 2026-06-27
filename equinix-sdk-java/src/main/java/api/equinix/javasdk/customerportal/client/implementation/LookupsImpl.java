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
import api.equinix.javasdk.customerportal.client.Lookups;
import api.equinix.javasdk.customerportal.client.internal.LookupClient;
import api.equinix.javasdk.customerportal.model.LookupLocation;
import api.equinix.javasdk.customerportal.model.json.LookupLocationJson;

public class LookupsImpl implements Lookups {

    private final CustomerPortal serviceManager;

    private final LookupClient<LookupLocation> serviceClient;

    public LookupsImpl(LookupClient<LookupLocation> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<LookupLocation> listLocations() {
        Page<LookupLocation, LookupLocationJson> responsePage = this.serviceClient.listLocations();
        PaginatedList<LookupLocation> locationList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(locationList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public LookupLocation getLocationByUuid(String uuid) {
        return this.serviceClient.getLocationByUuid(uuid);
    }

    public PaginatedList<LookupLocation> listPatchPanels() {
        Page<LookupLocation, LookupLocationJson> responsePage = this.serviceClient.listPatchPanels();
        PaginatedList<LookupLocation> patchPanelList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(patchPanelList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
