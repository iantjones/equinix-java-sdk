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
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.InternetAccessCages;
import api.equinix.javasdk.internetaccess.client.internal.CageClient;
import api.equinix.javasdk.internetaccess.model.Cage;
import api.equinix.javasdk.internetaccess.model.json.CageJson;

public class InternetAccessCagesImpl implements InternetAccessCages {

    private final InternetAccess serviceManager;

    private final CageClient serviceClient;

    public InternetAccessCagesImpl(CageClient serviceClient, InternetAccess serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Cage> list(String ibx, String accountNumber) {
        Page<CageJson> responsePage = this.serviceClient.list(ibx, accountNumber);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
