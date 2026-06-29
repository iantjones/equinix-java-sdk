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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.InternetAccessCabinets;
import api.equinix.javasdk.internetaccess.client.internal.CabinetClient;
import api.equinix.javasdk.internetaccess.model.Cabinet;
import api.equinix.javasdk.internetaccess.model.json.CabinetJson;

public class InternetAccessCabinetsImpl implements InternetAccessCabinets {

    private final InternetAccess serviceManager;

    private final CabinetClient serviceClient;

    public InternetAccessCabinetsImpl(CabinetClient serviceClient, InternetAccess serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Cabinet> list() {
        return list(null, null, null);
    }

    public PaginatedList<Cabinet> list(String cageSpaceId, String ibx, String accountNumber) {
        Page<Cabinet, CabinetJson> responsePage = this.serviceClient.list(cageSpaceId, ibx, accountNumber);
        return Utils.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
