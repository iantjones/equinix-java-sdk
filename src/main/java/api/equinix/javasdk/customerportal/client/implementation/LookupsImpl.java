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
import api.equinix.javasdk.customerportal.client.Lookups;
import api.equinix.javasdk.customerportal.client.internal.LookupClient;
import api.equinix.javasdk.customerportal.model.ConnectionService;
import api.equinix.javasdk.customerportal.model.LookupLocation;
import api.equinix.javasdk.customerportal.model.PatchPanel;
import api.equinix.javasdk.customerportal.model.Provider;

import java.util.List;

public class LookupsImpl implements Lookups {

    private final CustomerPortal serviceManager;

    private final LookupClient serviceClient;

    public LookupsImpl(LookupClient serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public List<? extends LookupLocation> listLocations(String permissionCode) {
        return this.serviceClient.listLocations(permissionCode, null, null, null, null, null);
    }

    public List<? extends LookupLocation> listLocations(String permissionCode, List<String> ibxs,
                                                        String providerAccountNumber, String aSideIbx,
                                                        String connectionService, Boolean details) {
        return this.serviceClient.listLocations(permissionCode, ibxs, providerAccountNumber, aSideIbx,
                connectionService, details);
    }

    public List<? extends PatchPanel> listPatchPanels(String cabinetId) {
        return this.serviceClient.listPatchPanels(cabinetId);
    }

    public PatchPanel getPatchPanelById(String patchPanelId) {
        return this.serviceClient.getPatchPanelById(patchPanelId);
    }

    public List<? extends Provider> listProviders(String cageId, String accountNumber) {
        return this.serviceClient.listProviders(cageId, accountNumber);
    }

    public List<? extends ConnectionService> listConnectionServices(String ibx) {
        return this.serviceClient.listConnectionServices(ibx);
    }
}
