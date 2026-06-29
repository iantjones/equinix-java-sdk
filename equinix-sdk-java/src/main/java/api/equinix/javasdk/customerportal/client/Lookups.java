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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.customerportal.model.ConnectionService;
import api.equinix.javasdk.customerportal.model.LookupLocation;
import api.equinix.javasdk.customerportal.model.PatchPanel;
import api.equinix.javasdk.customerportal.model.Provider;

import java.util.List;

/**
 * Client interface for colocation reference-data lookups in the Equinix Customer Portal.
 *
 * <p>Backed by the Colocation v2 lookup APIs under {@code /colocations/v2}. Provides the locations,
 * patch panels, providers and connection services required when building cross-connect and other
 * colocation orders.</p>
 */
public interface Lookups {

    /**
     * Lists the locations the current user may order at, for the given permission code.
     *
     * <p>Maps to {@code GET /colocations/v2/locations} ({@code Get Locations by permission code}).</p>
     *
     * @param permissionCode the permission code ({@code CROSS_CONNECT}, {@code WORK_VISIT} or {@code SHIPMENTS})
     * @return the list of permitted locations
     */
    List<? extends LookupLocation> listLocations(String permissionCode);

    /**
     * Lists the locations the current user may order at, for the given permission code, with optional
     * filters.
     *
     * <p>Maps to {@code GET /colocations/v2/locations} ({@code Get Locations by permission code}). The
     * {@code providerAccountNumber}, {@code aSideIbx} and {@code connectionService} filters apply only
     * when {@code permissionCode} is {@code CROSS_CONNECT}.</p>
     *
     * @param permissionCode        the permission code ({@code CROSS_CONNECT}, {@code WORK_VISIT} or {@code SHIPMENTS})
     * @param ibxs                  the IBX codes to filter by, or {@code null}
     * @param providerAccountNumber the service provider's (Z-side) account number, or {@code null}
     * @param aSideIbx              the A-side IBX used to fetch Z-side details, or {@code null}
     * @param connectionService     the connection service type used to fetch Z-side details, or {@code null}
     * @param details               when {@code true}, returns cage, cabinet and account details, or {@code null} for the default
     * @return the list of permitted locations
     */
    List<? extends LookupLocation> listLocations(String permissionCode, List<String> ibxs,
                                                 String providerAccountNumber, String aSideIbx,
                                                 String connectionService, Boolean details);

    /**
     * Lists the patch panels in the given cabinet.
     *
     * <p>Maps to {@code GET /colocations/v2/patchPanels} ({@code Retrieve all patch panels}).</p>
     *
     * @param cabinetId the cabinet id (required)
     * @return the list of patch panels
     */
    List<? extends PatchPanel> listPatchPanels(String cabinetId);

    /**
     * Retrieves the details of a patch panel by id.
     *
     * <p>Maps to {@code GET /colocations/v2/patchPanels/{patchPanelId}}
     * ({@code Retrieve patch panel details}).</p>
     *
     * @param patchPanelId the patch panel id
     * @return the patch panel details
     */
    PatchPanel getPatchPanelById(String patchPanelId);

    /**
     * Lists the cross-connect providers available for a cabinet and account.
     *
     * <p>Maps to {@code GET /colocations/v2/providers} ({@code Retrieve list of providers}).</p>
     *
     * @param cageId        the cage id (required)
     * @param accountNumber the account number (required)
     * @return the list of providers
     */
    List<? extends Provider> listProviders(String cageId, String accountNumber);

    /**
     * Lists the connection services available at an IBX.
     *
     * <p>Maps to {@code GET /colocations/v2/connectionServices}
     * ({@code Retrieve list of connection services}).</p>
     *
     * @param ibx the IBX code (required)
     * @return the list of connection services
     */
    List<? extends ConnectionService> listConnectionServices(String ibx);
}
