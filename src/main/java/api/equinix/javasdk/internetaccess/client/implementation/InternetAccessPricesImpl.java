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

package api.equinix.javasdk.internetaccess.client.implementation;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.internetaccess.client.InternetAccessPrices;
import api.equinix.javasdk.internetaccess.client.internal.PriceClient;
import api.equinix.javasdk.internetaccess.model.Price;
import api.equinix.javasdk.internetaccess.model.json.PriceJson;
import api.equinix.javasdk.internetaccess.model.json.creators.PriceSearchRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessPricesImpl implements InternetAccessPrices {

    private final PriceClient serviceClient;

    private final InternetAccess serviceManager;

    public PaginatedFilteredList<Price> search(PriceSearchRequest searchRequest) {
        Page<PriceJson> responsePage = this.serviceClient.search(searchRequest);
        return ResponseHandler.toPaginatedFilteredList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
