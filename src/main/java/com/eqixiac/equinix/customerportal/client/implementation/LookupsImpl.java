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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.customerportal.client.Lookups;
import com.eqixiac.equinix.customerportal.client.internal.LookupClient;
import com.eqixiac.equinix.customerportal.model.ConnectionService;
import com.eqixiac.equinix.customerportal.model.LookupLocation;
import com.eqixiac.equinix.customerportal.model.PatchPanel;
import com.eqixiac.equinix.customerportal.model.Provider;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LookupsImpl implements Lookups {

    private final LookupClient serviceClient;

    private final CustomerPortal serviceManager;

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
