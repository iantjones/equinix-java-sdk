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
import api.equinix.javasdk.customerportal.client.Assets;
import api.equinix.javasdk.customerportal.client.internal.AssetClient;
import api.equinix.javasdk.customerportal.model.Asset;
import api.equinix.javasdk.customerportal.model.json.AssetJson;

public class AssetsImpl implements Assets {

    private final CustomerPortal serviceManager;

    private final AssetClient<Asset> serviceClient;

    public AssetsImpl(AssetClient<Asset> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Asset> list() {
        Page<Asset, AssetJson> responsePage = this.serviceClient.list();
        PaginatedList<Asset> assetList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(assetList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Asset getByUuid(String uuid) {
        return this.serviceClient.getByUuid(uuid);
    }
}
