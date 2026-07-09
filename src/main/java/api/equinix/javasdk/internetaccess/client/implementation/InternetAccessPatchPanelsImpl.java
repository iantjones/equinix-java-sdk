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
import api.equinix.javasdk.internetaccess.client.InternetAccessPatchPanels;
import api.equinix.javasdk.internetaccess.client.internal.PatchPanelClient;
import api.equinix.javasdk.internetaccess.model.PatchPanel;
import api.equinix.javasdk.internetaccess.model.json.PatchPanelJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessPatchPanelsImpl implements InternetAccessPatchPanels {

    private final PatchPanelClient serviceClient;

    private final InternetAccess serviceManager;

    public PaginatedList<PatchPanel> list(String ibx, String accountNumber, String cageSpaceId, String cabinetSpaceId) {
        return list(ibx, accountNumber, cageSpaceId, cabinetSpaceId, null);
    }

    public PaginatedList<PatchPanel> list(String ibx, String accountNumber, String cageSpaceId, String cabinetSpaceId,
                                          String mediaTypesName) {
        Page<PatchPanelJson> responsePage =
                this.serviceClient.list(ibx, accountNumber, cageSpaceId, cabinetSpaceId, mediaTypesName);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
