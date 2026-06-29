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

package api.equinix.javasdk.internetaccess.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.PatchPanelClient;
import api.equinix.javasdk.internetaccess.model.PatchPanel;
import api.equinix.javasdk.internetaccess.model.json.PatchPanelJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 product-availability
 * lookup {@code GET /internetAccess/v1/patchPanels}. The {@code PatchPanel} response is read-only,
 * so the deserialized {@link PatchPanelJson} (which implements {@link PatchPanel} directly) is
 * returned without a wrapper.
 */
public class PatchPanelClientImpl extends ResourceClientBase<PatchPanel, PatchPanelJson> implements PatchPanelClient {

    public PatchPanelClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "PatchPanelsV1", PatchPanelJson.class);
    }

    @Override
    protected PatchPanel wrap(PatchPanelJson json) {
        return json;
    }

    public Page<PatchPanel, PatchPanelJson> list(String ibx, String accountNumber, String cageSpaceId,
                                                 String cabinetSpaceId, String mediaTypesName) {
        Map<String, List<String>> queryParams = new HashMap<>();
        Utils.addAdditionalValue(queryParams, "location.ibx", ibx);
        Utils.addAdditionalValue(queryParams, "account.accountNumber", accountNumber);
        Utils.addAdditionalValue(queryParams, "cage.spaceId", cageSpaceId);
        Utils.addAdditionalValue(queryParams, "cabinet.spaceId", cabinetSpaceId);
        if (mediaTypesName != null) {
            Utils.addAdditionalValue(queryParams, "mediaTypes.name", mediaTypesName);
        }
        return listPage("ListPatchPanels", queryParams);
    }
}
