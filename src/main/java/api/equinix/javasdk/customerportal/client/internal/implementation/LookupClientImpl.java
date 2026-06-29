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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.LookupClient;
import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.customerportal.model.ConnectionService;
import api.equinix.javasdk.customerportal.model.LookupLocation;
import api.equinix.javasdk.customerportal.model.PatchPanel;
import api.equinix.javasdk.customerportal.model.Provider;
import api.equinix.javasdk.customerportal.model.json.ConnectionServiceJson;
import api.equinix.javasdk.customerportal.model.json.LocationsDetailsResponseJson;
import api.equinix.javasdk.customerportal.model.json.PatchPanelJson;
import api.equinix.javasdk.customerportal.model.json.ProviderJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LookupClientImpl extends ClientBase implements LookupClient {

    public LookupClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Lookup");
    }

    public List<? extends LookupLocation> listLocations(String permissionCode, List<String> ibxs,
                                                        String providerAccountNumber, String aSideIbx,
                                                        String connectionService, Boolean details) {
        Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("permissionCode", List.of(permissionCode));
        if (ibxs != null && !ibxs.isEmpty()) {
            queryParams.put("ibxs", ibxs);
        }
        if (providerAccountNumber != null) {
            queryParams.put("providerAccountNumber", List.of(providerAccountNumber));
        }
        if (aSideIbx != null) {
            queryParams.put("aSideIbx", List.of(aSideIbx));
        }
        if (connectionService != null) {
            queryParams.put("connectionService", List.of(connectionService));
        }
        if (details != null) {
            queryParams.put("details", List.of(String.valueOf(details)));
        }
        LocationsDetailsResponseJson response = getAs("ListLocations", null, queryParams,
                LocationsDetailsResponseJson.class);
        return response.getCrossConnects();
    }

    public List<? extends PatchPanel> listPatchPanels(String cabinetId) {
        return listAs("ListPatchPanels", null, Map.of("cabinetId", List.of(cabinetId)), PatchPanelJson.class);
    }

    public PatchPanelJson getPatchPanelById(String patchPanelId) {
        return getAs("GetPatchPanel", Map.of("patchPanelId", patchPanelId), null, PatchPanelJson.class);
    }

    public List<? extends Provider> listProviders(String cageId, String accountNumber) {
        return listAs("ListProviders", null,
                Map.of("cageId", List.of(cageId), "accountNumber", List.of(accountNumber)), ProviderJson.class);
    }

    public List<? extends ConnectionService> listConnectionServices(String ibx) {
        return listAs("ListConnectionServices", null, Map.of("ibx", List.of(ibx)), ConnectionServiceJson.class);
    }
}
