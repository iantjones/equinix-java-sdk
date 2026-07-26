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

package com.eqixiac.equinix.internetaccess.client.implementation;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.client.InternetAccessIbxs;
import com.eqixiac.equinix.internetaccess.client.internal.IbxClient;
import com.eqixiac.equinix.internetaccess.client.internal.IbxV1Client;
import com.eqixiac.equinix.internetaccess.enums.ConnectionType;
import com.eqixiac.equinix.internetaccess.model.Ibx;
import com.eqixiac.equinix.internetaccess.model.json.IbxJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessIbxsImpl implements InternetAccessIbxs {

    private final IbxClient serviceClient;

    private final IbxV1Client ibxV1Client;

    private final InternetAccess serviceManager;

    public PaginatedList<Ibx> availability(ConnectionType connectionType) {
        return availability(connectionType, null, null);
    }

    public PaginatedList<Ibx> availability(ConnectionType connectionType, String accessPointType, String assetType) {
        Page<IbxJson> responsePage = this.serviceClient.list(connectionType, accessPointType, assetType);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }

    public Ibx getByCode(String ibx) {
        return getByCode(ibx, null, null);
    }

    public Ibx getByCode(String ibx, ConnectionType connectionType, String accessPointType) {
        return this.ibxV1Client.getByCode(ibx, connectionType, accessPointType);
    }
}
