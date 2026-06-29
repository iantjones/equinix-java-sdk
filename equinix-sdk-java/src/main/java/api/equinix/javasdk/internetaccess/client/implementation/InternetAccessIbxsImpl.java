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
import api.equinix.javasdk.internetaccess.client.InternetAccessIbxs;
import api.equinix.javasdk.internetaccess.client.internal.IbxClient;
import api.equinix.javasdk.internetaccess.client.internal.IbxV1Client;
import api.equinix.javasdk.internetaccess.enums.ConnectionType;
import api.equinix.javasdk.internetaccess.model.Ibx;
import api.equinix.javasdk.internetaccess.model.json.IbxJson;

public class InternetAccessIbxsImpl implements InternetAccessIbxs {

    private final InternetAccess serviceManager;

    private final IbxClient serviceClient;

    private final IbxV1Client ibxV1Client;

    public InternetAccessIbxsImpl(IbxClient serviceClient, IbxV1Client ibxV1Client, InternetAccess serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
        this.ibxV1Client = ibxV1Client;
    }

    public PaginatedList<Ibx> availability(ConnectionType connectionType) {
        return availability(connectionType, null, null);
    }

    public PaginatedList<Ibx> availability(ConnectionType connectionType, String accessPointType, String assetType) {
        Page<Ibx, IbxJson> responsePage = this.serviceClient.list(connectionType, accessPointType, assetType);
        return Utils.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }

    public Ibx getByCode(String ibx) {
        return getByCode(ibx, null, null);
    }

    public Ibx getByCode(String ibx, ConnectionType connectionType, String accessPointType) {
        return this.ibxV1Client.getByCode(ibx, connectionType, accessPointType);
    }
}
