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
import api.equinix.javasdk.internetaccess.client.InternetAccessConnectionServices;
import api.equinix.javasdk.internetaccess.client.internal.ConnectionServiceClient;
import api.equinix.javasdk.internetaccess.model.ConnectionService;
import api.equinix.javasdk.internetaccess.model.json.ConnectionServiceJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessConnectionServicesImpl implements InternetAccessConnectionServices {

    private final ConnectionServiceClient serviceClient;

    private final InternetAccess serviceManager;

    public PaginatedList<ConnectionService> list(String ibx) {
        Page<ConnectionServiceJson> responsePage = this.serviceClient.list(ibx);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
